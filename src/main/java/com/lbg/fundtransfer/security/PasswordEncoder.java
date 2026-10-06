package com.lbg.fundtransfer.security;

/**
 * Password hashing abstraction (replaces Spring Security's
 * {@code PasswordEncoder} bean). Implemented by {@link BcryptPasswordEncoder}.
 */
public interface PasswordEncoder {

    /** Hashes a raw password for storage. */
    String encode(String rawPassword);

    /** Verifies a raw password against a previously stored hash. */
    boolean matches(String rawPassword, String encodedPassword);
}
