package com.mckevin.roombooking.adapter.out.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration validity) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret doit contenir au moins 32 caractères");
        }
        validity = validity == null ? Duration.ofHours(10) : validity;
    }
}
