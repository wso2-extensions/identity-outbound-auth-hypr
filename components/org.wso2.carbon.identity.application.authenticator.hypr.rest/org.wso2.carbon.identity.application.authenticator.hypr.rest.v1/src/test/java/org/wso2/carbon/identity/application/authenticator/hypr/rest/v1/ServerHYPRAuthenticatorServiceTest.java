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

package org.wso2.carbon.identity.application.authenticator.hypr.rest.v1;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRAuthnFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.State;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.StateResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRAuthorizationAPIClient;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.APIError;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.v1.core.ServerHYPRAuthenticatorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * The ServerHYPRAuthenticatorServiceTest class contains all the test cases corresponding to the
 * ServerHYPRAuthenticatorService class.
 */
public class ServerHYPRAuthenticatorServiceTest {

    private static final String apiToken = "testApiToken";
    private static final String baseUrl = "https://wso2.hypr.com";
    private static final String sessionDataKey = "testSessionKey";
    private static final String requestId = "testRequestId";
    private static final String username = "testUser";
    private static final String statusCompleted = "COMPLETED";
    private static final String statusRequestSent = "REQUEST_SENT";
    private static final String statusInitiated = "INITIATED";
    private static final String statusInitiatedResponse = "INITIATED_RESPONSE";
    private static final String statusFailed = "FAILED";
    private static final String statusCanceled = "CANCELED";
    private static final String statusPending = "PENDING";
    private static Map<String, String> hyprConfigurations;
    private ServerHYPRAuthenticatorService serverHYPRAuthenticatorService;
    private MockedStatic<HYPRAuthorizationAPIClient> mockedHyprAuthorizationAPIClient;
    private MockedStatic<FrameworkUtils> mockedFrameworkUtils;
    @Mock
    private AuthenticationContext context;

    @BeforeClass
    public void setUp() {

        serverHYPRAuthenticatorService = new ServerHYPRAuthenticatorService();
        mockedHyprAuthorizationAPIClient = mockStatic(HYPRAuthorizationAPIClient.class);
        mockedFrameworkUtils = mockStatic(FrameworkUtils.class);

        hyprConfigurations = new HashMap<>();
        hyprConfigurations.put(HyprAuthenticatorConstants.HYPR.BASE_URL, baseUrl);
        hyprConfigurations.put(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN, apiToken);
    }

    @BeforeMethod
    public void methodSetUp() {

        MockitoAnnotations.openMocks(this);
    }

    @AfterClass
    public void close() {

        mockedHyprAuthorizationAPIClient.close();
        mockedFrameworkUtils.close();
    }

    @Test(description = "Test case for handling invalid sessionKey which doesn't have an authentication context.")
    public void testHandleInvalidSessionKey() {

        when(FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey)).thenReturn(null);

        try {
            serverHYPRAuthenticatorService.getAuthenticationStatus(sessionDataKey);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (APIError e) {
            assertEquals(e.getCode(),
                    HyprAuthenticatorConstants.ErrorMessages.CLIENT_ERROR_INVALID_SESSION_KEY.getCode());
        }
    }

    @DataProvider(name = "hyprConfigurationProviders")
    public Object[][] getHyprConfigurationProviders() {

        return new String[][]{
                {baseUrl, null},
                {null, apiToken},
                {null, null}
        };
    }

    @Test(dataProvider = "hyprConfigurationProviders", description = "Test case for handling missing HYPR " +
            "configurations in the extracted authentication context for the provided session key.")
    public void testHandleInvalidHyprConfigurations(String baseUrl, String apiToken) {


        when(context.getProperty(HyprAuthenticatorConstants.HYPR.BASE_URL)).thenReturn(baseUrl);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN)).thenReturn(apiToken);
        when(FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey)).thenReturn(context);

        try {
            serverHYPRAuthenticatorService.getAuthenticationStatus(sessionDataKey);
            // If the flow worked without throwing errors the test case should fail.
            Assert.fail();
        } catch (APIError e) {
            assertEquals(e.getCode(), HyprAuthenticatorConstants
                    .ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATOR_CONFIGURATIONS.getCode());
        }
    }

    @DataProvider(name = "hyprAuthenticationPropertiesProviders")
    public Object[][] getHyprAuthenticationPropertiesProviders() {

        return new String[][]{
                {statusCompleted, null},
                {statusFailed, null},
                {statusCanceled, null},
                {null, requestId},
                {null, null}
        };
    }

    private void mockAuthenticationContext(AuthenticationContext mockAuthenticationContext) {

        when(mockAuthenticationContext.getAuthenticatorProperties()).thenReturn(hyprConfigurations);
    }

    @Test(dataProvider = "hyprAuthenticationPropertiesProviders", description = "Test case for handling missing HYPR " +
            "authentication properties (set via the HYPR authenticator when initiating the authentication flow) in " +
            "the extracted authentication context for the provided session key.")
    public void testHandleInvalidHyprAuthenticationProperties(String authStatus, String requestId) {

        mockAuthenticationContext(context);

        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS)).thenReturn(authStatus);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID)).thenReturn(requestId);
        when(FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey)).thenReturn(context);

        try {
            serverHYPRAuthenticatorService.getAuthenticationStatus(sessionDataKey);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (APIError e) {
            Assert.assertEquals(e.getCode(), HyprAuthenticatorConstants
                    .ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES.getCode());
        }
    }

    @DataProvider(name = "hyprTerminatingAuthStatusProviders")
    public Object[][] getHyprTerminatingAuthStatusProviders() {

        return new String[][]{
                {statusCompleted},
                {statusFailed},
                {statusCanceled},
        };
    }

    @Test(dataProvider = "hyprTerminatingAuthStatusProviders", description = "Test case for handling authentication " +
            "status property extracted from the authentication context already being assigned with a terminating " +
            "status (i.e. 'COMPLETED', 'FAILED', 'CANCELED'), avoid making API call to the HYPR server.")
    public void testHandleExistingAuthenticationStatusWithTerminatingStatus(String authStatus) {

        mockAuthenticationContext(context);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS)).thenReturn(authStatus);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID)).thenReturn(requestId);
        when(FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey)).thenReturn(context);

        StatusResponse statusResponse = new StatusResponse();
        statusResponse.setStatus(StatusResponse.StatusEnum.fromValue(authStatus));
        statusResponse.setSessionKey(sessionDataKey);

        Assert.assertEquals(serverHYPRAuthenticatorService.getAuthenticationStatus(sessionDataKey), statusResponse);
    }

    @DataProvider(name = "hyprAuthStatusProviders")
    public Object[][] getHyprAuthStatusProviders() {

        return new String[][]{
                {statusInitiated},
                {statusInitiatedResponse},
                {statusCompleted},
                {statusFailed},
                {statusCanceled},
        };
    }

    @Test(dataProvider = "hyprAuthStatusProviders", description = "Test case for handling successful authentication " +
            "status retrieving from HYPR server upon providing a valid session key.")
    public void testHandleSuccessfulAuthenticationStatusRetrieving(String retrievedAuthStatus) {

        mockAuthenticationContext(context);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS)).thenReturn(statusPending);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID)).thenReturn(requestId);
        when(FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey)).thenReturn(context);

        List<State> states = new ArrayList<>();
        states.add(new State(statusRequestSent, ""));
        states.add(new State(retrievedAuthStatus, ""));

        StateResponse stateResponse = new StateResponse(requestId, username, states);

        mockedHyprAuthorizationAPIClient
                .when(() -> HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId))
                .thenReturn(stateResponse);

        StatusResponse statusResponse = serverHYPRAuthenticatorService.getAuthenticationStatus(sessionDataKey);

        StatusResponse.StatusEnum currentAuthenticationState = StatusResponse.StatusEnum.REQUEST_SENT;

        switch (statusResponse.getStatus().value()) {
            case statusInitiated:
                currentAuthenticationState = StatusResponse.StatusEnum.INITIATED;
                break;
            case statusInitiatedResponse:
                currentAuthenticationState = StatusResponse.StatusEnum.INITIATED_RESPONSE;
                break;
            case statusCompleted:
                currentAuthenticationState = StatusResponse.StatusEnum.COMPLETED;
                break;
            case statusFailed:
                currentAuthenticationState = StatusResponse.StatusEnum.FAILED;
                break;
            case statusCanceled:
                currentAuthenticationState = StatusResponse.StatusEnum.CANCELED;
                break;
        }
        Assert.assertEquals(statusResponse.getSessionKey(), sessionDataKey);
        Assert.assertEquals(statusResponse.getStatus(), currentAuthenticationState);
    }

    @DataProvider(name = "getInvalidAuthenticationParameterProviders")
    public Object[][] getInvalidAuthenticationParameterProviders() {

        return new Object[][]{
                {HyprAuthenticatorConstants.ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE},
                {HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES}
        };
    }

    @Test(dataProvider = "getInvalidAuthenticationParameterProviders", description = "Test case for handling invalid " +
            " parameters such as requestId or expired HYPR API token.")
    public void testHandleInvalidAPIToken(HyprAuthenticatorConstants.ErrorMessages errorMessage) {

        mockAuthenticationContext(context);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS)).thenReturn(statusPending);
        when(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID)).thenReturn(requestId);
        when(FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey)).thenReturn(context);

        mockedHyprAuthorizationAPIClient
                .when(() -> HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId))
                .thenThrow(new HYPRAuthnFailedException(errorMessage.getCode(), errorMessage.getMessage()));

        try {
            serverHYPRAuthenticatorService.getAuthenticationStatus(sessionDataKey);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (APIError e) {
            Assert.assertEquals(e.getCode(), errorMessage.getCode());
        }
    }

}
