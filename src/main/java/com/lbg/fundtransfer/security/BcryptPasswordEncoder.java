package com.lbg.fundtransfer.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * BCrypt implementation of {@link PasswordEncoder}, using strength 10 to match
 * the default of the original Spring Security {@code BCryptPasswordEncoder}.
 */
@ApplicationScoped
public class BcryptPasswordEncoder implements PasswordEncoder {

    private static final int COST = 10;

    @Override
    public String encode(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword).verified;
    }
}
