package org.wso2.carbon.identity.application.authenticator.hypr.common.exception;

/***/
public class HYPRClientException extends Exception {

    private String errorCode;

    private String description;

    public HYPRClientException(String message, String errorCode) {

        super(message);
        this.errorCode = errorCode;
    }

    public HYPRClientException(String message, String description, String errorCode) {

        super(message);
        this.description = description;
        this.errorCode = errorCode;
    }

    public HYPRClientException(String message, String description, String errorCode, Throwable cause) {

        super(message, cause);
        this.description = description;
        this.errorCode = errorCode;
    }
}
