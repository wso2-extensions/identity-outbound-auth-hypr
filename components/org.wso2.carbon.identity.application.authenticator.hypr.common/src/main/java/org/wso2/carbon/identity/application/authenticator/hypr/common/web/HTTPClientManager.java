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

package org.wso2.carbon.identity.application.authenticator.hypr.common.web;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.wso2.carbon.identity.application.authenticator.hypr.common.constants.HyprAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.hypr.common.exception.HYPRClientException;

import java.io.IOException;
import static java.util.Objects.isNull;

/**
 * Class to retrieve the HTTP Clients.
 */
public class HTTPClientManager {

    private static final int HTTP_CONNECTION_TIMEOUT = 3000;
    private static final int HTTP_READ_TIMEOUT = 3000;
    private static final int HTTP_CONNECTION_REQUEST_TIMEOUT = 3000;
    private static final int DEFAULT_MAX_CONNECTIONS = 20;
    private static volatile HTTPClientManager httpClientManagerInstance;
    private final CloseableHttpClient httpClient;

    /**
     * Creates a client manager.
     *
     * @throws HYPRClientException Exception thrown when an error occurred when creating HTTP client.
     */
    private HTTPClientManager() throws HYPRClientException {

        PoolingHttpClientConnectionManager connectionManager;
        try {
            connectionManager = createPoolingConnectionManager();
        } catch (IOException e) {
            throw handleServerException(HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_CREATING_HTTP_CLIENT, e);
        }

        RequestConfig config = createRequestConfig();
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setConnectionManager(connectionManager).build();
    }

    /**
     * Returns an instance of the HTTPClientManager.
     *
     * @throws HYPRClientException Exception thrown when an error occurred when creating HTTP client.
     */
    public static HTTPClientManager getInstance() throws HYPRClientException {

        if (httpClientManagerInstance == null) {
            synchronized (HTTPClientManager.class) {
                if (httpClientManagerInstance == null) {
                    httpClientManagerInstance = new HTTPClientManager();
                }
            }
        }
        return httpClientManagerInstance;
    }

    /**
     * Get HTTP client.
     *
     * @return CloseableHttpClient instance.
     * @throws HYPRClientException Exception thrown when an error occurred when getting HTTP client.
     */
    public CloseableHttpClient getHttpClient() throws HYPRClientException {

        if (isNull(httpClient)) {
            throw handleServerException(
                    HyprAuthenticatorConstants.ErrorMessages.SERVER_ERROR_GETTING_HTTP_CLIENT, null);
        }
        return httpClient;
    }

    private RequestConfig createRequestConfig() {

        return RequestConfig.custom()
                .setConnectTimeout(HTTP_CONNECTION_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_CONNECTION_REQUEST_TIMEOUT)
                .setSocketTimeout(HTTP_READ_TIMEOUT)
                .setRedirectsEnabled(false)
                .setRelativeRedirectsAllowed(false)
                .build();
    }

    private PoolingHttpClientConnectionManager createPoolingConnectionManager() throws IOException {

        PoolingHttpClientConnectionManager poolingHttpClientConnectionMgr = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 20.
        poolingHttpClientConnectionMgr.setMaxTotal(DEFAULT_MAX_CONNECTIONS);
        // Increase default max connection per route to 20.
        poolingHttpClientConnectionMgr.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS);
        return poolingHttpClientConnectionMgr;
    }

    private static HYPRClientException handleServerException(
            HyprAuthenticatorConstants.ErrorMessages error, Throwable throwable, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new HYPRClientException(error.getMessage(), description, error.getCode(), throwable);
    }

}
