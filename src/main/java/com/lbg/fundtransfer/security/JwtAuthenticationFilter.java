package com.lbg.fundtransfer.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.security.Principal;
import java.util.List;

/**
 * Reads a Bearer token from the Authorization header, validates it, and installs
 * a {@link SecurityContext} carrying the customer's username as the principal.
 *
 * <p>This single filter does the job of the Spring Boot original's
 * {@code JwtAuthenticationFilter} <em>and</em> {@code SecurityConfig}: it also
 * enforces the "everything authenticated except the public endpoints" rule.
 * Public prefixes ({@code /api/auth}, {@code /internal}, Quarkus' own {@code /q})
 * are left open — the internal endpoint authorises itself with a shared service
 * key. Any other path without a valid token is rejected with a bare 401, matching
 * the original stateless {@code HttpStatusEntryPoint(UNAUTHORIZED)} behaviour.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PREFIXES = List.of("/api/auth", "/internal", "/q");

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = normalize(requestContext.getUriInfo().getPath());
        Principal principal = authenticate(requestContext);

        if (principal != null) {
            boolean secure = "https".equalsIgnoreCase(requestContext.getUriInfo().getRequestUri().getScheme());
            requestContext.setSecurityContext(new JwtSecurityContext(principal, secure));
            return;
        }

        if (!isPublic(path)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private Principal authenticate(ContainerRequestContext requestContext) {
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            String username = jwtService.extractUsername(token);
            if (username != null && jwtService.isValid(token, username)) {
                return new UsernamePrincipal(username);
            }
        } catch (Exception ex) {
            log.debugf("Invalid JWT rejected: %s", ex.getMessage());
        }
        return null;
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Returns the request path with exactly one leading slash. Quarkus REST's
     * {@code UriInfo.getPath()} returns the path <em>with</em> a leading slash
     * (e.g. {@code /api/auth/login}); other JAX-RS implementations return it
     * without one. Normalising here means the public-prefix check works the same
     * either way — the earlier {@code "/" + getPath()} produced {@code //api/auth/login},
     * which matched no prefix, so every request (login included) was rejected 401.
     */
    private static String normalize(String rawPath) {
        String path = rawPath == null ? "" : rawPath;
        return path.startsWith("/") ? path : "/" + path;
    }

    /** Request-scoped security context backed by the validated token's principal. */
    private record JwtSecurityContext(Principal principal, boolean secure) implements SecurityContext {

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return "Bearer";
        }
    }
}
