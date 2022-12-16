package org.wso2.carbon.identity.application.authenticator.hypr.model;


import java.util.List;

/***/
public class RegisteredDevicesResponse {

    private final List<RegisteredDevice> registeredDevices;

    public RegisteredDevicesResponse(List<RegisteredDevice> registeredDevices) {
        this.registeredDevices = registeredDevices;
    }

    public List<RegisteredDevice> getRegisteredDevices() {
        return registeredDevices;
    }
}
