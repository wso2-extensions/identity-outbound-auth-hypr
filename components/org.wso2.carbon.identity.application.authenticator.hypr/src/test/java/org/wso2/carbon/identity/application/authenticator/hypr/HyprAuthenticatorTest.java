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

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.DeviceAuthenticationResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevice;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevicesResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RequestIDResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.ResponseEntity;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRAuthorizationAPIClient;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class HyprAuthenticatorTest {

    private static final String apiToken = "testApiToken";
    private static final String appID = "testApp";
    private static final String baseUrl = "https://wso2.hypr.com";
    private static final String sessionDataKey = "testSessionKey";
    private static final String deviceId = "testDeviceID";
    private static final String protocolVersion = null;
    private static final String modelNumber = "testModelNumber";
    private static final String machineId = "testMachineID";
    private static final String username = "testUser";


    private HyprAuthenticator hyprAuthenticator;

    @Mock
    private HyprAuthenticator mockedHyprAuthenticator;
    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Spy
    private AuthenticationContext context;

    @Spy
    private HyprAuthenticator spy;

    private MockedStatic<HYPRAuthorizationAPIClient> mockedHyprAuthorizationAPIClient;

    private AutoCloseable autoCloseable;
    private MockedStatic<ServiceURLBuilder> mockedServiceURLBuilder;

    @BeforeClass
    public void setUp() {
// TODO ; enable and check
        autoCloseable = MockitoAnnotations.openMocks(this);
        hyprAuthenticator = new HyprAuthenticator();
        mockedHyprAuthorizationAPIClient = mockStatic(HYPRAuthorizationAPIClient.class);
        mockedServiceURLBuilder = mockStatic(ServiceURLBuilder.class);
    }

    @AfterClass
    public void close() throws Exception {
        mockedHyprAuthorizationAPIClient.close();
        mockedServiceURLBuilder.close();
        autoCloseable.close();
    }

    private void mockServiceURLBuilder() {

        ServiceURLBuilder builder = new ServiceURLBuilder() {

            String path = "";

            @Override
            public ServiceURLBuilder addPath(String... strings) {

                Arrays.stream(strings).forEach(x -> path += "/" + x);
                return this;
            }

            @Override
            public ServiceURLBuilder addParameter(String s, String s1) {

                return this;
            }

            @Override
            public ServiceURLBuilder setFragment(String s) {

                return this;
            }

            @Override
            public ServiceURLBuilder addFragmentParameter(String s, String s1) {

                return this;
            }

            @Override
            public ServiceURL build() {

                ServiceURL serviceURL = mock(ServiceURL.class);
                when(serviceURL.getAbsolutePublicURL()).thenReturn("https://localhost:9443" + path);
                when(serviceURL.getRelativePublicURL()).thenReturn(path);
                when(serviceURL.getRelativeInternalURL()).thenReturn(path);
                return serviceURL;
            }
        };

        when(ServiceURLBuilder.create()).thenReturn(builder);
    }

    @Test(description = "Test case for canHandle() method.")
    public void testCanHandle() {

        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        Assert.assertTrue(hyprAuthenticator.canHandle(httpServletRequest));

        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY)).thenReturn(null);
        Assert.assertFalse(hyprAuthenticator.canHandle(httpServletRequest));
    }

    @Test(description = "Test case for getContextIdentifier() method.")
    public void testGetContextIdentifier() {

        when(httpServletRequest.getParameter("sessionDataKey")).thenReturn(sessionDataKey);
        Assert.assertEquals(hyprAuthenticator.getContextIdentifier(httpServletRequest), sessionDataKey);

        when(httpServletRequest.getParameter("sessionDataKey")).thenReturn(null);
        Assert.assertNull(hyprAuthenticator.getContextIdentifier(httpServletRequest));
    }

    @Test(description = "Test case for getFriendlyName() method.")
    public void testGetFriendlyName() {

        Assert.assertEquals(hyprAuthenticator.getFriendlyName(),
                HyprAuthenticatorConstants.HYPR.AUTHENTICATOR_FRIENDLY_NAME);
    }

    @Test(description = "Test case for getName() method.")
    public void testGetName() {

        Assert.assertEquals(hyprAuthenticator.getName(), HyprAuthenticatorConstants.HYPR.AUTHENTICATOR_NAME);
    }


    @Test(description = "Test case for process() method where the user requests to initiate the login with HYPR.")
    public void testProcessWithUsernameFalseAuthStatusFalse()
            throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(null);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.AUTH_STATUS)).thenReturn(null);
        doNothing().when(spy).initiateAuthenticationRequest(httpServletRequest, httpServletResponse, context);
        AuthenticatorFlowStatus status = spy.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(description = "Test case for process() method where user has successfully completed the authentication " +
            "with HYPR.")
    public void testProcessWithAuthStatusCompleted() throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(null);
        context.setProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS, "COMPLETED");
        doNothing().when(spy).processAuthenticationResponse(httpServletRequest, httpServletResponse, context);
        AuthenticatorFlowStatus status = spy.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test(description = "Test case for process() method where user's authentication flow is still in the pending " +
            "stage.")
    public void testProcessWithAuthStatusPending() throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(null);
        context.setProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS, "PENDING");

        mockServiceURLBuilder();
        AuthenticatorFlowStatus status = hyprAuthenticator.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(description = "Test case for process() method where user cancels the authentication request from the " +
            "registered device side.")
    public void testProcessWithAuthStatusCanceled() throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(null);
        context.setProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS, "CANCELED");

        mockServiceURLBuilder();
        AuthenticatorFlowStatus status = hyprAuthenticator.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(description = "Test case for process() method with session data key and auth status as 'FAILED' " +
            "and no username.")
    public void testProcessWithAuthStatusFailed() throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(null);
        context.setProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS, "FAILED");

        mockServiceURLBuilder();
        AuthenticatorFlowStatus status = hyprAuthenticator.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(description = "Test case for process() method where user initiates the login request with a valid username.")
    public void testProcessWithValidUsername() throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(username);

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(HyprAuthenticatorConstants.HYPR.USERNAME, new String[]{username});
        when(httpServletRequest.getParameterMap()).thenReturn(parameterMap);

        mockAuthenticatorContext();

        mockServiceURLBuilder();

        RegisteredDevice registeredDevice = new RegisteredDevice();
        registeredDevice.setDeviceId(deviceId);
        registeredDevice.setProtocolVersion(protocolVersion);
        registeredDevice.setMachineId(machineId);
        registeredDevice.setModelNumber(modelNumber);
        registeredDevice.setNamedUser(username);

        List<RegisteredDevice> registeredDevices = new ArrayList<>();
        registeredDevices.add(registeredDevice);

        RegisteredDevicesResponse registeredDevicesResponse = new RegisteredDevicesResponse(registeredDevices);

        mockedHyprAuthorizationAPIClient
                .when(() -> HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username))
                .thenReturn(registeredDevicesResponse);

        ResponseEntity authenticationRequestResponseEntity = new ResponseEntity();
        authenticationRequestResponseEntity.setResponseCode(200);

        RequestIDResponse requestIDResponse = new RequestIDResponse();
        requestIDResponse.setRequestId("testRequestId");

        DeviceAuthenticationResponse deviceAuthenticationResponse = new DeviceAuthenticationResponse();
        deviceAuthenticationResponse.setStatus(authenticationRequestResponseEntity);
        deviceAuthenticationResponse.setResponse(requestIDResponse);

        mockedHyprAuthorizationAPIClient
                .when(() -> HYPRAuthorizationAPIClient.initiateAuthenticationRequest(
                        baseUrl, appID, apiToken, username, machineId))
                .thenReturn(deviceAuthenticationResponse);

        AuthenticatorFlowStatus status = spy.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
        Assert.assertEquals(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_STATUS),
                HyprAuthenticatorConstants.HYPR.AuthenticationStatus.PENDING.getName());
        Assert.assertEquals(context.getProperty(HyprAuthenticatorConstants.HYPR.AUTH_REQUEST_ID),
                "testRequestId");
        Assert.assertEquals(context.getProperty(HyprAuthenticatorConstants.HYPR.USERNAME), username);
    }

    @Test(description = "Test case for process() method where user initiates the login request with an invalid " +
            "username.")
    public void testProcessWithInvalidUsername() throws AuthenticationFailedException, LogoutFailedException {

        doReturn(true).when(mockedHyprAuthenticator).canHandle(httpServletRequest);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.SESSION_DATA_KEY))
                .thenReturn(sessionDataKey);
        when(httpServletRequest.getParameter(HyprAuthenticatorConstants.HYPR.USERNAME)).thenReturn(username);

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(HyprAuthenticatorConstants.HYPR.USERNAME, new String[]{username});
        when(httpServletRequest.getParameterMap()).thenReturn(parameterMap);

        mockAuthenticatorContext();
        mockServiceURLBuilder();

        List<RegisteredDevice> registeredDevices = new ArrayList<>();

        RegisteredDevicesResponse registeredDevicesResponse = new RegisteredDevicesResponse(registeredDevices);
        mockedHyprAuthorizationAPIClient
                .when(() -> HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username))
                .thenReturn(registeredDevicesResponse);

        AuthenticatorFlowStatus status = spy.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    private void mockAuthenticatorContext() {

        Map<String, String> authenticatorProperties = new HashMap<>();
        authenticatorProperties.put(HyprAuthenticatorConstants.HYPR.BASE_URL, baseUrl);
        authenticatorProperties.put(HyprAuthenticatorConstants.HYPR.APP_ID, appID);
        authenticatorProperties.put(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN, apiToken);
        when(context.getAuthenticatorProperties()).thenReturn(authenticatorProperties);
    }
}
