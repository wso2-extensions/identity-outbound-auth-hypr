package org.wso2.carbon.identity.application.authenticator.hypr.common.model;

/***/
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
