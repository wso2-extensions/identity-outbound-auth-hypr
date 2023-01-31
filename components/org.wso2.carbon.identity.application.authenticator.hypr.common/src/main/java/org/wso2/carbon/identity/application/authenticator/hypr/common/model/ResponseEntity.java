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

package org.wso2.carbon.identity.application.authenticator.hypr.common.model;

/**
 * Model class for the sub response received upon successfully sending a push notification from HYPR platform.
 */
public class ResponseEntity {

    private int responseCode;
    private String responseMessage;

    public ResponseEntity() {

        this.responseCode = 500;
    }

    public final int getResponseCode() {

        return this.responseCode;
    }

    public final void setResponseCode(final int code) {

        this.responseCode = code;
    }

    public final String getResponseMessage() {

        return this.responseMessage;
    }

    public final void setResponseMessage(final String message) {

        this.responseMessage = message;
    }

    @Override
    public String toString() {

        return "ResponseEntity(responseCode=" + this.responseCode + ", responseMessage=" + this.responseMessage + ')';
    }
}
