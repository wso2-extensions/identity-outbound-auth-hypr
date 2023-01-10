package org.wso2.carbon.identity.application.authenticator.hypr.common.model;

import java.util.StringJoiner;

/***/
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
