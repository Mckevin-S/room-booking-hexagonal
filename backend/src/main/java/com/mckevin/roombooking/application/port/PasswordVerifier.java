package com.mckevin.roombooking.application.port;

public interface PasswordVerifier {

    boolean matches(String rawPassword, String hash);
}
