package com.authenticated.server.example.handlers;

import com.authenticated.server.example.dto.AccessToken;
import com.authenticated.server.example.dto.AuthenticateBody;
import com.authenticated.server.example.dto.ErrorResponse;
import com.authenticated.server.example.dto.Jwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * AuthenticationHandler will be run when the /authenticate endpoint is called
 */
public class AuthenticateHandler implements HttpHandler {
    
    public void handle(HttpExchange httpExchange) throws IOException {
        HttpResponse tokenResponse = getAccessTokenFromPureCloud();
        String tokenData;
        BasicResponseHandler basicResponseHandler = new BasicResponseHandler();
    
        try {
            tokenData = basicResponseHandler.handleResponse(tokenResponse);
        } catch (HttpResponseException e) {
            sendResponse(e.getStatusCode(), new ErrorResponse(e.getMessage()), httpExchange);
            return;
        }

        // TODO: Validate client token and fetch data to display to agent
        Headers requestHeaders = httpExchange.getRequestHeaders();
        String clientToken = requestHeaders.getFirst("Client-Token");

        AuthenticateBody authenticateBody = new AuthenticateBody();
        if (clientToken != null && !clientToken.isEmpty()) {
            authenticateBody.setFirstName("John");
            authenticateBody.setLastName("Doe");
        } else {
            sendResponse(400, new ErrorResponse("No client token submitted"), httpExchange);
            return;
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        AccessToken token = objectMapper.readValue(tokenData, AccessToken.class);
        HttpResponse jwtResponse = getJwtFromPureCloud(token, authenticateBody);
        String jwtData;
        try {
            jwtData = basicResponseHandler.handleResponse(jwtResponse);
        } catch (HttpResponseException e) {
            sendResponse(e.getStatusCode(), new ErrorResponse(e.getMessage()), httpExchange);
            return;
        }
        
        Jwt jwt = objectMapper.readValue(jwtData, Jwt.class);
        sendResponse(200, jwt, httpExchange);
    }
    
    private void sendResponse(int statusCode, Object response, HttpExchange httpExchange) throws IOException {
        Gson gson = new Gson();
        String jsonResponse = gson.toJson(response);
        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Content-Type", "application/json");
        headers.add("Connection", "keep-alive");
        httpExchange.sendResponseHeaders(statusCode, jsonResponse.getBytes().length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(jsonResponse.getBytes());
        outputStream.close();
    }
    
    private String getAuthEncoding() {
        //TODO: Insert valid client id and secret for OAuth Application with the authentication -> signature -> create permission
        return Base64.getEncoder().encodeToString(("clientId:secret").getBytes());
    }
    
    /**
     * This method will make a POST request to PureCloud using an {@link AccessToken}
     *
     * @param accessToken - {@link AccessToken} that is returned from PureCloud when doing client credentials OAuth
     * @return - the {@link HttpResponse} from the request to PureCloud
     * @throws IOException - not handling the error here
     */
    private HttpResponse getJwtFromPureCloud(AccessToken accessToken, AuthenticateBody body) throws IOException {
        
        HttpHost httpHost = HttpHost.create("https://api.mypurecloud.com");
        
        Header contentTypeHeader = new BasicHeader("Content-Type", "application/json");
        Header authorizationHeader = new BasicHeader("Authorization", accessToken.getToken_type() + " " + accessToken.getAccess_token());
        
        //Create http client for getting access token with client id and client secret
        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultHeaders(Arrays.asList(contentTypeHeader, authorizationHeader))
                .build();
        
        Gson gson = new Gson();
        String authenticateJson = gson.toJson(body);
        StringEntity httpEntity = new StringEntity(authenticateJson, ContentType.APPLICATION_JSON);
        
        HttpPost httpPost = new HttpPost("/api/v2/signeddata");
        httpPost.setEntity(httpEntity);
        
        return httpClient.execute(httpHost, httpPost);
    }
    
    /**
     * Performs OAuth using client credential grant type
     *
     * @return - the {@link HttpResponse} from the request to PureCloud
     * @throws IOException - not handling the error here
     */
    private HttpResponse getAccessTokenFromPureCloud() throws IOException {
        /*
         * Documentation for getting access token with client credentials can be found here
         * https://developer.mypurecloud.com/api/rest/authorization/use-client-credentials.html
         */
        Header contentTypeHeader = new BasicHeader("Content-Type", "application/x-www-form-urlencoded");
        Header authorizationHeader = new BasicHeader("Authorization", "Basic " + getAuthEncoding());

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultHeaders(Arrays.asList(contentTypeHeader, authorizationHeader))
                .build();
        
        HttpPost httpPost = new HttpPost("/oauth/token");

        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        httpPost.addHeader(authorizationHeader);

        HttpHost httpHost = HttpHost.create("https://login.mypurecloud.com");

        //Make api call and get response body
        return httpClient.execute(httpHost, httpPost);
    }
}
