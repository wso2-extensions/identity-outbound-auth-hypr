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

package org.wso2.carbon.identity.application.authenticator.hypr.common.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRAuthnFailedException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRClientException;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.DeviceAuthenticationRequest;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.DeviceAuthenticationResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevice;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.RegisteredDevicesResponse;
import org.wso2.carbon.identity.application.authenticator.hypr.common.model.StateResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * The HYPRAuthorizationAPIClient class contains all the functions related to handling the API calls to the HYPR server.
 **/
public class HYPRAuthorizationAPIClient {

    /**
     * Call the HYPR server API to retrieve the registered devices.
     *
     * @param username The username provided by the user.
     * @return response         A HTTPResponse object.
     * @throws HYPRAuthnFailedException Exception throws when there is an error occurred when retrieving the
     *                                  registered devices via the api call
     */
    public static RegisteredDevicesResponse getRegisteredDevicesRequest(String baseUrl, String appId, String apiToken,
                                                                        String username)
            throws HYPRAuthnFailedException {

        // Device info URL :
        // {{baseUrl}}/rp/ api/oob/client/authentication/{{appId}}/{{username}}/devices
        String deviceInfoURL = String.format("%s%s%s/%s/devices", baseUrl,
                HyprAuthenticatorConstants.HYPR.HYPR_USER_DEVICE_INFO_PATH, appId, username);

        try {
            HttpResponse response = HYPRWebUtils.httpGet(apiToken, deviceInfoURL);

            if (response.getStatusLine().getStatusCode() == 401) {
                throw getHyprAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE);
            } else if (response.getStatusLine().getStatusCode() > 400) {
                throw getHyprAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE);
            }

            Gson gson = new GsonBuilder().create();
            HttpEntity entity = response.getEntity();
            String jsonString = EntityUtils.toString(entity);
            Type listType = new TypeToken<List<RegisteredDevice>>() {
            }.getType();

            return new RegisteredDevicesResponse(gson.fromJson(jsonString, listType));

        } catch (IOException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                    .AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE, e);
        } catch (HYPRClientException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                    .SERVER_ERROR_CREATING_HTTP_CLIENT, e);
        }
    }

    /**
     * Call the HYPR server API to initiate the authentication request via sending the push notification to the
     * registered device.
     *
     * @param baseUrl   The baseURL provided from the HYPR.
     * @param appId     The ID of the application created via the HYPR control center.
     * @param apiToken  The API token generated from the application created via the HYPR control center.
     * @param username  The username provided by the user.
     * @param machineId The machineId unique per user.
     * @return DeviceAuthenticationResponse
     * @throws HYPRAuthnFailedException Exception throws when there is an error occurred when initiating the
     *                                  authentication with HYPR server
     */
    public static DeviceAuthenticationResponse initiateAuthenticationRequest(
            String baseUrl, String appId, String apiToken, String username, String machineId)
            throws HYPRAuthnFailedException {

        try {

            String initiateAuthenticationURL = String.format("%s%s", baseUrl,
                    HyprAuthenticatorConstants.HYPR.HYPR_AUTH_PATH);

            DeviceAuthenticationRequest deviceAuthenticationRequest =
                    new DeviceAuthenticationRequest(username, machineId, appId);

            Gson gson = new GsonBuilder().create();
            String jsonRequestBody = gson.toJson(deviceAuthenticationRequest);
            HttpResponse response = HYPRWebUtils.httpPost(apiToken, initiateAuthenticationURL, jsonRequestBody);

            if (response.getStatusLine().getStatusCode() == 401) {
                throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                        .HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE);
            } else if (response.getStatusLine().getStatusCode() >= 400) {
                throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                        .AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE);
            }

            HttpEntity entity = response.getEntity();
            String jsonString = EntityUtils.toString(entity);

            return gson.fromJson(jsonString, DeviceAuthenticationResponse.class);

        } catch (IOException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                    .AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE, e);
        } catch (NoSuchAlgorithmException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                    .AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE, e);
        } catch (HYPRClientException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                    .SERVER_ERROR_CREATING_HTTP_CLIENT, e);
        }
    }

    /**
     * Call the HYPR server API to retrieve the authentication status of the user.
     *
     * @param baseUrl   The baseURL provided from the HYPR.
     * @param apiToken  The API token generated from the application created via the HYPR control center.
     * @param requestId A unique identifier provided by HYPR upon successfully initiating the push notification to the
     *                  registered devices.
     * @return StateResponse
     * @throws HYPRAuthnFailedException Exception throws when there is an error occurred when receiving the
     *                                  authentication status.
     */
    public static StateResponse getAuthenticationStatus(String baseUrl, String apiToken, String requestId)
            throws HYPRAuthnFailedException {

        // Get authentication status URL :
        // {{baseUrl}}/rp/api/oob/client/authentication/requests/{{requestId}}
        String authenticationStatusPollURL = String.format("%s%s%s", baseUrl,
                HyprAuthenticatorConstants.HYPR.HYPR_AUTH_STATUS_CHECK_PATH, requestId);

        try {
            HttpResponse response = HYPRWebUtils.httpGet(apiToken, authenticationStatusPollURL);

            if (response.getStatusLine().getStatusCode() == 200) {

                HttpEntity entity = response.getEntity();
                String jsonString = EntityUtils.toString(entity);

                Gson gson = new GsonBuilder().create();
                StateResponse stateResponse = gson.fromJson(jsonString, StateResponse.class);

                return stateResponse;

            } else if (response.getStatusLine().getStatusCode() == 400) {
                throw getHyprAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES);

            } else if (response.getStatusLine().getStatusCode() == 401) {
                throw getHyprAuthnFailedException(
                        HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_INVALID_API_TOKEN);
            }

        } catch (IOException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants
                    .ErrorMessages.AUTHENTICATION_FAILED_RETRIEVING_AUTHENTICATION_STATUS_FAILURE, e);
        } catch (HYPRClientException e) {
            throw getHyprAuthnFailedException(HyprAuthenticatorConstants.ErrorMessages
                    .SERVER_ERROR_CREATING_HTTP_CLIENT, e);
        }

        return null;
    }

    private static HYPRAuthnFailedException getHyprAuthnFailedException(
            HyprAuthenticatorConstants.ErrorMessages errorMessage) {

        return new HYPRAuthnFailedException(errorMessage.getCode(), errorMessage.getMessage());
    }

    private static HYPRAuthnFailedException getHyprAuthnFailedException(
            HyprAuthenticatorConstants.ErrorMessages errorMessage, Exception e) {

        return new HYPRAuthnFailedException(errorMessage.getCode(), errorMessage.getMessage(), e);
    }

}
