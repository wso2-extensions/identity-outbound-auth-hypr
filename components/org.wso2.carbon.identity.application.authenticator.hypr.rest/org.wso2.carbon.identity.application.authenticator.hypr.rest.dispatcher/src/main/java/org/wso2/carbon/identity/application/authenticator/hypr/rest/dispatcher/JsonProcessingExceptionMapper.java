/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 LLC. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 *
 */

package org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.ErrorDTO;
import org.wso2.carbon.identity.application.authenticator.hypr.rest.common.error.ErrorResponse;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.ErrorConstants.ERROR_CODE;
import static org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.ErrorConstants.ERROR_DESCRIPTION;
import static org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.ErrorConstants.ERROR_MESSAGE;

/**
 * Handles exceptions when an incorrect json requests body is received.
 * Sends a default error response.
 */
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Log LOG = LogFactory.getLog(JsonProcessingExceptionMapper.class);

    @Override
    public Response toResponse(JsonProcessingException e) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Provided JSON request content is not in the valid format:", e);
        }
        ErrorDTO errorDTO = new ErrorResponse.Builder().withCode(ERROR_CODE)
                .withMessage(ERROR_MESSAGE)
                .withDescription(ERROR_DESCRIPTION).build();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDTO)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }
}
