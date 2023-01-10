/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * The HYPRWebUtils class contains all the general helper functions required by the HYPR Authenticator.
 */
public class HYPRWebUtils {

    /**
     * Send an HTTP Get request.
     *
     * @param apiToken   API token provided by HYPR.
     * @param requestURL The URL to which the GET request should be sent.
     * @return httpResponse         The response received from the HTTP call.
     * @throws IOException Exception thrown when an error occurred during converting the HTTPResponse to a jsonNode.
     */
    public static HttpResponse httpGet(String apiToken, String requestURL) throws IOException {

        HttpGet request = new HttpGet(requestURL);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            return toHttpResponse(response);
        }
    }

    /**
     * Send an HTTP POST request.
     *
     * @param apiToken    API token provided by HYPR.
     * @param requestURL  The URL to which the POST request should be sent.
     * @param requestBody A hashmap that includes the parameters to be sent through the request.
     * @return httpResponse         The response received from the HTTP call.
     * @throws IOException Exception thrown when an error occurred during converting the HTTPResponse to a jsonNode.
     */
    public static HttpResponse httpPost(String apiToken, String requestURL, String requestBody) throws IOException {
// TODO : Have a pool of connections . check
        HttpPost request = new HttpPost(requestURL);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            return toHttpResponse(response);
        }
    }

    private static HttpResponse toHttpResponse(final CloseableHttpResponse response) throws IOException {

        final HttpResponse result = new BasicHttpResponse(response.getStatusLine());
        if (response.getEntity() != null) {
            result.setEntity(new BufferedHttpEntity(response.getEntity()));
        }
        return result;
    }

    /**
     * Generate  a random pin.
     *
     * @return A randomly generated pin.
     */
    public static int generateRandomPIN() {

        return 100000 + new Random().nextInt(900000);
    }

    /**
     * Generate the hashcode.
     *
     * @param stringToHash The string on which the hash needs to be generated.
     * @return hashCode     The hash code  of the provided text.
     * @throws NoSuchAlgorithmException Exception thrown when an error occurred during getting the SHA-256 algorithm.
     */
    public static String doSha256(final String stringToHash) throws NoSuchAlgorithmException {

        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(stringToHash.getBytes());
        final byte[] bytes = md.digest();
        final StringBuilder hexString = new StringBuilder();
        for (final byte aByte : bytes) {
            final String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
