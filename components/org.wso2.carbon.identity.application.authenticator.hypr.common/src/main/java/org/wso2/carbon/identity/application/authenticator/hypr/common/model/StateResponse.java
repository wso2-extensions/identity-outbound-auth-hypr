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

import java.util.List;

/**
 * Model class for the response received upon inquiring the authentication status.
 */
public class StateResponse {

    private final String requestId;
    private final String namedUser;
    private final List<State> state;

    public StateResponse(String requestId, String namedUser, List<State> state) {

        this.requestId = requestId;
        this.namedUser = namedUser;
        this.state = state;
    }

    public String getRequestId() {

        return requestId;
    }

    public String getNamedUser() {

        return namedUser;
    }

    public List<State> getState() {

        return state;
    }

    public String getCurrentState() {

        return String.valueOf(state.get(state.size() - 1).getValue());
    }
}
