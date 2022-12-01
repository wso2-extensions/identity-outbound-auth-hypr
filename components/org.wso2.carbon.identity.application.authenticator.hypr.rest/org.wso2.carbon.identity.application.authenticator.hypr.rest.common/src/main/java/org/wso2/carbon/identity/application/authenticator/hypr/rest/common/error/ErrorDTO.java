/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 LLC. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 *
 */

package org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.org.codehaus.commons.nullanalysis.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Common DTO for all the API related error responses.
 */
@ApiModel(description = "")
public class ErrorDTO implements Serializable {

    private static final long serialVersionUID = 1984062651956875663L;

    @NotNull
    private String code = null;

    @NotNull
    private String message = null;

    private String description = null;

    private String ref = null;

    /**
     * Returns the error code.
     *
     * @return Error code.
     */
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("code")
    public String getCode() {

        return code;
    }

    public void setCode(String code) {

        this.code = code;
    }

    /**
     * Returns the error message.
     *
     * @return Error message.
     */
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("message")
    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {

        this.message = message;
    }

    /**
     * Returns the error description.
     *
     * @return Error description.
     */
    @ApiModelProperty(value = "")
    @JsonProperty("description")
    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    /**
     * Returns the traceId of the error.
     *
     * @return TraceId of the error.
     */
    @ApiModelProperty(value = "")
    @JsonProperty("traceId")
    public String getRef() {

        return ref;
    }

    public void setRef(String ref) {

        this.ref = ref;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorDTO {\n");
        sb.append("  code: ").append(code).append("\n");
        sb.append("  message: ").append(message).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  traceId: ").append(ref).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
