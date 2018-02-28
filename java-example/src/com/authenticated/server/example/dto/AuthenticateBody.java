package com.authenticated.server.example.dto;

public class AuthenticateBody {

    private boolean authorized = true;

    public boolean isAuthorized() {
            return authorized;
        }
    
    public void setAuthorized(boolean authorized) {
            this.authorized = authorized;
        }
}
