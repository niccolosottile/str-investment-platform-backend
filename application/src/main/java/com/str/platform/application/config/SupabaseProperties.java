package com.str.platform.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Supabase authentication configuration properties.
 */
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {
    private String url;
    private String jwkSetUri;
    private String jwtIssuer;
    private String audience;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
