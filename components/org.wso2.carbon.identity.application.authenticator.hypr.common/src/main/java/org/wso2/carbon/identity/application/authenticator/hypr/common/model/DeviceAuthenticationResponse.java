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
 * Model class for the response received upon successfully sending a push notification
 * from HYPR platform.
 */
public class DeviceAuthenticationResponse {

    private ResponseEntity status;
    private RequestIDResponse response;

    public ResponseEntity getStatus() {
        return this.status;
    }

    public void setStatus(final ResponseEntity status) {
        this.status = status;
    }

    public RequestIDResponse getResponse() {
        return this.response;
    }

    public void setResponse(final RequestIDResponse response) {
        this.response = response;
    }
}
