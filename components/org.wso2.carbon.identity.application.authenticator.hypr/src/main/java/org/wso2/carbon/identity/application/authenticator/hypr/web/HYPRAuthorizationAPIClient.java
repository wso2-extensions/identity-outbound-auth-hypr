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
    private final Map<String, String> hyprConfiguration;

    public HYPRAuthorizationAPIClient(final String baseUrl, final String appId, final String apiToken) {
        this.hyprConfiguration = new HashMap<>();
        this.hyprConfiguration.put(HyprAuthenticatorConstants.HYPR.BASE_URL, baseUrl);
        this.hyprConfiguration.put(HyprAuthenticatorConstants.HYPR.APP_ID, appId);
        this.hyprConfiguration.put(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN, apiToken);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String getBaseUrl() {
        return this.hyprConfiguration.get(HyprAuthenticatorConstants.HYPR.BASE_URL);
    }

    public String getAppId() {
        return this.hyprConfiguration.get(HyprAuthenticatorConstants.HYPR.APP_ID);
    }

    public String getApiToken() {
        return this.hyprConfiguration.get(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN);
    }

    public Map<String, String> getHyprConfiguration() {
        return this.hyprConfiguration;
    }

    /**
     * Call the HYPR server API to retrieve the registered devices.
     *
     * @param username          The username provided by the user.
     * @return response         A HTTPResponse object.
     * @throws HYPRAuthnFailedException
     * */
    public HttpResponse getRegisteredDevicesRequest (String username) throws HYPRAuthnFailedException {
        //{{baseUrl}}/rp/ api/oob/client/authentication/{{appId}}/{{username}}/devices


            String deviceInfoURL = String.format("%s%s%s/%s/devices", this.getBaseUrl(),
                    HyprAuthenticatorConstants.HYPR.HYPR_USER_DEVICE_INFO_PATH, this.getAppId(), username);

            try {
                HttpResponse response = HYPRWebUtils.jsonGet(this.getHyprConfiguration(), deviceInfoURL);

                if (response.getStatusLine().getStatusCode() > 400) {
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
     * @param namedUser          The username provided by the user.
     * @param machineId          The machineId unique per user.
     * @return response         A HTTPResponse object.
     * @throws HYPRAuthnFailedException
     * */
    public HttpResponse initiateAuthenticationRequest(String namedUser, String machineId)
            throws HYPRAuthnFailedException {
        try {

            String initiateAuthenticationURL = String.format("%s%s", this.getBaseUrl(),
                    HyprAuthenticatorConstants.HYPR.HYPR_AUTH_PATH);

            Map<String, String> requestBodyMap = new HashMap<>();
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.APP_ID, this.getAppId());
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.MACHINE_ID, machineId);
            requestBodyMap.put(HyprAuthenticatorConstants.HYPR.NAMED_USER, namedUser);
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

            HttpResponse response = HYPRWebUtils.jsonPost(this.getHyprConfiguration(), initiateAuthenticationURL,
                    jsonRequestBody);

            if (response.getStatusLine().getStatusCode() > 400) {
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
