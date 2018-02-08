package com.authenticated.server.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.api.SignedDataApi;
import com.mypurecloud.sdk.v2.api.request.PostSigneddataRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Server {

    public static void main(String[] args) throws Exception {

        //start http server on port 8000
        HttpsServer server = createServer();
        server.start();
        server.start();
    }

    /**
     * AuthenticationHandler will be run when the /authenticate endpoint is called
     */
    private static class AuthenticationHandler implements HttpHandler {

        public void handle(HttpExchange httpExchange) throws IOException {
            /*
             * Documentation for getting access token with client credentials can be found here
             * https://developer.mypurecloud.com/api/rest/authorization/use-client-credentials.html
             */
            Header contentTypeHeader = new BasicHeader("Content-Type", "application/x-www-form-urlencoded");
            Header authorizationHeader = new BasicHeader("Authorization", "Basic " + getAuthEncoding());

            //Create http client for getting access token with client id and client secret
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
            String response = new BasicResponseHandler().handleResponse(httpClient.execute(httpHost, httpPost));

            //Deserialize response body into a Data Transfer Object
            AccessToken token = new ObjectMapper().readValue(response, AccessToken.class);

            //Create ApiClient (from PureCloud public sdk) using access token
            ApiClient client = ApiClient.Builder.standard()
                    .withAccessToken(token.getAccess_token())
                    .withBasePath("https://api.mypurecloud.com")
                    .build();

            String jwt;
            try {
                jwt = new SignedDataApi(client).postSigneddata(new PostSigneddataRequest().withBody(new AuthenticateBody())).getJwt();
            } catch (IOException | ApiException e) {
                sendResponse(500, "There was an error with PureCloud while authenticating the chat \n" + e.getMessage(), httpExchange);
                return;
            }

            sendResponse(200, jwt, httpExchange);
        }

        private void sendResponse(int statusCode, String response, HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private String getAuthEncoding() {

            //TODO: Insert valid client id and secret for OAuth Application with the authentication -> signature -> create permission
            return Base64.getEncoder().encodeToString(("client id" + ":" + "client secret").getBytes());
        }

    }


    private static HttpsServer createServer() {
        int port = 8000;
        try {
            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);

            // initialise the keystore
            char[] password = "password".toCharArray();
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //TODO: make sure to have a signed cert for your key store
            FileInputStream fileInputStream = new FileInputStream("testkey.jks");
            keyStore.load(fileInputStream, password);

            // setup the key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, password);

            // setup the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            // setup the HTTPS context and parameters
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext context = SSLContext.getDefault();
                        SSLEngine engine = context.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // get the default parameters
                        SSLParameters defaultSSLParameters = context.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);

                    } catch (Exception ex) {
                        System.out.println("Failed to create HTTPS port");
                    }
                }
            });

            //create authentication endpoint
            httpsServer.createContext("/authenticate", new AuthenticationHandler());
            httpsServer.setExecutor(null); // creates a default executor
            return httpsServer;

        } catch (Exception exception) {
            throw new RuntimeException("Failed to create HTTPS server on port " + port + " of localhost because of \n"
                    + exception.getLocalizedMessage());
        }
    }


}
