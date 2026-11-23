package com.mckevin.roombooking.adapter.out.security;

import com.mckevin.roombooking.application.port.PasswordVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder encoder;

    BCryptPasswordVerifier(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
