package com.lbg.fundtransfer.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds) {

    public static LoginResponse bearer(String token, long expiresInSeconds) {
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}
