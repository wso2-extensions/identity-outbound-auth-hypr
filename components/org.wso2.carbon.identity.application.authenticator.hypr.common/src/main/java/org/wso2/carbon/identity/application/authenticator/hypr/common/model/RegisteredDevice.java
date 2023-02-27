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

import java.util.StringJoiner;

/**
 * Model class for the request which inquire the list of registered devices given the username.
 */
public class RegisteredDevice {

    private String deviceId;
    private String protocolVersion;
    private String modelNumber;
    private String machineId;
    private String namedUser;

    public String getDeviceId() {

        return this.deviceId;
    }

    public void setDeviceId(final String deviceId) {

        this.deviceId = deviceId;
    }

    public String getProtocolVersion() {

        return this.protocolVersion;
    }

    public void setProtocolVersion(final String protocolVersion) {

        this.protocolVersion = protocolVersion;
    }

    public String getModelNumber() {

        return this.modelNumber;
    }

    public void setModelNumber(final String modelNumber) {

        this.modelNumber = modelNumber;
    }

    public String getMachineId() {

        return this.machineId;
    }

    public void setMachineId(final String machineId) {

        this.machineId = machineId;
    }

    public String getNamedUser() {

        return this.namedUser;
    }

    public void setNamedUser(final String namedUser) {

        this.namedUser = namedUser;
    }

    @Override
    public String toString() {

        return new StringJoiner(", ", RegisteredDevicesResponse.class.getSimpleName() + "[", "]")
                .add(", protocolVersion=\"" + this.protocolVersion + "\"")
                .add(", modelNumber=\"" + this.modelNumber + "\"")
                .add(", machineId=" + this.machineId)
                .add(", namedUser=" + this.namedUser)
                .toString();
    }
}
