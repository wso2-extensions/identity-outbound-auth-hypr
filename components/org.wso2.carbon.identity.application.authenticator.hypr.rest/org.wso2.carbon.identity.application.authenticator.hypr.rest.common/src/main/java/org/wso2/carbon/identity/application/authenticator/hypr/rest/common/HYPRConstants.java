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

package org.wso2.carbon.identity.application.authenticator.hypr.rest.common;

/**
 * Includes all the constants variables used by the HYPR REST API.
 */
public class HYPRConstants {

    public static final String CORRELATION_ID_KEY = "Correlation-ID";

    public static final String IDP_NAME = "HYPR";
    public static final String TENANT_DOMAIN = "carbon.super";
    public static final String BASE_URL = "baseUrl";
    public static final String API_TOKEN = "apiToken";

    public static final String HYPR_API_PREFIX = "HYPR-API-";
    public static final String AUTH_STATUS = "authStatus";
    public static final String AUTH_REQUEST_ID = "authRequestId";
    public static final String[] TERMINATING_STATUSES = {"COMPLETED", "FAILED", "CANCELED"};

    // Authentication API paths
    public static final String HYPR_AUTH_STATUS_CHECK_PATH = "/rp/api/oob/client/authentication/requests/";

    /**
     * Enum for hypr rest api related errors in the following format.
     * Error Code - code to identify the error
     * Error Message - What went wrong
     * Error Description - Why it went wrong
     */
    public enum ErrorMessage {

        CLIENT_ERROR_INVALID_SESSION_KEY("60001",
                "Invalid session key provided.",
                "The provided session key doesn't exist."),
        SERVER_ERROR_GENERAL("65001",
                "Server error occurred",
                "Unable to complete the action due to a server error"),
        SERVER_ERROR_INVALID_API_TOKEN("65002",
                "Invalid API token",
                "The extracted HYPR API token is either expired or invalid."),
        SERVER_ERROR_INVALID_HYPR_URL("65003",
                "Invalid HYPR Base URL.",
                "Extracted HYPR Base URL doesn't exist."),
        SERVER_ERROR_RETRIEVING_AUTHENTICATION_STATUS("65004",
                "Error while retrieving authentication status",
                "Error occurred while retrieving the authentication status from the HYPR server."),
        SERVER_ERROR_INVALID_AUTHENTICATOR_CONFIGURATIONS("65005",
                "Invalid authenticator configurations",
                "Extracted HYPR authenticator configurations missing either baseUrl or apiToken"),
        SERVER_ERROR_INVALID_AUTHENTICATION_PROPERTIES("65006",
                "Invalid authenticator configurations",
                "Extracted HYPR authentication properties from the context missing either authStatus or " +
                        "requestId");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {
            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {
            return HYPR_API_PREFIX + code;
        }

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
}
