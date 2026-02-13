package com.str.platform.application.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return Map.of("authenticated", false);
        }

        return Map.of(
            "authenticated", true,
            "userId", jwt.getSubject(),
            "email", jwt.getClaimAsString("email"),
            "role", jwt.getClaimAsString("role"),
            "audience", jwt.getClaimAsString("aud"),
            "issuedAt", toEpoch(jwt.getIssuedAt()),
            "expiresAt", toEpoch(jwt.getExpiresAt())
        );
    }

    private Long toEpoch(Instant instant) {
        return instant == null ? null : instant.getEpochSecond();
    }
}
