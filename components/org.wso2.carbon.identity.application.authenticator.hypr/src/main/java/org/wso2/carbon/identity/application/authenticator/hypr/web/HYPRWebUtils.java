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

package org.wso2.carbon.identity.application.authenticator.hypr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
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
import org.apache.http.util.EntityUtils;

import org.wso2.carbon.identity.application.authenticator.hypr.HyprAuthenticatorConstants;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;


/**
 * The HYPRWebUtils class contains all the general helper functions required by the HYPR Authenticator.
 * **/
public class HYPRWebUtils {

    /**
     * Send an HTTP Get request.
     *
     * @param hyprConfiguration     A hashmap composed of HYPR configurations.
     * @param requestURL            The URL to which the GET request should be sent.
     * @return httpResponse         The response received from the HTTP call.
     * @throw IOException
     */
    public static HttpResponse jsonGet(final Map<String, String> hyprConfiguration, final String requestURL)
            throws IOException {

        HttpGet request = new HttpGet(requestURL);
        request.addHeader("Authorization",
                "Bearer " + hyprConfiguration.get(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN));

        HttpResponse httpResponse;

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            httpResponse = toHttpResponse(response);
        }

        return httpResponse;

    }

    /**
     * Send an HTTP POST request.
     *
     * @param hyprConfiguration     A hashmap composed of HYPR configurations.
     * @param requestURL            The URL to which the POST request should be sent.
     * @param requestBody           A hashmap that includes the parameters to be sent through the request.
     * @return httpResponse         The response received from the HTTP call.
     * @throw IOException
     */
    public static HttpResponse jsonPost(final Map<String, String> hyprConfiguration, final String requestURL,
                                        final String requestBody)throws IOException {

        HttpPost request = new HttpPost(requestURL);
        request.addHeader("Authorization",
                "Bearer " + hyprConfiguration.get(HyprAuthenticatorConstants.HYPR.HYPR_API_TOKEN));
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

        HttpResponse httpResponse;

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            httpResponse = toHttpResponse(response);
        }

        return httpResponse;

    }

    private static HttpResponse toHttpResponse (final CloseableHttpResponse response) throws IOException {
        final HttpResponse result = new BasicHttpResponse(response.getStatusLine());
        if (response.getEntity() != null) {
            result.setEntity(new BufferedHttpEntity(response.getEntity()));
        }
        return result;
    }

    /**
     * Convert the HTTPResponse to a json node.
     *
     * @param response A HTTPResponse object received from API call.
     * @throw IOException
     */
    public static JsonNode toJsonNode(HttpResponse response) throws IOException {

        JsonNode rootNode = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            // Convert to a string
            String result = EntityUtils.toString(entity);
            rootNode = new ObjectMapper().readTree(new StringReader(result));
        }
        return rootNode;
    }

    /**
     * Generate  a random pin.
     *
     * @return              A randomly generated pin.
     */
    public static int generateRandomPIN() {
        return 100000 + new Random().nextInt(900000);
    }

    /**
     * Generate the hashcode.
     *
     * @param stringToHash  The string on which the hash needs to be generated.
     * @return hashCode     The hash code  of the provided text.
     * @throws  NoSuchAlgorithmException
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
