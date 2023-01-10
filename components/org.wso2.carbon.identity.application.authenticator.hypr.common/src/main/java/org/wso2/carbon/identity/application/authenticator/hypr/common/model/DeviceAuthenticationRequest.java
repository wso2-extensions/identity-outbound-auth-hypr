package org.wso2.carbon.identity.application.authenticator.hypr.common.model;

import org.wso2.carbon.identity.application.authenticator.hypr.common.web.HYPRWebUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

/***/ //TODO:
public class DeviceAuthenticationRequest {

    private final String namedUser;
    private final String machine;
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
        this.machine = "WEB";
        this.sessionNonce = HYPRWebUtils.doSha256(String.valueOf(HYPRWebUtils.generateRandomPIN()));
        this.deviceNonce = HYPRWebUtils.doSha256(String.valueOf(HYPRWebUtils.generateRandomPIN()));
        this.serviceNonce = HYPRWebUtils.doSha256(String.valueOf(HYPRWebUtils.generateRandomPIN()));
        this.serviceHmac = HYPRWebUtils.doSha256(String.valueOf(HYPRWebUtils.generateRandomPIN()));
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
