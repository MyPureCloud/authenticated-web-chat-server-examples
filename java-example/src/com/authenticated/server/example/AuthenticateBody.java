package com.authenticated.server.example;

public class AuthenticateBody {

    boolean authorized = true;

    public boolean isAuthorized() {
            return authorized;
        }

    public void setAuthorized(boolean authorized) {
            this.authorized = authorized;
        }
}
