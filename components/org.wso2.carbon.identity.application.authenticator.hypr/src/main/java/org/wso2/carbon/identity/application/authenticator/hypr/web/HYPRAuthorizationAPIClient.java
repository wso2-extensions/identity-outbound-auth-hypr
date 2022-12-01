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

package org.wso2.carbon.identity.application.authenticator.hypr.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.exception.HYPRAuthnFailedException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * The HYPRAuthorizationAPIClient class contains all the functions related to handling the API calls to the HYPR server.
 **/
public class HYPRAuthorizationAPIClient {

    private final ObjectMapper objectMapper;
    // HYPR Configuration parameters
    private final String baseUrl;
    private final String appId;
    private final String apiToken;

    public HYPRAuthorizationAPIClient(final String baseUrl, final String appId, final String apiToken) {

        this.baseUrl = baseUrl;
        this.appId = appId;
        this.apiToken = apiToken;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Call the HYPR server API to retrieve the registered devices.
     *
     * @param username The username provided by the user.
     * @return response         A HTTPResponse object.
     * @throws HYPRAuthnFailedException Exception throws when there is an error occurred when retrieving the
     *                                  registered devices via the api call
     */
    public HttpResponse getRegisteredDevicesRequest(String username) throws HYPRAuthnFailedException {

        // Device info URL : {{baseUrl}}/rp/ api/oob/client/authentication/{{appId}}/{{username}}/devices
        String deviceInfoURL = String.format("%s%s%s/%s/devices", this.baseUrl,
                HyprAuthenticatorConstants.HYPR.HYPR_USER_DEVICE_INFO_PATH, this.appId, username);

        try {
            HttpResponse response = HYPRWebUtils.jsonGet(this.apiToken, deviceInfoURL);

            if (response.getStatusLine().getStatusCode() == 401) {
                throw new HYPRAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.
                                HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode(),
                        HyprAuthenticatorConstants.ErrorMessages.
                                HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getMessage());
            } else if (response.getStatusLine().getStatusCode() > 400) {
                throw new HYPRAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.
                                AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getCode(),
                        HyprAuthenticatorConstants.ErrorMessages.
                                AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getMessage());
            }
            return response;

        } catch (IOException e) {
            throw new HYPRAuthnFailedException(
                    HyprAuthenticatorConstants.ErrorMessages
                            .AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getCode(),
                    HyprAuthenticatorConstants.ErrorMessages.
                            AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE.getMessage(), e);
        }
    }

    /**
     * Call the HYPR server API to initiate the authentication request via sending the push notification to the
     * registered device.
     *
     * @param username  The username provided by the user.
     * @param machineId The machineId unique per user.
     * @return response         A HTTPResponse object.
     * @throws HYPRAuthnFailedException Exception throws when there is an error occurred when initiating the
     *                                  authentication with HYPR server
     */
    public HttpResponse initiateAuthenticationRequest(String username, String machineId)
            throws HYPRAuthnFailedException {

        try {
            String initiateAuthenticationURL = String.format("%s%s", this.baseUrl,
                    HyprAuthenticatorConstants.HYPR.HYPR_AUTH_PATH);

            Map<String, String> requestBodyMap = new HashMap<>();
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.APP_ID, this.appId);
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.MACHINE_ID, machineId);
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.NAMED_USER, username);
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.MACHINE, "WEB");
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.SESSION_NONCE, HYPRWebUtils.doSha256(
                    String.valueOf(HYPRWebUtils.generateRandomPIN())));
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.DEVICE_NONCE, HYPRWebUtils.doSha256(
                    String.valueOf(HYPRWebUtils.generateRandomPIN())));
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.SERVICE_NONCE, HYPRWebUtils.doSha256(
                    String.valueOf(HYPRWebUtils.generateRandomPIN())));
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.SERVICE_MAC, HYPRWebUtils.doSha256(
                    String.valueOf(HYPRWebUtils.generateRandomPIN())));

            String jsonRequestBody = this.objectMapper.writeValueAsString(requestBodyMap);

            HttpResponse response = HYPRWebUtils.jsonPost(this.apiToken, initiateAuthenticationURL,
                    jsonRequestBody);

            if (response.getStatusLine().getStatusCode() == 401) {
                throw new HYPRAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.
                                HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getCode(),
                        HyprAuthenticatorConstants.ErrorMessages.
                                HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE.getMessage());
            } else if (response.getStatusLine().getStatusCode() >= 400) {
                throw new HYPRAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages
                                .AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE.getCode(),
                        HyprAuthenticatorConstants.ErrorMessages.
                                AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE.getMessage());
            }
            return response;

        } catch (IOException e) {
            throw new HYPRAuthnFailedException(
                    HyprAuthenticatorConstants.ErrorMessages
                            .AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE.getCode(),
                    HyprAuthenticatorConstants.ErrorMessages.
                            AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new HYPRAuthnFailedException(
                    HyprAuthenticatorConstants.ErrorMessages
                            .AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE.getCode(),
                    HyprAuthenticatorConstants.ErrorMessages.
                            AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE.getMessage(), e);
        }
    }
}
