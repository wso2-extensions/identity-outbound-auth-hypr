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

package org.wso2.carbon.identity.application.authenticator.hypr;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants.ErrorMessages;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants.HYPR;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRAuthnFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.DeviceAuthenticationResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevicesResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRAuthorizationAPIClient;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * The HyprAuthenticator class contains all the functional tasks handled by the authenticator with HYPR IdP and
 * WSO2 Identity Server, such as initiating authentication with HYPR, sending a push notification to registered devices
 * and finally authenticating the user upon providing successful FIDO verification.
 */
public class HyprAuthenticator extends AbstractApplicationAuthenticator implements
        FederatedApplicationAuthenticator {

    private static final Log LOG = LogFactory.getLog(HyprAuthenticator.class);

    /**
     * Returns the authenticator's name.
     *
     * @return String  The identifier of the authenticator.
     */
    @Override
    public String getName() {

        return HyprAuthenticatorConstants.HYPR.AUTHENTICATOR_NAME;
    }

    /**
     * Returns authenticator's friendly name.
     *
     * @return String  The display name of the authenticator.
     */
    @Override
    public String getFriendlyName() {

        return HYPR.AUTHENTICATOR_FRIENDLY_NAME;
    }

    /**
     * Returns all user input fields of the authenticator.
     *
     * @return List  Returns the federated authenticator properties.
     */
    @Override
    public List<Property> getConfigurationProperties() {

        // Get the required configuration properties.
        List<Property> configProperties = new ArrayList<>();
        Property baseUrl = new Property();
        baseUrl.setName(HYPR.BASE_URL);
        baseUrl.setDisplayName("Base URL");
        baseUrl.setRequired(true);
        baseUrl.setDescription("Enter the base URL of your HYPR server deployment.");
        baseUrl.setDisplayOrder(1);
        configProperties.add(baseUrl);

        Property relyingPartyAppId = new Property();
        relyingPartyAppId.setName(HYPR.APP_ID);
        relyingPartyAppId.setDisplayName("Relying Party App ID");
        relyingPartyAppId.setRequired(true);
        relyingPartyAppId.setDescription("Enter the relying party app ID in HYPR control center.");
        relyingPartyAppId.setType("string");
        relyingPartyAppId.setDisplayOrder(2);
        configProperties.add(relyingPartyAppId);

        Property apiToken = new Property();
        apiToken.setName(HYPR.HYPR_API_TOKEN);
        apiToken.setDisplayName("API Token");
        apiToken.setRequired(true);
        apiToken.setDescription("Enter the relying party app access token generated in the control center.");
        apiToken.setType("string");
        apiToken.setConfidential(true);
        apiToken.setDisplayOrder(4);
        configProperties.add(apiToken);

        return configProperties;
    }

    /**
     * Returns a unique string to identify each request and response separately.
     * This contains the session data key, processed by the WSO2 IS.
     *
     * @param request The request that is received by the authenticator.
     * @return String  Returns the state parameter value that is carried by the request.
     */
    @Override
    public String getContextIdentifier(HttpServletRequest request) {

        String sessionDataKey = request.getParameter(HYPR.SESSION_DATA_KEY);
        if (StringUtils.isNotBlank(sessionDataKey)) {
            return sessionDataKey;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("A unique identifier cannot be issued for both Request and Response. " +
                        "ContextIdentifier is NULL.");
            }
            return null;
        }
    }

    /**
     * Checks whether the request and response can be handled by the authenticator.
     *
     * @param request The request that is received by the authenticator.
     * @return Boolean Whether the request can be handled by the authenticator.
     */
    @Override
    public boolean canHandle(HttpServletRequest request) {

        // With HYPR if sessionDataKey is not received then the entire flow will break as it highly relied on it.
        return request.getParameter(HYPR.SESSION_DATA_KEY) != null;
    }

    /**
     * Redirects the user to the login page for authentication purposes. This authenticator redirects the user to the
     * HYPR login page deployed with the IS.
     *
     * @param request  The request that is received by the authenticator.
     * @param response Appends the authorized URL once a valid authorized URL is built.
     * @param context  The Authentication context received by the authenticator.
     * @throws AuthenticationFailedException Exception thrown while redirecting the user to the login page.
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {

        try {
            if (context.getLastAuthenticatedUser() != null) {
                // If the user is already authenticated, initiate HYPR authentication request.
                initiateHYPRAuthenticationRequest(request, response, context);
            } else {
                // If the user is not authenticated, redirect to the HYPR login page to prompt username.
                redirectHYPRLoginPage(response, context, null);
            }
        } catch (AuthenticationFailedException e) {
            String errorMessage = "Error occurred when trying to redirect user to the login page.";
            throw new AuthenticationFailedException(errorMessage, e);
        }
    }

    /**
     * Redirect the user to the HYPR login page with authentication status and messages, if there is any.
     *
     * @param response             The response that is received to the authenticator.
     * @param context              The Authentication context received by the authenticator.
     * @param authenticationStatus The authentication status of the user when authenticating via HYPR.
     * @throws HYPRAuthnFailedException Exception thrown while redirecting user to login page.
     */
    private void redirectHYPRLoginPage(HttpServletResponse response, AuthenticationContext context,
                                       HYPR.AuthenticationStatus authenticationStatus)
            throws HYPRAuthnFailedException {

        try {
            ServiceURLBuilder hyprLoginPageURLBuilder = ServiceURLBuilder.create()
                    .addPath(HYPR.HYPR_LOGIN_PAGE)
                    .addParameter(HYPR.SESSION_DATA_KEY, context.getContextIdentifier())
                    .addParameter("AuthenticatorName", HYPR.AUTHENTICATOR_FRIENDLY_NAME)
                    .addParameter(HYPR.TENANT_DOMAIN, context.getTenantDomain());

            if (authenticationStatus != null) {
                hyprLoginPageURLBuilder.addParameter("status", String.valueOf(authenticationStatus.getName()));
                hyprLoginPageURLBuilder.addParameter(
                        "message", String.valueOf(authenticationStatus.getMessage()));
            }

            String hyprLoginPageURL = hyprLoginPageURLBuilder.build().getAbsolutePublicURL();
            response.sendRedirect(hyprLoginPageURL);

        } catch (IOException e) {
            throw getHyprAuthnFailedException(ErrorMessages.AUTHENTICATION_FAILED_REDIRECTING_LOGIN_FAILURE, e);
        } catch (URLBuilderException e) {
            throw getHyprAuthnFailedException(ErrorMessages.AUTHENTICATION_FAILED_BUILDING_LOGIN_URL_FAILURE, e);
        }
    }

    /**
     * Send a push notification to the user-registered devices to start the user authentication process using
     * HYPR, the external identity provider.
     *
     * @param request  The request that is received by the authenticator.
     * @param response The response that is received to the authenticator.
     * @param context  The Authentication context received by the authenticator.
     * @throws HYPRAuthnFailedException Exception thrown while sending push notification to the registered device.
     */
    private void initiateHYPRAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                   AuthenticationContext context) throws AuthenticationFailedException {

        String username = null;
        if (context.getSequenceConfig() != null) {
            Map<Integer, StepConfig> stepConfigMap = context.getSequenceConfig().getStepMap();
            // loop through the authentication steps and find the authenticated user from the subject identifier step.
            if (stepConfigMap != null) {
                for (StepConfig stepConfig : stepConfigMap.values()) {
                    if (stepConfig.getAuthenticatedUser() != null && stepConfig.isSubjectIdentifierStep()) {
                        username = stepConfig.getAuthenticatedUser().getUserName();
                        break;
                    }
                }
            }
        }

        if (StringUtils.isEmpty(username)) {
            username = request.getParameter(HYPR.USERNAME);
        }

        // Extract the HYPR configurations.
        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
        String baseUrl = authenticatorProperties.get(HYPR.BASE_URL);
        String appId = authenticatorProperties.get(HYPR.APP_ID);
        String apiToken = authenticatorProperties.get(HYPR.HYPR_API_TOKEN);

        // Validate username and the HYPR configurable parameters.
        if (StringUtils.isBlank(username)) {
            redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.INVALID_REQUEST);
            return;
        }
        validateHYPRConfiguration(baseUrl, appId, apiToken);

        try {
            // Get the registered devices.
            RegisteredDevicesResponse registeredDevicesResponse = HYPRAuthorizationAPIClient.getRegisteredDevicesRequest
                    (baseUrl, appId, apiToken, username);

            // If an empty array received for the registered devices redirect user back to the login page and
            // display "Invalid username" since a HYPR user cannot exist without a set of registered devices.
            if (registeredDevicesResponse.getRegisteredDevices().isEmpty()) {
                // If HYPR is used as a 2nd factor, disabling the username field and login button in login page
                if (context.getCurrentStep() == 1) {
                    redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.INVALID_REQUEST);
                } else {
                    redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.INVALID_USER);
                }
                return;
            }

            String maskedUsername = getMaskedUsername(username);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully retrieved the registered devices for the user " + maskedUsername);
            }

            // Extract the user specific machineId which is a unique ID across all the registered devices under a
            // particular unique username.
            String machineId = registeredDevicesResponse.getRegisteredDevices().get(0).getMachineId();

            if (StringUtils.isBlank(machineId)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Retrieved machine ID for the user " + maskedUsername + " is either null or empty.");
                }
                redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.FAILED);
                return;
            }

            // Send a push notification and extract the requestId received from the HYPR server.
            DeviceAuthenticationResponse deviceAuthenticationResponse =
                    HYPRAuthorizationAPIClient.initiateAuthenticationRequest(
                            baseUrl, appId, apiToken, username, machineId);
            String requestId = deviceAuthenticationResponse.getResponse().getRequestId();

            if (StringUtils.isBlank(requestId)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Retrieved request ID for the authentication request for the user " + maskedUsername +
                            " is either null or empty.");
                }
                redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.FAILED);
                return;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully sent a push notification for the registered devices of the user " +
                        maskedUsername);
            }

            // Store the HYPR context information.
            context.setProperty(HYPR.AUTH_STATUS, HYPR.AuthenticationStatus.PENDING.getName());
            context.setProperty(HYPR.AUTH_REQUEST_ID, requestId);
            context.setProperty(HYPR.USERNAME, username);

            // Inform the user that the push notification has been sent to the registered device.
            redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.PENDING);

        } catch (HYPRAuthnFailedException e) {
            // Handle invalid or expired token.
            if (ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode().equals(e.getErrorCode())) {
                LOG.error(e.getErrorCode() + " : " + e.getMessage());
                redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.INVALID_TOKEN);
            } else {
                throw new AuthenticationFailedException(e.getMessage(), e);
            }
        }
    }

    private String getMaskedUsername(String username) {
        if (LoggerUtils.isLogMaskingEnable) {
            return LoggerUtils.getMaskedContent(username);
        }
        return username;
    }

    private void validateHYPRConfiguration(String baseUrl, String appId, String apiToken)
            throws HYPRAuthnFailedException {

        if (StringUtils.isBlank(baseUrl)) {
            throw getHyprAuthnFailedException(ErrorMessages.HYPR_BASE_URL_INVALID_FAILURE);
        }

        if (StringUtils.isBlank(appId)) {
            throw getHyprAuthnFailedException(ErrorMessages.HYPR_APP_ID_INVALID_FAILURE);
        }

        if (StringUtils.isBlank(apiToken)) {
            throw getHyprAuthnFailedException(ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE);
        }

    }

    private static HYPRAuthnFailedException getHyprAuthnFailedException(ErrorMessages errorMessage) {

        return new HYPRAuthnFailedException(errorMessage.getCode(), errorMessage.getMessage());
    }

    private static HYPRAuthnFailedException getHyprAuthnFailedException(ErrorMessages errorMessage, Exception e) {

        return new HYPRAuthnFailedException(errorMessage.getCode(), errorMessage.getMessage(), e);
    }

    /**
     * This method is overridden to authenticate user.
     *
     * @param request  The request that is received by the authenticator.
     * @param response The response that is received to the authenticator.
     * @param context  The Authentication context received by authenticator.
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) {

        String username = (String) context.getProperty(HYPR.USERNAME);

        //Set the authenticated user.
        AuthenticatedUser authenticatedUser =
                AuthenticatedUser.createFederateAuthenticatedUserFromSubjectIdentifier(username);
        context.setSubject(authenticatedUser);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully logged in the user " + getMaskedUsername(username));
        }
    }

    @Override
    public AuthenticatorFlowStatus process(HttpServletRequest request, HttpServletResponse response,
                                           AuthenticationContext context)
            throws AuthenticationFailedException, LogoutFailedException {

        if (context.isLogoutRequest()) {
            // if the logout request comes, then no need to go through and complete the flow.
            return AuthenticatorFlowStatus.SUCCESS_COMPLETED;

        } else if (request.getParameterMap().containsKey(HYPR.USERNAME)) {
            // if the login form submission request comes, then go through this flow.
            initiateHYPRAuthenticationRequest(request, response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;

        } else if (context.getProperty(HYPR.AUTH_STATUS) != null) {
            // if intermediate authentication request comes, then go through this flow.
            String authStatus = (String) context.getProperty(HYPR.AUTH_STATUS);

            if (HYPR.AuthenticationStatus.COMPLETED.getName().equals(authStatus)) {
                processAuthenticationResponse(request, response, context);
                return AuthenticatorFlowStatus.SUCCESS_COMPLETED;

            } else if (HYPR.AuthenticationStatus.PENDING.getName().equals(authStatus)) {
                redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.PENDING);
                return AuthenticatorFlowStatus.INCOMPLETE;

            } else if (HYPR.AuthenticationStatus.CANCELED.getName().equals(authStatus)) {
                redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.CANCELED);
                return AuthenticatorFlowStatus.INCOMPLETE;

            } else if (HYPR.AuthenticationStatus.FAILED.getName().equals(authStatus)) {
                redirectHYPRLoginPage(response, context, HYPR.AuthenticationStatus.FAILED);
                return AuthenticatorFlowStatus.INCOMPLETE;
            }
        } else {
            // Redirect the user to the login page.
            initiateAuthenticationRequest(request, response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        }
        return super.process(request, response, context);
    }
}
