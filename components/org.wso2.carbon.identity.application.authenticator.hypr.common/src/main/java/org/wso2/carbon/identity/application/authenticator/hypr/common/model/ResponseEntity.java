package org.wso2.carbon.identity.application.authenticator.hypr.common.model;

/***/
public class ResponseEntity {

    private Integer responseCode;
    private String responseMessage;

    public ResponseEntity() {
        this.responseCode = 500;
    }

    public final Integer getResponseCode() {
        return this.responseCode;
    }

    public final void setResponseCode(final Integer code) {
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
