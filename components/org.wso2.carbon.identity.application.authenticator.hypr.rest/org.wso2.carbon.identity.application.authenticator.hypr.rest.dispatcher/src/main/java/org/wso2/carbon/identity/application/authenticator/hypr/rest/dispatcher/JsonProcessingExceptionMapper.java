/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
 * AN exception mapper class that handles exceptions when an incorrect json requests body is received.
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
