package com.authenticated.server.example.dto;

/**
 * Created by matt.harter on 2/28/18.
 */
public class ErrorResponse {
    
    String message;
    public ErrorResponse(String errorMessage) {
        message = errorMessage;
    }
}
