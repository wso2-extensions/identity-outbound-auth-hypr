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

/**
 * Includes all the constants variables used by the HYPR authenticator.
 */
public class HyprAuthenticatorConstants {

    /**
     * Includes the error codes and the corresponding error messages.
     */
    public enum ErrorMessages {

        AUTHENTICATION_FAILED_REDIRECTING_LOGIN_FAILURE("HYPR-65001",
                "Authentication failed when redirecting the user to the login page."),
        AUTHENTICATION_FAILED_BUILDING_LOGIN_URL_FAILURE("HYPR-65002",
                "Authentication when building the login URL."),
        AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE("HYPR-65003",
                "Authentication failed retrieving the registered devices."),
        AUTHENTICATION_FAILED_EXTRACTING_MACHINE_ID_FAILURE("HYPR-65004",
                "Authentication failed when extracting the machine ID of the user."),
        AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE("HYPR-65005",
                "Authentication failed when sending a push notification to the registered device."),
        AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE("HYPR-65006",
                "Authentication failed retrieving the hash algorithm."),
        AUTHENTICATION_FAILED_EXTRACTING_REQUEST_ID_FAILURE("HYPR-65007",
                "Authentication failed when extracting the request ID provided by the HYPR server upon " +
                        "initiating the send push notification request."),
        HYPR_BASE_URL_INVALID_FAILURE("HYPR-65008", "Provided HYPR base URL is invalid."),
        HYPR_APP_ID_INVALID_FAILURE("HYPR-65009", "Provided HYPR app ID is invalid."),
        HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE("HYPR-65010",
                "Provided HYPR endpoint API token is either invalid or expired");

        private final String code;
        private final String message;

        /**
         * Create an Error Message.
         *
         * @param code    Relevant error code.
         * @param message Relevant error message.
         */
        ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
        }

        /**
         * To get the code of specific error.
         *
         * @return Error code.
         */
        public String getCode() {

            return code;
        }

        /**
         * To get the message of specific error.
         *
         * @return Error message.
         */
        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return String.format("%s  - %s", code, message);
        }
    }

    /**
     * Includes the HTTP header parameters.
     */
    public static class HTTP {

        // HTTP header parameters
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER = "Bearer ";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String APPLICATION_JSON = "application/json";
    }

    /**
     * Includes the HYPR authentication and registration related constants.
     */
    public static class HYPR {

        public static final String AUTHENTICATOR_NAME = "HYPRAuthenticator";
        public static final String AUTHENTICATOR_FRIENDLY_NAME = "HYPR";
        public static final String SESSION_DATA_KEY = "sessionDataKey";
        public static final String USERNAME = "username";
        public static final String AUTH_STATUS = "authStatus";
        public static final String AUTH_REQUEST_ID = "authRequestId";

        //Configurable parameters
        public static final String BASE_URL = "baseUrl";
        public static final String APP_ID = "appId";
        public static final String HYPR_API_TOKEN = "apiToken";

        // API Parameters
        public static final String MACHINE = "machine";
        public static final String MACHINE_ID = "machineId";
        public static final String REQUEST_ID = "requestId";
        public static final String RESPONSE = "response";
        public static final String NAMED_USER = "namedUser";
        public static final String SESSION_NONCE = "sessionNonce";
        public static final String DEVICE_NONCE = "deviceNonce";
        public static final String SERVICE_NONCE = "serviceNonce";
        public static final String SERVICE_MAC = "serviceHmac";

        // Authentication API paths
        public static final String HYPR_USER_DEVICE_INFO_PATH = "/rp/api/oob/client/authentication/";
        public static final String HYPR_AUTH_PATH = "/rp/api/oob/client/authentication/requests";

        //Page paths
        public static final String HYPR_LOGIN_PAGE = "/authenticationendpoint/hyprlogin.jsp";

        /**
         * Object holding authentication mobile response status.
         */
        public enum AuthenticationStatus {

            INVALID_TOKEN("INVALID_TOKEN", "Authentication failed due to an internal server error." +
                    "To fix this, contact your system administrator."),
            INVALID_REQUEST("INVALID_REQUEST", "Invalid username provided"),
            PENDING("PENDING", "Authentication with HYPR is in progress. Awaiting for the user to " +
                    "authenticate via the registered smart device"),
            COMPLETED("COMPLETED", "Authentication successfully completed."),
            FAILED("FAILED", "Authentication failed. Try again."),
            CANCELED("CANCELED", "Authentication with HYPR was cancelled by the user.");

            private final String name;
            private final String message;

            /**
             * Create an Error Message.
             *
             * @param name    Relevant error code.
             * @param message Relevant error message.
             */
            AuthenticationStatus(String name, String message) {

                this.name = name;
                this.message = message;
            }

            public String getName() {

                return name;
            }

            public String getMessage() {

                return message;
            }
        }
    }
}
