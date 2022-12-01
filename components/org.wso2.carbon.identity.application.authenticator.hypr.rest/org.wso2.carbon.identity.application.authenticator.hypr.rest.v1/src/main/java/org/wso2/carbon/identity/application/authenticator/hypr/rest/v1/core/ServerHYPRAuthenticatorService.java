/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.HYPRConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.APIError;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.ErrorResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.v1.StatusResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;

/**
 * The ServerHYPRAuthenticatorService class contains all the functional tasks handled by the HYPR REST API,
 * such as getting the authentication status of a user provided the session key.
 */
public class ServerHYPRAuthenticatorService {

    private static final Log log = LogFactory.getLog(ServerHYPRAuthenticatorService.class);

    /**
     * Get the authentication status of the user with the given session key via an API call to the HYPR server.
     *
     * @param sessionKey The session key assigned for the user by the framework.
     */
    public StatusResponse getAuthenticationStatus(String sessionKey) {

        try {
            //Get the authentication context based on the session key.
            AuthenticationContext sessionContext = getAuthenticationContext(sessionKey);

            // Extract the hypr configurations.
            Map<String, String> hyprConfigurations = getHyprConfigurations(sessionContext);

            // Extract hypr authentication properties.
            Map<String, String> hyprAuthenticationProperties = getHyprAuthenticationProperties(sessionContext);

            // If the authentication status property has assigned with one of the terminating status
            // (i.e. "COMPLETED", "FAILED", "CANCELED"), avoid making API call to the HYPR server.
            if (Arrays.asList(HYPRConstants.TERMINATING_STATUSES)
                    .contains(hyprAuthenticationProperties.get(HYPRConstants.AUTH_STATUS))) {
                StatusResponse statusResponse = new StatusResponse();
                statusResponse.setStatus(StatusResponse.StatusEnum
                        .fromValue(hyprAuthenticationProperties.get(HYPRConstants.AUTH_STATUS)));
                statusResponse.setSessionKey(sessionKey);

                return statusResponse;
            }

            // Make an API call to get the authentication status from the HYPR server.
            // URL : {{baseUrl}}/rp/api/oob/client/authentication/requests/{{requestId}}
            String authenticationStatusPollURL = String.format("%s%s%s",
                    hyprConfigurations.get(HYPRConstants.BASE_URL),
                    HYPRConstants.HYPR_AUTH_STATUS_CHECK_PATH,
                    hyprAuthenticationProperties.get(HYPRConstants.AUTH_REQUEST_ID));

            HttpGet request = new HttpGet(authenticationStatusPollURL);
            request.addHeader("Authorization", "Bearer " + hyprConfigurations.get(HYPRConstants.API_TOKEN));
            request.addHeader("Content-Type", "application/json");

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {

                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode authenticationStatusJsonNode = toJsonNode(response);
                    if (!authenticationStatusJsonNode.isEmpty()) {
                        if (authenticationStatusJsonNode.has("state") &&
                                !authenticationStatusJsonNode.get("state").isEmpty()) {
                            String status = authenticationStatusJsonNode.get("state")
                                    .get(authenticationStatusJsonNode.get("state").size() - 1)
                                    .get("value")
                                    .toString()
                                    .replace("\"", "");

                            // Store the status.
                            sessionContext.setProperty(HYPRConstants.AUTH_STATUS, status);

                            // Return the status as a response.
                            StatusResponse statusResponse = new StatusResponse();
                            statusResponse.setStatus(StatusResponse.StatusEnum.fromValue(status));
                            statusResponse.setSessionKey(sessionKey);

                            return statusResponse;
                        }
                    }
                    throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                            HYPRConstants.ErrorMessage.SERVER_ERROR_RETRIEVING_AUTHENTICATION_STATUS);

                } else if (response.getStatusLine().getStatusCode() == 400) {
                    // Inform the requestId is invalid.
                    throw handleInvalidInput(HYPRConstants.ErrorMessage.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES);
                }
            }
        } catch (IOException e) {
            //Internal server error when converting the response to jsonNode
            throw handleException(e, HYPRConstants.ErrorMessage.SERVER_ERROR_RETRIEVING_AUTHENTICATION_STATUS, false);
        }
        throw handleError(Response.Status.INTERNAL_SERVER_ERROR, HYPRConstants.ErrorMessage.SERVER_ERROR_GENERAL);
    }

    /**
     * Get the authentication context based on the session key.
     *
     * @param sessionKey The session key assigned for the user by the framework.
     */
    private AuthenticationContext getAuthenticationContext(String sessionKey) {
        AuthenticationContext sessionContext = FrameworkUtils.getAuthenticationContextFromCache(sessionKey);

        if (sessionContext == null) {
            throw handleInvalidInput(HYPRConstants.ErrorMessage.CLIENT_ERROR_INVALID_SESSION_KEY);
        }
        return sessionContext;
    }

    /**
     * Extract the HYPR authenticator configurations from the context.
     *
     * @param sessionContext The authentication context for the given session key.
     */
    private Map<String, String> getHyprConfigurations(AuthenticationContext sessionContext) {

        Map<String, String> authenticatorProperties = sessionContext.getAuthenticatorProperties();

        if (!(authenticatorProperties.containsKey(HYPRConstants.BASE_URL) &&
                authenticatorProperties.containsKey(HYPRConstants.API_TOKEN))) {
            throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                    HYPRConstants.ErrorMessage.SERVER_ERROR_INVALID_AUTHENTICATOR_CONFIGURATIONS);
        }

        Map<String, String> hyprConfigurations = new HashMap<>();
        hyprConfigurations.put(HYPRConstants.BASE_URL, authenticatorProperties.get(HYPRConstants.BASE_URL));
        hyprConfigurations.put(HYPRConstants.API_TOKEN, authenticatorProperties.get(HYPRConstants.API_TOKEN));

        return hyprConfigurations;
    }

    /**
     * Extract the user authentication properties such as authentication status and request ID from the context.
     *
     * @param sessionContext The authentication context for the given session key.
     */
    private Map<String, String> getHyprAuthenticationProperties(AuthenticationContext sessionContext) {

        if (sessionContext.getProperty(HYPRConstants.AUTH_STATUS) == null ||
                sessionContext.getProperty(HYPRConstants.AUTH_REQUEST_ID) == null) {
            throw handleError(Response.Status.INTERNAL_SERVER_ERROR,
                    HYPRConstants.ErrorMessage.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES);
        }

        Map<String, String> hyprAuthenticationProperties = new HashMap<>();
        hyprAuthenticationProperties.put(HYPRConstants.AUTH_STATUS,
                (String) sessionContext.getProperty(HYPRConstants.AUTH_STATUS));
        hyprAuthenticationProperties.put(HYPRConstants.AUTH_REQUEST_ID,
                (String) sessionContext.getProperty(HYPRConstants.AUTH_REQUEST_ID));
        return hyprAuthenticationProperties;
    }

    /**
     * Convert the HTTPResponse to a json node.
     *
     * @param response A HTTPResponse object received from API call.
     * @throws IOException
     */
    private JsonNode toJsonNode(CloseableHttpResponse response) throws IOException {

        JsonNode rootNode = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            // Convert to a string
            String result = EntityUtils.toString(entity);
            rootNode = new ObjectMapper().readTree(new StringReader(result));
        }
        return rootNode;
    }

    public APIError handleInvalidInput(HYPRConstants.ErrorMessage errorEnum, String... data) {

        return handleError(Response.Status.BAD_REQUEST, errorEnum);
    }

    private APIError handleException(Exception e, HYPRConstants.ErrorMessage errorEnum, boolean isClientException,
                                     String... data) {

        ErrorResponse errorResponse;
        if (data != null) {
            errorResponse = getErrorBuilder(errorEnum).build(log, String.format(errorEnum.getDescription(),
                    (Object[]) data), isClientException);
        } else {
            errorResponse = getErrorBuilder(errorEnum).build(log, errorEnum.getDescription(), isClientException);
        }

        if (isClientException) {
            return new APIError(Response.Status.BAD_REQUEST, errorResponse);
        } else {
            return new APIError(Response.Status.INTERNAL_SERVER_ERROR, errorResponse);
        }

    }

    private APIError handleError(Response.Status status, HYPRConstants.ErrorMessage error) {

        return new APIError(status, getErrorBuilder(error).build());
    }

    private ErrorResponse.Builder getErrorBuilder(HYPRConstants.ErrorMessage errorEnum) {

        return new ErrorResponse.Builder().withCode(errorEnum.getCode()).withMessage(errorEnum.getMessage())
                .withDescription(errorEnum.getDescription());
    }
}
