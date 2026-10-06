package com.lbg.fundtransfer.service.impl;

import com.lbg.fundtransfer.domain.Customer;
import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.LoginRequest;
import com.lbg.fundtransfer.dto.LoginResponse;
import com.lbg.fundtransfer.exception.InvalidCredentialsException;
import com.lbg.fundtransfer.repository.CustomerRepository;
import com.lbg.fundtransfer.security.JwtService;
import com.lbg.fundtransfer.security.PasswordEncoder;
import com.lbg.fundtransfer.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Authenticates a customer and mints a JWT on success.
 *
 * <p>In the Spring Boot original this delegated to the
 * {@code AuthenticationManager} (which used a {@code UserDetailsService} and the
 * {@code PasswordEncoder}). Quarkus has no such manager for this custom,
 * stateless scheme, so the same check is performed directly: look the customer
 * up by username and verify the BCrypt password.
 */
@ApplicationScoped
@Transactional
public class AuthServiceImpl implements AuthService {
    private static final Logger log = Logger.getLogger(AuthServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.infof("Login attempt for user '%s'", request.username());

        Optional<Customer> customer = customerRepository.findByUsername(request.username());
        if (customer.isEmpty()
                || !passwordEncoder.matches(request.password(), customer.get().getPassword())) {
            log.warnf("Authentication failed for user '%s'", request.username());
            throw new InvalidCredentialsException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                    "Invalid username or password");
        }

        String token = jwtService.generateToken(request.username());
        log.infof("Login successful for user '%s'", request.username());
        return LoginResponse.bearer(token, jwtService.getExpirationSeconds());
    }
}
