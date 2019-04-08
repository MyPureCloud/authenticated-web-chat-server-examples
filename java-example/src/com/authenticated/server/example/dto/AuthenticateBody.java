package com.authenticated.server.example.dto;

public class AuthenticateBody {

    private boolean authorized = true;

    private String firstName;

    private String lastName;

    public boolean isAuthorized() {
        return authorized;
    }
    
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
