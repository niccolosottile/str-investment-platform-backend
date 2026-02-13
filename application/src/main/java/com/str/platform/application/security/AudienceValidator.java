package com.str.platform.application.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Validates that the JWT contains the expected audience.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;

    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (audience == null || audience.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }

        Object audClaim = jwt.getClaims().get("aud");
        boolean valid = false;
        if (audClaim instanceof String audString) {
            valid = audience.equals(audString);
        } else if (audClaim instanceof Collection<?> audCollection) {
            valid = audCollection.contains(audience);
        }

        if (valid) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
