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

package org.wso2.carbon.identity.application.authenticator.hypr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authenticator.hypr.exception.HYPRAuthnFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.web.HYPRAuthorizationAPIClient;
import org.wso2.carbon.identity.application.authenticator.hypr.web.HYPRWebUtils;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.wso2.carbon.identity.application.authenticator.hypr.HyprAuthenticatorConstants.ErrorMessages;
import static org.wso2.carbon.identity.application.authenticator.hypr.HyprAuthenticatorConstants.HYPR;

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

        return HYPR.AUTHENTICATOR_NAME;
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
            String sessionDataKey = request.getParameter(HYPR.SESSION_DATA_KEY);
            redirectHYPRLoginPage(response, sessionDataKey, null);
        } catch (AuthenticationFailedException e) {
            String errorMessage = "Error occurred when trying to redirect user to the login page.";
            throw new AuthenticationFailedException(errorMessage, e);
        }
    }

    /**
     * Redirect the user to the HYPR login page with authentication status and messages, if there is any.
     *
     * @param response             The response that is received to the authenticator.
     * @param sessionDataKey       The session data key extracted from the request.
     * @param authenticationStatus The authentication status of the user when authenticating via HYPR.
     * @throws HYPRAuthnFailedException Exception thrown while redirecting user to login page.
     */
    private void redirectHYPRLoginPage(HttpServletResponse response, String sessionDataKey,
                                       HYPR.AuthenticationStatus authenticationStatus)
            throws HYPRAuthnFailedException {

        try {
            ServiceURLBuilder hyprLoginPageURLBuilder = ServiceURLBuilder.create()
                    .addPath(HYPR.HYPR_LOGIN_PAGE)
                    .addParameter(HYPR.SESSION_DATA_KEY, sessionDataKey)
                    .addParameter("AuthenticatorName",
                            HYPR.AUTHENTICATOR_FRIENDLY_NAME);

            if (authenticationStatus != null) {
                hyprLoginPageURLBuilder.addParameter("status", String.valueOf(authenticationStatus.getName()));
                hyprLoginPageURLBuilder.addParameter(
                        "message", String.valueOf(authenticationStatus.getMessage()));
            }

            String hyprLoginPageURL = hyprLoginPageURLBuilder.build().getAbsolutePublicURL();
            response.sendRedirect(hyprLoginPageURL);

        } catch (IOException e) {
            throw new HYPRAuthnFailedException(
                    ErrorMessages.AUTHENTICATION_FAILED_REDIRECTING_LOGIN_FAILURE.getCode(),
                    ErrorMessages.AUTHENTICATION_FAILED_REDIRECTING_LOGIN_FAILURE
                            .getMessage(), e);
        } catch (URLBuilderException e) {
            throw new HYPRAuthnFailedException(
                    ErrorMessages.AUTHENTICATION_FAILED_BUILDING_LOGIN_URL_FAILURE.getCode(),
                    ErrorMessages.AUTHENTICATION_FAILED_BUILDING_LOGIN_URL_FAILURE
                            .getMessage(), e);
        }
    }

    /**
     * Send a push notification to the user-registered devices to start the user authentication process using
     * HYPR, the external identity provider.
     *
     * @param request  The request that is received by the authenticator.
     * @param response The response that is received to the authenticator.
     * @param context  The Authentication context received by the authenticator.
     * @throws AuthenticationFailedException Exception thrown while sending push notification to the registered device.
     */
    private void initiateHYPRAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                   AuthenticationContext context) throws AuthenticationFailedException {


        String username = request.getParameter(HYPR.USERNAME);
        String sessionDataKey = request.getParameter(HYPR.SESSION_DATA_KEY);

        if (StringUtils.isBlank(username)) {
            redirectHYPRLoginPage(response, sessionDataKey, HYPR.AuthenticationStatus.INVALID_REQUEST);
            return;
        }

        // Create HYPRAuthorizationAPIClient to make rest api calls.
        HYPRAuthorizationAPIClient hyprAuthorizationAPIClient = getHYPRAPIClient(context);

        // Get the registered devices.
        ArrayNode registeredDevices = getRegisteredDevices(username, hyprAuthorizationAPIClient);

        // If an empty array received for the registered devices redirect user back to the login page and
        // display "Invalid username" since a HYPR user cannot exist without a set of registered devices.
        if (registeredDevices.isEmpty()) {
            redirectHYPRLoginPage(response, sessionDataKey, HYPR.AuthenticationStatus.INVALID_REQUEST);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully retrieved the registered devices for the user : " + username);
        }

        // Extract the user specific machineId.
        String machineId = getMachineId(registeredDevices);

        if (StringUtils.isBlank(machineId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieved machine ID for the username " + username + " is either null or empty.");
            }
            redirectHYPRLoginPage(response, sessionDataKey, HYPR.AuthenticationStatus.FAILED);
            return;
        }

        // Send a push notification and extract the requestId received from the HYPR server.
        String requestId = getRequestIDFromSendPushNotification(username, machineId, hyprAuthorizationAPIClient);

        if (StringUtils.isBlank(requestId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieved request ID for the authentication request for the username " + username +
                        " is either null or empty.");
            }
            redirectHYPRLoginPage(response, sessionDataKey, HYPR.AuthenticationStatus.FAILED);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully sent a push notification for the registered devices of the user" + username);
        }

        // Store the HYPR context information.
        context.setProperty(HYPR.AUTH_STATUS, HYPR.AuthenticationStatus.PENDING.getName());
        context.setProperty(HYPR.AUTH_REQUEST_ID, requestId);
        context.setProperty(HYPR.USERNAME, username);

        // Inform the user that the push notification has been sent to the registered device.
        redirectHYPRLoginPage(response, sessionDataKey, HYPR.AuthenticationStatus.PENDING);

    }

    /**
     * Create an instance of the HYPRAuthorizationAPIClient.
     *
     * @param context The Authentication context received by the authenticator.
     * @return HYPRAuthorizationAPIClient      An instance of a HYPRAuthorizationAPIClient class.
     * @throws HYPRAuthnFailedException Exception while retrieving the authenticator properties.
     */
    private HYPRAuthorizationAPIClient getHYPRAPIClient(AuthenticationContext context) throws HYPRAuthnFailedException {

        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
        String baseUrl = authenticatorProperties.get(HYPR.BASE_URL);
        String appId = authenticatorProperties.get(HYPR.APP_ID);
        String apiToken = authenticatorProperties.get(HYPR.HYPR_API_TOKEN);

        if (StringUtils.isBlank(baseUrl)) {
            throw new HYPRAuthnFailedException(
                    ErrorMessages.HYPR_BASE_URL_INVALID_FAILURE.getCode(),
                    ErrorMessages.HYPR_BASE_URL_INVALID_FAILURE.getMessage());
        }

        if (StringUtils.isBlank(appId)) {
            throw new HYPRAuthnFailedException(
                    ErrorMessages.HYPR_APP_ID_INVALID_FAILURE.getCode(),
                    ErrorMessages.HYPR_APP_ID_INVALID_FAILURE.getMessage());
        }

        if (StringUtils.isBlank(apiToken)) {
            // TODO: Check for the token expiry. Redirect to the error page
            // Authentication failed. Contact the admin ....
            throw new HYPRAuthnFailedException(
                    ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode(),
                    ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getMessage());
        }

        return new HYPRAuthorizationAPIClient(baseUrl, appId, apiToken);
    }

    /**
     * Get the list of devices registered with the provided username.
     *
     * @param username                   Username provided by the user.
     * @param hyprAuthorizationAPIClient An instance of a HYPRAuthorizationAPIClient class.
     * @return registeredDevices               An ArrayNode of the user registered devices.
     * @throws HYPRAuthnFailedException Throws an exception when there is an error occurred when retrieving the
     *                                  registered devices.
     */
    private ArrayNode getRegisteredDevices(String username, HYPRAuthorizationAPIClient hyprAuthorizationAPIClient)
            throws HYPRAuthnFailedException {

        try {
            HttpResponse hyprRegisteredDevicesResponse =
                    hyprAuthorizationAPIClient.getRegisteredDevicesRequest(username);

            JsonNode registeredDevices = HYPRWebUtils.toJsonNode(hyprRegisteredDevicesResponse);
            if (registeredDevices.isArray() && !registeredDevices.isEmpty()) {
                return (ArrayNode) registeredDevices;
            }

            return new ObjectMapper().createArrayNode();
        } catch (IOException e) {
            throw new HYPRAuthnFailedException(
                    ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getCode(),
                    ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getMessage(), e);
        }
    }

    /**
     * Send a push notification to the registered device and extract the requestId provided by the HYPR server which
     * can be later used to poll the status of the authentication process.
     *
     * @param username                   Username provided by the user.
     * @param machineId                  An identifier provided when registering the devices. Unique per user.
     * @param hyprAuthorizationAPIClient An instance of a HYPRAuthorizationAPIClient class.
     * @return requestId                       A unique identifier provided by the HYPR server that can be used when
     * polling for the authentication status
     */
    private String getRequestIDFromSendPushNotification(String username, String machineId,
                                                        HYPRAuthorizationAPIClient hyprAuthorizationAPIClient)
            throws HYPRAuthnFailedException {

        // Send a push notification.
        HttpResponse hyprPushNotificationResponse =
                hyprAuthorizationAPIClient.initiateAuthenticationRequest(username, machineId);

        // Extract the requestId received from the HYPR server response and return.
        return getHyprRequestId(hyprPushNotificationResponse);
    }

    /**
     * Extracts the machine Id (Unique per user) from the received response for the get registered device request.
     *
     * @param registeredDevices The JsonNode that includes the list of user registered devices.
     */
    private String getMachineId(ArrayNode registeredDevices) {

        String machineId = null;
        if (!registeredDevices.isEmpty()) {
            for (JsonNode deviceJsonNode : registeredDevices) {
                if (deviceJsonNode.has(HYPR.MACHINE_ID)) {
                    machineId = deviceJsonNode.get(HYPR.MACHINE_ID).toString().replace("\"", "");
                    break;
                }
            }
        }
        return machineId;
    }

    /**
     * Extracts the request Id (Unique per authentication request) from the received response for the push notification
     * request.
     *
     * @param hyprPushNotificationResponse The response that is received when requested to send push notification.
     * @throws HYPRAuthnFailedException Exception thrown while extracting the request ID.
     */
    private static String getHyprRequestId(HttpResponse hyprPushNotificationResponse) throws HYPRAuthnFailedException {

        try {
            String requestId = null;
            JsonNode pushNotificationResponseJsonNode = HYPRWebUtils.toJsonNode(hyprPushNotificationResponse);
            if (pushNotificationResponseJsonNode.has(HYPR.RESPONSE)) {
                JsonNode responseJsonNode = pushNotificationResponseJsonNode.get(HYPR.RESPONSE);
                if (responseJsonNode.has(HYPR.REQUEST_ID)) {
                    requestId = responseJsonNode.get(HYPR.REQUEST_ID).toString().replace("\"", "");
                }
            }
            return requestId;
        } catch (IOException e) {
            throw new HYPRAuthnFailedException(
                    ErrorMessages.AUTHENTICATION_FAILED_EXTRACTING_REQUEST_ID_FAILURE.getCode(),
                    ErrorMessages.AUTHENTICATION_FAILED_EXTRACTING_REQUEST_ID_FAILURE.getMessage(), e);
        }
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
            LOG.debug("Successfully logged in the user : " + username);
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
            String sessionDataKey = request.getParameter(HYPR.SESSION_DATA_KEY);
            String authStatus = (String) context.getProperty(HYPR.AUTH_STATUS);

            if (authStatus.equals(HYPR.AuthenticationStatus.COMPLETED.getName())) {
                processAuthenticationResponse(request, response, context);
                return AuthenticatorFlowStatus.SUCCESS_COMPLETED;

            } else if (authStatus.equals(HYPR.AuthenticationStatus.PENDING.getName())) {
                handleAuthenticationIncompleteState(response, sessionDataKey, HYPR.AuthenticationStatus.PENDING);
                return AuthenticatorFlowStatus.INCOMPLETE;

            } else if (authStatus.equals(HYPR.AuthenticationStatus.CANCELED.getName())) {
                handleAuthenticationIncompleteState(response, sessionDataKey, HYPR.AuthenticationStatus.CANCELED);
                return AuthenticatorFlowStatus.INCOMPLETE;

            } else if (authStatus.equals(HYPR.AuthenticationStatus.FAILED.getName())) {
                handleAuthenticationIncompleteState(response, sessionDataKey, HYPR.AuthenticationStatus.FAILED);
                return AuthenticatorFlowStatus.INCOMPLETE;
            }
        } else {
            // Redirect the user to the login page.
            initiateAuthenticationRequest(request, response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        }
        return super.process(request, response, context);
    }

    /**
     * Retrieve the user session context stored in the HYPR context manager.
     *
     * @param response             The response that is received to the authenticator.
     * @param sessionDataKey       The session data key extracted from the request.
     * @param authenticationStatus An AuthenticationStatus object which specifies the current status of the
     *                             authentication
     * @throws HYPRAuthnFailedException Exception thrown when redirecting the user to login page on which the error
     *                                  messages are displayed.
     */
    private void handleAuthenticationIncompleteState(HttpServletResponse response, String sessionDataKey,
                                                     HYPR.AuthenticationStatus authenticationStatus)
            throws HYPRAuthnFailedException {

        redirectHYPRLoginPage(response, sessionDataKey, authenticationStatus);
    }
}
