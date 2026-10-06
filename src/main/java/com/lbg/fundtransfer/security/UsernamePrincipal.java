package com.lbg.fundtransfer.security;

import java.security.Principal;

/**
 * Minimal {@link Principal} whose name is the authenticated customer's username.
 * This is what JAX-RS resources read via {@code SecurityContext.getUserPrincipal()}
 * in place of the Spring {@code @AuthenticationPrincipal UserDetails}.
 */
public record UsernamePrincipal(String username) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
