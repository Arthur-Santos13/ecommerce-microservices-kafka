package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.dto.LoginRequest;
import com.ecommerce.gateway.dto.LoginResponse;
import com.ecommerce.gateway.dto.RefreshRequest;
import com.ecommerce.gateway.security.JwtService;
import com.ecommerce.gateway.security.RefreshTokenService;
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
 * Handles authentication requests: login, token refresh, and logout.
 *
 * <p>All endpoints under {@code /auth/**} are public — no JWT required.</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          ReactiveUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and returns a signed JWT access token + opaque refresh token.
     *
     * <p>Returns {@code 200 OK} with the token pair on success,
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
                    String accessToken  = jwtService.generateToken(user.getUsername(), roles);
                    String refreshToken = refreshTokenService.createRefreshToken(user.getUsername(), roles);
                    log.info("Successful login: user={} roles={}", user.getUsername(), roles);
                    return ResponseEntity.ok(
                            new LoginResponse(accessToken, refreshToken, user.getUsername(), roles));
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("Failed login attempt for user={}", request.username());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<LoginResponse>build();
                }));
    }

    /**
     * Issues a new access token given a valid refresh token (token rotation).
     *
     * <p>The old refresh token is revoked and a new one is issued.
     * Returns {@code 401 Unauthorized} if the token is missing, expired, or invalid.</p>
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refresh(@RequestBody RefreshRequest request) {
        return Mono.fromCallable(() -> {
            if (request.refreshToken() == null || request.refreshToken().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<LoginResponse>build();
            }
            RefreshTokenService.TokenEntry entry = refreshTokenService.validate(request.refreshToken());
            if (entry == null) {
                log.warn("Invalid or expired refresh token presented");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<LoginResponse>build();
            }
            // Rotate: revoke old, issue new pair
            refreshTokenService.revoke(request.refreshToken());
            String newAccessToken  = jwtService.generateToken(entry.username(), entry.roles());
            String newRefreshToken = refreshTokenService.createRefreshToken(entry.username(), entry.roles());
            log.info("Access token refreshed for user={}", entry.username());
            return ResponseEntity.ok(
                    new LoginResponse(newAccessToken, newRefreshToken, entry.username(), entry.roles()));
        });
    }

    /**
     * Revokes the given refresh token (logout).
     *
     * <p>Returns {@code 204 No Content} regardless of token validity to avoid leaking info.</p>
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestBody RefreshRequest request) {
        return Mono.fromCallable(() -> {
            if (request.refreshToken() != null && !request.refreshToken().isBlank()) {
                refreshTokenService.revoke(request.refreshToken());
            }
            return ResponseEntity.noContent().<Void>build();
        });
    }
}
