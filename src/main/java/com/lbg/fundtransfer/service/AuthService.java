package com.lbg.fundtransfer.service;

import com.lbg.fundtransfer.dto.LoginRequest;
import com.lbg.fundtransfer.dto.LoginResponse;

/**
 * Authenticates a customer and issues a JWT on success.
 * Implemented by {@code com.lbg.fundtransfer.service.impl.AuthServiceImpl}.
 */
public interface AuthService {

    /**
     * Verifies the credentials and returns a signed token.
     *
     * @param request the username/password to authenticate
     * @return a bearer token and its lifetime
     * @throws com.lbg.fundtransfer.exception.InvalidCredentialsException if authentication fails
     */
    LoginResponse login(LoginRequest request);
}
