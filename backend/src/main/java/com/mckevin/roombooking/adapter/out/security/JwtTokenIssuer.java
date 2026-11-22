package com.mckevin.roombooking.adapter.out.security;

import com.mckevin.roombooking.application.port.Account;
import com.mckevin.roombooking.application.port.TokenIssuer;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Jeton signé HS256 : sujet = identifiant du collaborateur ; nom et rôle en revendications. */
@Component
class JwtTokenIssuer implements TokenIssuer {

    static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Token issue(Account account) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.validity());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("room-booking-hexagonal")
                .subject(String.valueOf(account.id().value()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("name", account.fullName())
                .claim(ROLE_CLAIM, account.officeManager() ? "OFFICE_MANAGER" : "EMPLOYEE")
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(value, expiresAt);
    }
}
