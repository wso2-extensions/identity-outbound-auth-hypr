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
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mockStatic;

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
    private static final String deviceInfoURL = String.format("%s%s%s/%s/devices", baseUrl,
    HyprAuthenticatorConstants.HYPR.HYPR_USER_DEVICE_INFO_PATH, appID, username);
    private static final String initiateAuthenticationURL = String.format("%s%s", baseUrl,
            HyprAuthenticatorConstants.HYPR.HYPR_AUTH_PATH);
    String authenticationStatusPollURL = String.format("%s%s%s", baseUrl,
            HyprAuthenticatorConstants.HYPR.HYPR_AUTH_STATUS_CHECK_PATH, requestId);
    private MockedStatic<HYPRWebUtils> mockedHyprWebUtils;
    private Gson gson;

    @BeforeClass
    public void setUp() {
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

    @Test(description = "Test case for getRegisteredDevicesRequest() method for an invalid HYPR API token provided.")
    public void testGetRegisteredDevicesRequestWithInvalidApiToken() {

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_UNAUTHORIZED, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL)).thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode());
        }
    }

    @Test(description = "Test case for getRegisteredDevicesRequest() method where an error occurred while retrieving" +
            " the registered devices.")
    public void testGetRegisteredDevicesRequestWithGeneralErrors() {

        // Handling error responses retrieved via call HYPR APIs.
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_NOT_FOUND, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL)).thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getCode());
        }

        // Handling IOException thrown when making HTTP calls.
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL)).thenThrow(IOException.class);
        try {
            HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getCode());
        }

    }

    @Test(description = "Test case for getRegisteredDevicesRequest() method where the HTTPClientManager failed to " +
            "pass a valid HTTPClient instance.")
    public void testGetRegisteredDevicesRequestWithHttpClientRetrievingFailure() {

        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, deviceInfoURL))
                .thenThrow(HYPRClientException.class);

        try {
            HYPRAuthorizationAPIClient.getRegisteredDevicesRequest(baseUrl, appID, apiToken, username);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT.getCode());
        }
    }

    @Test(description = "Test case for initiateAuthenticationRequest() method for a valid username and machineId.")
    public void testInitiateAuthenticationRequestWithValidParameters() throws NoSuchAlgorithmException,
            HYPRAuthnFailedException, UnsupportedEncodingException {

        DeviceAuthenticationRequest deviceAuthenticationRequest =
                new DeviceAuthenticationRequest(username, machineId, appID);

        Gson gson = new GsonBuilder().create();
        String jsonRequestBody = gson.toJson(deviceAuthenticationRequest);

        ResponseEntity authenticationRequestResponseEntity = new ResponseEntity();
        authenticationRequestResponseEntity.setResponseCode(200);

        RequestIDResponse requestIDResponse = new RequestIDResponse();
        requestIDResponse.setRequestId("testRequestId");

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

    @Test(description = "Test case for initiateAuthenticationRequest() method for an invalid HYPR API token provided.")
    public void testInitiateAuthenticationRequestWithInvalidApiToken() throws NoSuchAlgorithmException {

        DeviceAuthenticationRequest deviceAuthenticationRequest =
                new DeviceAuthenticationRequest(username, machineId, appID);

        Gson gson = new GsonBuilder().create();
        String jsonRequestBody = gson.toJson(deviceAuthenticationRequest);

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_UNAUTHORIZED, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody))
                .thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode());
        }
    }

    @Test(description = "Test case for initiateAuthenticationRequest() method for either an invalid username or " +
            "machineId.")
    public void testInitiateAuthenticationRequestWithInvalidParameters() throws NoSuchAlgorithmException {

        DeviceAuthenticationRequest deviceAuthenticationRequest =
                new DeviceAuthenticationRequest(username, machineId, appID);

        Gson gson = new GsonBuilder().create();
        String jsonRequestBody = gson.toJson(deviceAuthenticationRequest);

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_BAD_REQUEST, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody))
                .thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE.getCode());
        }
    }

    @Test(description = "Test case for initiateAuthenticationRequest() method where an error occurred while " +
            "retrieving the hash algorithm.")
    public void testInitiateAuthenticationRequestWithRetrievingHashAlgorithmFailure() {

        mockedHyprWebUtils.when(HYPRWebUtils::getRandomPinSha256).thenThrow(NoSuchAlgorithmException.class);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE.getCode());
        }
    }

    @Test(description = "Test case for initiateAuthenticationRequest() method where the HTTPClientManager failed to " +
            "pass a valid HTTPClient instance.")
    public void testInitiateAuthenticationRequestWithHttpClientRetrievingFailure() throws NoSuchAlgorithmException {

        DeviceAuthenticationRequest deviceAuthenticationRequest =
                new DeviceAuthenticationRequest(username, machineId, appID);

        Gson gson = new GsonBuilder().create();
        String jsonRequestBody = gson.toJson(deviceAuthenticationRequest);

        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody))
                .thenThrow(HYPRClientException.class);

        try {
            HYPRAuthorizationAPIClient.initiateAuthenticationRequest(baseUrl, appID, apiToken, username, machineId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT.getCode());
        }
    }

    @Test(description = "Test case for getAuthenticationStatus() method for a valid requestId.")
    public void testGetAuthenticationStatusWithValidRequestId() throws UnsupportedEncodingException,
            HYPRAuthnFailedException {

        List<State> states = new ArrayList<>();
        states.add(new State("REQUEST_SENT", ""));
        states.add(new State("COMPLETED", ""));

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

    @Test(description = "Test case for getAuthenticationStatus() method for an invalid requestId.")
    public void testGetAuthenticationStatusWithInvalidRequestId() {

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_BAD_REQUEST, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL)).thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES.getCode());
        }
    }

    @Test(description = "Test case for getAuthenticationStatus() method for an invalid HYPR API token provided.")
    public void testGetAuthenticationStatusWithInvalidApiToken() {

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_UNAUTHORIZED, null));
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL)).thenReturn(response);

        try {
            HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode());
        }
    }

    @Test(description = "Test case for getAuthenticationStatus() method where an error occurred while retrieving the " +
            "authentication status.")
    public void testGetAuthenticationStatusWithGeneralErrors() {

        // Handling IOException thrown when making HTTP calls.
        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL))
                .thenThrow(IOException.class);
        try {
            HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_AUTHENTICATION_STATUS_FAILURE.getCode());
        }
    }

    @Test(description = "Test case for getAuthenticationStatus() method where the HTTPClientManager failed to " +
            "pass a valid HTTPClient instance.")
    public void testGetAuthenticationStatusWithHttpClientRetrievingFailure() {

        mockedHyprWebUtils.when(() -> HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL))
                .thenThrow(HYPRClientException.class);

        try {
            HYPRAuthorizationAPIClient.getAuthenticationStatus(baseUrl, apiToken, requestId);
        } catch (HYPRAuthnFailedException e) {
            Assert.assertEquals(e.getErrorCode(), HyprAuthenticatorConstants
                    .ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT.getCode());
        }
    }

}
