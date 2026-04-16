package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.dto.LoginRequest;
import com.ecommerce.gateway.dto.LoginResponse;
import com.ecommerce.gateway.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Handles authentication requests.
 *
 * <p>Exposed at {@code /auth/**} (public — no JWT required).</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;
    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService,
                          ReactiveUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     *
     * <p>Returns {@code 200 OK} with the token on success,
     * or {@code 401 Unauthorized} when credentials are invalid.</p>
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        return userDetailsService.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .map(user -> {
                    List<String> roles = user.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .map(a -> a.replace("ROLE_", ""))
                            .toList();
                    String token = jwtService.generateToken(user.getUsername(), roles);
                    log.info("Successful login: user={} roles={}", user.getUsername(), roles);
                    return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), roles));
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("Failed login attempt for user={}", request.username());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<LoginResponse>build();
                }));
    }
}
