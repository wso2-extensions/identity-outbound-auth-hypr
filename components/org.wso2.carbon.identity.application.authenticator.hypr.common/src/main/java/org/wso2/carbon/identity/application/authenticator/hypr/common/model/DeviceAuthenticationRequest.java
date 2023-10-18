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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRWebUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Model class for the request which initiates the device authentication.
 */
public class DeviceAuthenticationRequest {

    private final String namedUser;
    @SuppressWarnings("SS_SHOULD_BE_STATIC")
    private final String machine = HyprAuthenticatorConstants.HYPR.MACHINE_VALUE;
    private final String machineId;
    private final String appId;
    private final String sessionNonce;
    private final String deviceNonce;
    private final String serviceNonce;
    private final String serviceHmac;
    private Map additionalDetails;

    public DeviceAuthenticationRequest(String namedUser, String machineId, String appId)
            throws NoSuchAlgorithmException {

        this.namedUser = namedUser;
        this.machineId = machineId;
        this.appId = appId;
        this.sessionNonce = HYPRWebUtils.getRandomPinSha256();
        this.deviceNonce = HYPRWebUtils.getRandomPinSha256();
        this.serviceNonce = HYPRWebUtils.getRandomPinSha256();
        this.serviceHmac = HYPRWebUtils.getRandomPinSha256();
    }

    public String getNamedUser() {

        return namedUser;
    }

    public String getMachine() {

        return machine;
    }

    public String getMachineId() {

        return machineId;
    }

    public String getAppId() {

        return appId;
    }

    public String getSessionNonce() {

        return sessionNonce;
    }

    public String getDeviceNonce() {

        return deviceNonce;
    }

    public String getServiceNonce() {

        return serviceNonce;
    }

    public String getServiceHmac() {

        return serviceHmac;
    }

    public Map getAdditionalDetails() {

        return additionalDetails;
    }

    public void setAdditionalDetails(Map additionalDetails) {

        this.additionalDetails = additionalDetails;
    }
}
