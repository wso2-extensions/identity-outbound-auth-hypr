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

package org.wso2.carbon.identity.application.authenticator.hypr.common.constants;

import java.util.Arrays;
import java.util.List;

/**
 * Includes all the constants variables used by the HYPR authenticator.
 */
public class HyprAuthenticatorConstants {

    /**
     * Includes the error codes and the corresponding error messages.
     */
    public enum ErrorMessages {

        AUTHENTICATION_FAILED_REDIRECTING_LOGIN_FAILURE("65001",
                "Authentication failed when redirecting the user to the login page."),
        AUTHENTICATION_FAILED_BUILDING_LOGIN_URL_FAILURE("HYPR-65002",
                "Authentication when building the login URL."),
        AUTHENTICATION_FAILED_RETRIEVING_REG_DEVICES_FAILURE("65003",
                "Authentication failed retrieving the registered devices."),
        AUTHENTICATION_FAILED_EXTRACTING_MACHINE_ID_FAILURE("65004",
                "Authentication failed when extracting the machine ID of the user."),
        AUTHENTICATION_FAILED_SENDING_PUSH_NOTIFICATION_FAILURE("65005",
                "Authentication failed when sending a push notification to the registered device."),
        AUTHENTICATION_FAILED_RETRIEVING_HASH_ALGORITHM_FAILURE("65006",
                "Authentication failed retrieving the hash algorithm."),
        AUTHENTICATION_FAILED_EXTRACTING_REQUEST_ID_FAILURE("65007",
                "Authentication failed when extracting the request ID provided by the HYPR server upon " +
                        "initiating the send push notification request."),
        AUTHENTICATION_FAILED_RETRIEVING_AUTHENTICATION_STATUS_FAILURE("65008",
                "Authentication failed when retrieving status of the user authentication."),
        HYPR_BASE_URL_INVALID_FAILURE("65009", "Provided HYPR base URL is invalid."),
        HYPR_APP_ID_INVALID_FAILURE("65010", "Provided HYPR app ID is invalid."),
        HYPR_ENDPOINT_API_TOKEN_INVALID_FAILURE("65011",
                "Provided HYPR endpoint API token is either invalid or expired"),
        SERVER_ERROR_GENERAL("65012", "Server error occurred",
                "Unable to complete the action due to a server error"),
        SERVER_ERROR_INVALID_API_TOKEN("65013", "Invalid API token",
                "The extracted HYPR API token is either expired or invalid."),
        SERVER_ERROR_INVALID_HYPR_URL("65014", "Invalid HYPR Base URL.",
                "Extracted HYPR Base URL doesn't exist."),
        SERVER_ERROR_RETRIEVING_AUTHENTICATION_STATUS("65015",
                "Error while retrieving authentication status",
                "Error occurred while retrieving the authentication status from the HYPR server."),
        SERVER_ERROR_INVALID_AUTHENTICATOR_CONFIGURATIONS("65016",
                "Invalid authenticator configurations",
                "Extracted HYPR authenticator configurations missing either baseUrl or apiToken"),
        SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES("65017",
                "Invalid authenticator configurations",
                "Extracted HYPR authentication properties from the context missing either authStatus or " +
                        "requestId"),
        SERVER_ERROR_CREATING_HTTP_CLIENT("65018", "Error while creating http client.",
                "Server error encountered while creating http client."),
        SERVER_ERROR_GETTING_HTTP_CLIENT("65019", "Error while getting the http client.",
                "Error preparing http client to publish events."),
        CLIENT_ERROR_INVALID_SESSION_KEY("60001", "Invalid session key provided.",
                                                 "The provided session key doesn't exist.");
        private final String code;
        private final String message;
        private final String description;

        /**
         * Create an Error Message.
         *
         * @param code    Relevant error code.
         * @param message Relevant error message.
         */
        ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
            description = null;
        }

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        /**
         * To get the code of specific error.
         *
         * @return Error code.
         */
        public String getCode() {

            return HYPR.HYPR_API_PREFIX + code;
        }

        /**
         * To get the message of specific error.
         *
         * @return Error message.
         */
        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }

        @Override
        public String toString() {

            return code + " | " + message;
        }
    }

    /**
     * Includes the HYPR authentication and registration related constants.
     */
    public static class HYPR {

        public static final String AUTHENTICATOR_NAME = "HYPRAuthenticator";
        public static final String AUTHENTICATOR_FRIENDLY_NAME = "HYPR";
        public static final String SESSION_DATA_KEY = "sessionDataKey";
        public static final String USERNAME = "username";

        public static final String CORRELATION_ID_KEY = "Correlation-ID";
        public static final String HYPR_API_PREFIX = "HYPR-API-";

        // Configurable parameters
        public static final String BASE_URL = "baseUrl";
        public static final String APP_ID = "appId";
        public static final String HYPR_API_TOKEN = "apiToken";

        // HYPR API Parameters
        public static final String MACHINE = "machine";
        public static final String MACHINE_ID = "machineId";
        public static final String REQUEST_ID = "requestId";
        public static final String RESPONSE = "response";
        public static final String NAMED_USER = "namedUser";
        public static final String SESSION_NONCE = "sessionNonce";
        public static final String DEVICE_NONCE = "deviceNonce";
        public static final String SERVICE_NONCE = "serviceNonce";
        public static final String SERVICE_MAC = "serviceHmac";

        // REST API Parameters
        public static final String AUTH_STATUS = "authStatus";
        public static final String AUTH_REQUEST_ID = "authRequestId";
        public static final List<String> TERMINATING_STATUSES = Arrays.asList("COMPLETED", "FAILED", "CANCELED");

        // Authentication API paths
        public static final String HYPR_USER_DEVICE_INFO_PATH = "/rp/api/oob/client/authentication/";
        public static final String HYPR_AUTH_PATH = "/rp/api/oob/client/authentication/requests";
        public static final String HYPR_AUTH_STATUS_CHECK_PATH = "/rp/api/oob/client/authentication/requests/";

        //Page paths
        public static final String HYPR_LOGIN_PAGE = "/authenticationendpoint/hyprlogin.jsp";

        /**
         * Object holding authentication mobile response status.
         */
        public enum AuthenticationStatus {

            INVALID_TOKEN("INVALID_TOKEN", "Authentication failed due to an internal server error. " +
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
