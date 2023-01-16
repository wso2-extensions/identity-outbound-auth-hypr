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

package org.wso2.carbon.identity.application.authenticator.hypr.common.exception;

/**
 * An exception class which is used to send a HYPR specific error code and error message when the HYPR connector
 * encountered any errors with regard to HTTP Client connections.
 */
public class HYPRClientException extends Exception {

    private String code;

    private String description;

    /**
     * An overloaded constructor which is used to throw an error code and error message once the HYPR connector
     * unable to proceed the authentication with HYPR due to HTTP client connection issue.
     *
     * @param code    An error code specified to the authenticator.
     * @param message An error message specified to the authenticator.
     */
    public HYPRClientException(String message, String code) {

        super(message);
        this.code = code;
    }

    /**
     * An overloaded constructor which is used to throw an error code, error message and error description once the
     * HYPR connector unable to proceed the authentication with HYPR due to HTTP client connection issue.
     *
     * @param code    An error code specified to the authenticator.
     * @param message An error message specified to the authenticator.
     * @param description An in-detail error description specified to the authenticator.
     */
    public HYPRClientException(String message, String description, String code) {

        super(message);
        this.description = description;
        this.code = code;
    }

    /**
     * An overloaded constructor which is used to throw an error code, error message, error description and
     * throwable cause once the HYPR connector unable to proceed the authentication with HYPR due to
     * HTTP client connection issue.
     *
     * @param code    An error code specified to the authenticator.
     * @param message An error message specified to the authenticator.
     * @param description An in-detail error description specified to the authenticator.
     * @param cause Thrown exception.
     */
    public HYPRClientException(String message, String description, String code, Throwable cause) {

        super(message, cause);
        this.description = description;
        this.code = code;
    }
}
