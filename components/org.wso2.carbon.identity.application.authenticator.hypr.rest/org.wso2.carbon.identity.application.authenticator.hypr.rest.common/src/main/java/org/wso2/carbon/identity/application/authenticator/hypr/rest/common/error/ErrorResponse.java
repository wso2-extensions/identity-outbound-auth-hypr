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

import org.apache.commons.logging.Log;

import static org.wso2.carbon.identity.application.authenticator.hypr.rest.common.Util.getCorrelation;
import static org.wso2.carbon.identity.application.authenticator.hypr.rest.common.Util.isCorrelationIDPresent;


/**
 * ErrorResponse class for all API related errors.
 */
public class ErrorResponse extends ErrorDTO {

    private static final long serialVersionUID = -3502358623560083025L;

    /**
     * ErrorResponse Builder.
     */
    public static class Builder {

        private String code;
        private String message;
        private String description;

        /**
         * Return current instance with code attribute set.
         *
         * @param code Error code.
         * @return Current builder instance.
         */
        public Builder withCode(String code) {

            this.code = code;
            return this;
        }

        /**
         * Return current instance with message attribute set.
         *
         * @param message Error message.
         * @return Current builder instance.
         */
        public Builder withMessage(String message) {

            this.message = message;
            return this;
        }

        /**
         * Return current instance with description attribute set.
         *
         * @param description Error description.
         * @return Current builder instance.
         */
        public Builder withDescription(String description) {

            this.description = description;
            return this;
        }

        /**
         * Build error response object.
         *
         * @return Error response.
         */
        public ErrorResponse build() {

            ErrorResponse error = new ErrorResponse();
            error.setCode(this.code);
            error.setMessage(this.message);
            error.setDescription(this.description);
            error.setRef(getCorrelation());
            return error;
        }

        /**
         * Build error response object from log and exception.
         *
         * @param log               Log object.
         * @param e                 Exception occurred.
         * @param message           Error message without code.
         * @param isClientException Flag to mark client,server exceptions.
         * @return Error response.
         */
        public ErrorResponse build(Log log, Exception e, String message, boolean isClientException) {

            ErrorResponse error = build();
            String errorMessageFormat = "errorCode: %s | message: %s";
            String errorMsg = String.format(errorMessageFormat, error.getCode(), message);
            if (!isCorrelationIDPresent()) {
                errorMsg = String.format("correlationID: %s | " + errorMsg, error.getRef());
            }
            if (isClientException) {
                if (log.isDebugEnabled()) {
                    log.debug(errorMsg, e);
                }
            } else {
                log.error(errorMsg, e);
            }
            return error;
        }

        /**
         * Build error response object from log.
         *
         * @param log               Log object.
         * @param message           Error message without code.
         * @param isClientException Flag to mark client,server exceptions.
         * @return Error response.
         */
        public ErrorResponse build(Log log, String message, boolean isClientException) {

            ErrorResponse error = build();
            String errorMessageFormat = "errorCode: %s | message: %s";
            String errorMsg = String.format(errorMessageFormat, error.getCode(), message);
            if (!isCorrelationIDPresent()) {
                errorMsg = String.format("correlationID: %s | " + errorMsg, error.getRef());
            }
            if (isClientException && log.isDebugEnabled()) {
                log.debug(errorMsg);
            } else {
                log.error(errorMsg);
            }
            return error;
        }
    }
}
