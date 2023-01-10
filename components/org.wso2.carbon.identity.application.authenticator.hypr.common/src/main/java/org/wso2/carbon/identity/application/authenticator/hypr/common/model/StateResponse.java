package org.wso2.carbon.identity.application.authenticator.hypr.common.model;

import java.util.List;

/***///TODO:  Add class descriptions
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
