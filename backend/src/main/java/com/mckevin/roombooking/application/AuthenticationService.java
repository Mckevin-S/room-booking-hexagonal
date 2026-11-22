package com.mckevin.roombooking.application;

import com.mckevin.roombooking.application.port.Account;
import com.mckevin.roombooking.application.port.AccountRepository;
import com.mckevin.roombooking.application.port.PasswordVerifier;
import com.mckevin.roombooking.application.port.TokenIssuer;
import java.util.Locale;

public class AuthenticationService {

    public record Session(TokenIssuer.Token token, Account account) {
    }

    private final AccountRepository accounts;
    private final PasswordVerifier passwords;
    private final TokenIssuer tokens;

    public AuthenticationService(AccountRepository accounts, PasswordVerifier passwords, TokenIssuer tokens) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public Session login(String email, String password) {
        Account account = accounts.byEmail(email == null ? "" : email.strip().toLowerCase(Locale.ROOT))
                .filter(a -> password != null && passwords.matches(password, a.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new Session(tokens.issue(account), account);
    }

    public Account me(Actor actor) {
        return accounts.byId(actor.id()).orElseThrow(() -> new NotFoundException("Compte introuvable."));
    }
}
