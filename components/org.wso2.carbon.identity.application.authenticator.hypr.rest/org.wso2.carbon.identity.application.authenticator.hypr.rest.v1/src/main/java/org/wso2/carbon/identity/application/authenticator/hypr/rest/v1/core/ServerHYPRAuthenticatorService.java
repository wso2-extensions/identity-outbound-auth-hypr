/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.hypr.rest.v1.core;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRAuthnFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.StateResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRAuthorizationAPIClient;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.APIError;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.ErrorResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.v1.StatusResponse;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

/**
 * The ServerHYPRAuthenticatorService class contains all the functional tasks handled by the HYPR REST API,
 * such as getting the authentication status of a user provided the session key.
 */
public class ServerHYPRAuthenticatorService {

    /**
     * Get the authentication status of the user with the given session key via an API call to the HYPR server.
     *
     * @param sessionKey The session key assigned for the user by the framework.
     * @return StatusResponse
     */
    public StatusResponse getAuthenticationStatus(String sessionKey) {

        try {
            // Get the authentication context based on the session key.
            AuthenticationContext authenticationContext = getAuthenticationContext(sessionKey);

            // Extract the hypr configurations.
            Map<String, String> hyprConfigurations = getHyprConfigurations(authenticationContext);

            // Extract hypr authentication properties.
            Map<String, String> hyprAuthenticationProperties = getHyprAuthenticationProperties(authenticationContext);

            // If the authentication status property has assigned with one of the terminating status
            // (i.e. "COMPLETED", "FAILED", "CANCELED"), avoid making API call to the HYPR server.
            String previousState = hyprAuthenticationProperties.get(HyprAuthenticatorConstants.HYPR.AUTH_STATUS);
            if (HyprAuthenticatorConstants.HYPR.TERMINATING_STATUSES.contains(previousState)) {
                StatusResponse statusResponse = new StatusResponse();
                statusResponse.setStatus(StatusResponse.StatusEnum.fromValue(previousState));
                statusResponse.setSessionKey(sessionKey);

                return statusResponse;
            }

            // Make an API call to get the authentication status from the HYPR server.
            StateResponse stateResponse = HYPRAuthorizationAPIClient.getAuthenticationStatus(
                    hyprConfigurations.get(HyprAuthenticatorConstants.HYPR.BASE_URL),
                    hyprConfigurations.get(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN),
                    hyprAuthenticationProperties.get(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID));

            String currentState = stateResponse.getCurrentState();

            // Store the state.
            authenticationContext.setProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS, currentState);

            // Return the state as a REST API response.
            StatusResponse statusResponse = new StatusResponse();
            statusResponse.setStatus(StatusResponse.StatusEnum.fromValue(currentState));
            statusResponse.setSessionKey(sessionKey);

            return statusResponse;

        } catch (HYPRAuthnFailedException e) {
            if (HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES.getCode()
                    .equals(e.getErrorCode())) {
                // Handle invalid request id.
                throw handleInvalidInput(HyprAuthenticatorConstants
                        .ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES);
            } else if (HyprAuthenticatorConstants.ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode()
                    .equals(e.getErrorCode())) {
                // Handle invalid or expired api token.
                throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                        HyprAuthenticatorConstants.ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE);
            }
        }

        throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_GENERAL);
    }

    /**
     * Get the authentication context based on the session key.
     *
     * @param sessionKey The session key assigned for the user by the framework.
     */
    private AuthenticationContext getAuthenticationContext(String sessionKey) {

        AuthenticationContext authenticationContext = FrameworkUtils.getAuthenticationContextFromCache(sessionKey);
        if (authenticationContext == null) {
            throw handleInvalidInput(HyprAuthenticatorConstants.ErrorMessages.CLIENT_ERROR_INVALID_SESSION_KEY);
        }
        return authenticationContext;
    }

    /**
     * Extract the HYPR authenticator configurations from the context.
     *
     * @param sessionContext The authentication context for the given session key.
     */
    private Map<String, String> getHyprConfigurations(AuthenticationContext sessionContext) {

        Map<String, String> authenticatorProperties = sessionContext.getAuthenticatorProperties();

        if (!(authenticatorProperties.containsKey(HyprAuthenticatorConstants.HYPR.BASE_URL) &&
                authenticatorProperties.containsKey(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN))) {
            throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                    HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATOR_CONFIGURATIONS);
        }

        String baseUrl = authenticatorProperties.get(HyprAuthenticatorConstants.HYPR.BASE_URL);
        String apiToken = authenticatorProperties.get(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN);

        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(apiToken)) {
            throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                    HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATOR_CONFIGURATIONS);
        }

        Map<String, String> hyprConfigurations = new HashMap<>();
        hyprConfigurations.put(HyprAuthenticatorConstants.HYPR.BASE_URL, baseUrl);
        hyprConfigurations.put(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN, apiToken);

        return hyprConfigurations;
    }

    /**
     * Extract the user authentication properties such as authentication status and request ID from the context.
     *
     * @param authenticationContext The authentication context for the given session key.
     */
    private Map<String, String> getHyprAuthenticationProperties(AuthenticationContext authenticationContext) {

        if (authenticationContext.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS) == null ||
                authenticationContext.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID) == null) {
            throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                    HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES);
        }

        String authStatus = String.valueOf(authenticationContext.getProperty(
                HyprAuthenticatorConstants.HYPR.AUTH_STATUS));
        String authRequestID = String.valueOf(authenticationContext.getProperty(
                HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID));

        if (StringUtils.isBlank(authStatus) || StringUtils.isBlank(authRequestID)) {
            throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                    HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES);
        }

        Map<String, String> hyprAuthenticationProperties = new HashMap<>();
        hyprAuthenticationProperties.put(HyprAuthenticatorConstants.HYPR.AUTH_STATUS, authStatus);
        hyprAuthenticationProperties.put(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID, authRequestID);

        return hyprAuthenticationProperties;
    }

    private APIError handleInvalidInput(HyprAuthenticatorConstants.ErrorMessages errorEnum, String... data) {

        return handleError(Response.Status.BAD_REQUEST, errorEnum);
    }

    private APIError handleError(Response.Status status, HyprAuthenticatorConstants.ErrorMessages error) {

        return new APIError(status, getErrorBuilder(error).build());
    }

    private ErrorResponse.Builder getErrorBuilder(HyprAuthenticatorConstants.ErrorMessages errorEnum) {

        return new ErrorResponse.Builder().withCode(errorEnum.getCode()).withMessage(errorEnum.getMessage())
                .withDescription(errorEnum.getDescription());
    }
}
