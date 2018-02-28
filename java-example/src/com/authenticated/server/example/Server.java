package com.authenticated.server.example;

import com.authenticated.server.example.handlers.AuthenticateHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class Server {

    public static void main(String[] args) throws Exception {

        //start https server on port 8000
        HttpsServer server = createServer();
        server.start();
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
            httpsServer.createContext("/authenticate", new AuthenticateHandler());
            httpsServer.setExecutor(null); // creates a default executor
            return httpsServer;

        } catch (Exception exception) {
            throw new RuntimeException("Failed to create HTTPS server on port " + port + " of localhost because of \n"
                    + exception.getLocalizedMessage());
        }
    }
    
}
