package com.mckevin.roombooking.application.port;

import java.time.Instant;

public interface TokenIssuer {

    record Token(String value, Instant expiresAt) {
    }

    Token issue(Account account);
}
