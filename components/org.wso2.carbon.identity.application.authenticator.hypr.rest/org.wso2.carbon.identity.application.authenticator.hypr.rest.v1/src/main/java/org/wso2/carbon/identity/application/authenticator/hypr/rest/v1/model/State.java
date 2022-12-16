package org.wso2.carbon.identity.application.authenticator.hypr.rest.v1.model;

/***/
public class State {

    private final String value;
    private final String message;

    public State(String value, String message) {
        this.value = value;
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }
}
