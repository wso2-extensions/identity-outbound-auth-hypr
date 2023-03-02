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

package org.wso2.carbon.identity.application.authenticator.hypr.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRAuthnFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRClientException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.DeviceAuthenticationRequest;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.DeviceAuthenticationResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevice;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevicesResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RequestIDResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.ResponseEntity;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.State;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.StateResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRAuthorizationAPIClient;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRWebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mockStatic;
import static org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants.HYPR.HYPR_AUTH_PATH;
import static org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants.HYPR.HYPR_AUTH_STATUS_CHECK_PATH;
import static org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants.HYPR.HYPR_USER_DEVICE_INFO_PATH;

/**
 * The HYPRAuthorizationAPIClientTest class contains all the test cases corresponding to the HYPRAuthorizationAPIClient
 * class.
 */
public class HYPRAuthorizationAPIClientTest {

    private static final String apiToken = "testApiToken";
    private static final String appID = "testApp";
    private static final String baseUrl = "https://wso2.hypr.com";
    private static final String machineId = "testMachineID";
    private static final String username = "testUser";
    private static final String deviceId = "testDeviceID";
    private static final String protocolVersion = null;
    private static final String modelNumber = "testModelNumber";
    private static final String requestId = "testRequestId";
    private static final String statusCompleted = "COMPLETED";
    private static final String statusRequestSent = "REQUEST_SENT";
    private static URI initiateAuthenticationURL;
    private static URI deviceInfoURL;
    private static URI authenticationStatusPollURL;
    private MockedStatic<HYPRWebUtils> mockedHyprWebUtils;
    private Gson gson;

    @BeforeClass
    public void setUp() throws URISyntaxException {

        deviceInfoURL =
                new URIBuilder(baseUrl).setPath(HYPR_USER_DEVICE_INFO_PATH + appID + "/" + username + "/devices")
                        .build();
        authenticationStatusPollURL =
                new URIBuilder(baseUrl).setPath(HYPR_AUTH_STATUS_CHECK_PATH + requestId).build();
        initiateAuthenticationURL = new URIBuilder(baseUrl).setPath(HYPR_AUTH_PATH).build();
        gson = new Gson();
    }

    @BeforeMethod
    public void methodSetUp() {

        mockedHyprWebUtils = mockStatic(HYPRWebUtils.class);
    }

    @AfterMethod
    public void methodClose() {

        mockedHyprWebUtils.close();
    }

    @Test(description = "Test case for getRegisteredDevicesRequest() method for a valid username.")
    public void testGetRegisteredDevicesRequestWithValidUsername() throws IOException, HYPRAuthnFailedException {

        RegisteredDevice registeredDevice = new RegisteredDevice();
        registeredDevice.setDeviceId(deviceId);
        registeredDevice.setProtocolVersion(protocolVersion);
        registeredDevice.setMachineId(machineId);
        registeredDevice.setModelNumber(modelNumber);
        registeredDevice.setNamedUser(username);

        List<RegisteredDevice> registeredDevices = new ArrayList<>();
        registeredDevices.add(registeredDevice);
        RegisteredDevicesResponse expectedRegisteredDevicesResponse = new RegisteredDevicesResponse(registeredDevices);

        String registeredDevicesJson = gson.toJson(registeredDevices);

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, null));
        response.setEntity(new StringEntity(registeredDevicesJson));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL)).thenReturn(response);

        RegisteredDevicesResponse retrievedRegisteredDevicesResponse =
                HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);

        Assert.assertEquals(expectedRegisteredDevicesResponse.getRegisteredDevices().get(0).getDeviceId(),
                (retrievedRegisteredDevicesResponse.getRegisteredDevices().get(0).getDeviceId()));

    }

    @DataProvider(name = "getRegisteredDevicesRequestApiErrorResponseProviders")
    public Object[][] getRegisteredDevicesRequestApiErrorResponseProviders() {

        return new Object[][]{
                {HttpStatus.SC_UNAUTHORIZED, HyprAuthenticatorConstants
                        .ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE},
                {HttpStatus.SC_INTERNAL_SERVER_ERROR, HyprAuthenticatorConstants
                        .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE}
        };
    }

    @Test(dataProvider = "getRegisteredDevicesRequestApiErrorResponseProviders", description = "Test " +
            "getRegisteredDevicesRequest() method for error response handling.")
    public void testGetRegisteredDevicesRequestWithInvalidParameters(
            int httpStatusCode, HyprAuthenticatorConstants.ErrorMessages errorMessage) {

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                httpStatusCode, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL)).thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), errorMessage.getCode());
        }
    }

    @DataProvider(name = "getRegisteredDevicesRequestExceptionProviders")
    public Object[][] getRegisteredDevicesRequestExceptionProviders() {

        return new Object[][]{
                {IOException.class, HyprAuthenticatorConstants.ErrorMessages.
                        AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE},
                {HYPRClientException.class, HyprAuthenticatorConstants
                        .ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT}
        };
    }

    @Test(dataProvider = "getRegisteredDevicesRequestExceptionProviders", description = "Test " +
            "getRegisteredDevicesRequest() method for exception handling")
    public void testGetRegisteredDevicesRequestWithExceptions(Class<Exception> exceptionClass,
                                                              HyprAuthenticatorConstants.ErrorMessages errorMessage) {

        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL))
                .thenThrow(exceptionClass);

        try {
            HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), errorMessage.getCode());
        }
    }

    public String getDeviceAuthenticationRequestJson(String username, String machineId, String appID)
            throws NoSuchAlgorithmException {

        DeviceAuthenticationRequest deviceAuthenticationRequest =
                new DeviceAuthenticationRequest(username, machineId, appID);

        Gson gson = new GsonBuilder().create();
        return gson.toJson(deviceAuthenticationRequest);
    }

    @Test(description = "Test case for initiateAuthenticationRequest() method for a valid username and machineId.")
    public void testInitiateAuthenticationRequestWithValidParameters() throws NoSuchAlgorithmException,
            HYPRAuthnFailedException, UnsupportedEncodingException {

        String jsonRequestBody = getDeviceAuthenticationRequestJson(username, machineId, appID);

        ResponseEntity authenticationRequestResponseEntity = new ResponseEntity();
        authenticationRequestResponseEntity.setResponseCode(HttpStatus.SC_OK);

        RequestIDResponse requestIDResponse = new RequestIDResponse();
        requestIDResponse.setRequestId(requestId);

        DeviceAuthenticationResponse expectedDeviceAuthenticationResponse = new DeviceAuthenticationResponse();
        expectedDeviceAuthenticationResponse.setStatus(authenticationRequestResponseEntity);
        expectedDeviceAuthenticationResponse.setResponse(requestIDResponse);

        String expectedDeviceAuthenticationResponseJson = gson.toJson(expectedDeviceAuthenticationResponse);

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, null));
        response.setEntity(new StringEntity(expectedDeviceAuthenticationResponseJson));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody))
                .thenReturn(response);

        DeviceAuthenticationResponse retrievedDeviceAuthenticationResponse =
                HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);

        Assert.assertEquals(expectedDeviceAuthenticationResponse.getResponse().getRequestId(),
                (retrievedDeviceAuthenticationResponse.getResponse().getRequestId()));
    }

    @DataProvider(name = "getInitiateAuthenticationRequestApiErrorResponseProviders")
    public Object[][] getInitiateAuthenticationRequestApiErrorResponseProviders() {

        return new Object[][]{
                {HttpStatus.SC_BAD_REQUEST, HyprAuthenticatorConstants
                        .ErrorMessages.AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_INVALID_USER},
                {HttpStatus.SC_UNAUTHORIZED, HyprAuthenticatorConstants
                        .ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE},
                {HttpStatus.SC_INTERNAL_SERVER_ERROR, HyprAuthenticatorConstants
                        .ErrorMessages.AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE}
        };
    }

    @Test(dataProvider = "getInitiateAuthenticationRequestApiErrorResponseProviders", description = "Test case for " +
            "initiateAuthenticationRequest() method for either an invalid username or machineId or HYPR API token.")
    public void testInitiateAuthenticationRequestWithInvalidParameters(
            int httpStatusCode, HyprAuthenticatorConstants.ErrorMessages errorMessage) throws NoSuchAlgorithmException {

        String jsonRequestBody = getDeviceAuthenticationRequestJson(username, machineId, appID);

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, httpStatusCode,
                null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody))
                .thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), errorMessage.getCode());
        }
    }

    @Test(description = "Test case for initiateAuthenticationRequest() method where an error occurred while " +
            "retrieving the hash algorithm.")
    public void testInitiateAuthenticationRequestWithRetrievingHashAlgorithmFailure() {

        mockedHyprWebUtils.when(HYPRWebUtils::getRandomPinSha256).thenThrow(NoSuchAlgorithmException.class);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE.getCode());
        }
    }

    @DataProvider(name = "getInitiateAuthenticationRequestExceptionProviders")
    public Object[][] getInitiateAuthenticationRequestExceptionProviders() {

        return new Object[][]{
                {IOException.class, HyprAuthenticatorConstants.ErrorMessages.
                        AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE},
                {HYPRClientException.class, HyprAuthenticatorConstants
                        .ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT}
        };
    }

    @Test(dataProvider = "getInitiateAuthenticationRequestExceptionProviders", description = "Test " +
            " initiateAuthenticationRequest() method for exception handling")
    public void testInitiateAuthenticationRequestWithExceptions(Class<Exception> exceptionClass,
                                                                HyprAuthenticatorConstants.ErrorMessages errorMessage)
            throws NoSuchAlgorithmException {

        String jsonRequestBody = getDeviceAuthenticationRequestJson(username, machineId, appID);

        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody))
                .thenThrow(exceptionClass);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), errorMessage.getCode());
        }
    }

    @Test(description = "Test case for getAuthenticationStatus() method for a valid requestId.")
    public void testGetAuthenticationStatusWithValidRequestId() throws UnsupportedEncodingException,
            HYPRAuthnFailedException {

        List<State> states = new ArrayList<>();
        states.add(new State(statusRequestSent, ""));
        states.add(new State(statusCompleted, ""));

        StateResponse expectedStateResponse = new StateResponse(requestId, username, states);

        String expectedStateResponseJson = gson.toJson(expectedStateResponse);

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, null));
        response.setEntity(new StringEntity(expectedStateResponseJson));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL)).thenReturn(response);

        StateResponse retrievedStateResponse =
                HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);

        Assert.assertEquals(expectedStateResponse.getRequestId(), (retrievedStateResponse.getRequestId()));
        Assert.assertEquals(expectedStateResponse.getCurrentState(), (retrievedStateResponse.getCurrentState()));
    }

    @DataProvider(name = "getAuthenticationStatusApiErrorResponseProviders")
    public Object[][] getAuthenticationStatusApiErrorResponseProviders() {

        return new Object[][]{
                {HttpStatus.SC_BAD_REQUEST, HyprAuthenticatorConstants
                        .ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES},
                {HttpStatus.SC_UNAUTHORIZED, HyprAuthenticatorConstants
                        .ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE},
                {HttpStatus.SC_INTERNAL_SERVER_ERROR, HyprAuthenticatorConstants
                        .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_AUTHENTICATION_STATUS_FAILURE}
        };
    }

    @Test(dataProvider = "getAuthenticationStatusApiErrorResponseProviders", description = "Test case for " +
            "getAuthenticationStatus() method for invalid authentication properties such as requestId & HYPR " +
            "API token.")
    public void testGetAuthenticationStatusWithInvalidAuthProperties(
            int httpStatusCode, HyprAuthenticatorConstants.ErrorMessages errorMessage) {

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, httpStatusCode,
                null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL)).thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), errorMessage.getCode());
        }
    }

    @DataProvider(name = "getAuthenticationStatusExceptionProviders")
    public Object[][] getAuthenticationStatusExceptionProviders() {

        return new Object[][]{
                {IOException.class, HyprAuthenticatorConstants.ErrorMessages.
                        AUTHENTICATION_FAILED_RETRIEVING_AUTHENTICATION_STATUS_FAILURE},
                {HYPRClientException.class, HyprAuthenticatorConstants
                        .ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT}
        };
    }

    @Test(dataProvider = "getAuthenticationStatusExceptionProviders", description = "Test case for " +
            "getAuthenticationStatus() method exception handling")
    public void testGetAuthenticationStatusWithExceptions(Class<Exception> exceptionClass,
                                                          HyprAuthenticatorConstants.ErrorMessages errorMessage) {

        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL))
                .thenThrow(exceptionClass);
        try {
            HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);
            // If the flow worked without throwing any errors the test case should fail.
            Assert.fail();
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), errorMessage.getCode());
        }
    }

}
