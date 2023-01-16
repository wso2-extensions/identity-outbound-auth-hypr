package org.wso2.carbon.identity.application.authenticator.hypr.common.web;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log LOG = LogFactory.getLog(HTTPClientManager.class);
    private static final int HTTP_CONNECTION_TIMEOUT = 300;
    private static final int HTTP_READ_TIMEOUT = 300;
    private static final int HTTP_CONNECTION_REQUEST_TIMEOUT = 300;
    private static final int DEFAULT_MAX_CONNECTIONS = 20;
    private static HTTPClientManager instance;
    private final CloseableHttpClient httpClient;

    /**
     * Creates a client manager.
     *
     * @throws
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
     * @throws
     */
    public static synchronized HTTPClientManager getInstance() throws HYPRClientException {
        if (instance == null) {
            instance = new HTTPClientManager();
        }
        return instance;
    }

    /**
     * Get HTTP client.
     *
     * @return CloseableHttpClient instance.
     */
    public CloseableHttpClient getClient() throws HYPRClientException {

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

        int maxConnections = DEFAULT_MAX_CONNECTIONS;
        int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS;

        PoolingHttpClientConnectionManager poolingHttpClientConnectionMgr = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 20.
        poolingHttpClientConnectionMgr.setMaxTotal(maxConnections);
        // Increase default max connection per route to 20.
        poolingHttpClientConnectionMgr.setDefaultMaxPerRoute(maxConnectionsPerRoute);
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
