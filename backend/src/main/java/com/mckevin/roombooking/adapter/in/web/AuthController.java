package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.adapter.in.web.ApiDtos.LoginRequest;
import com.mckevin.roombooking.adapter.in.web.ApiDtos.SessionDto;
import com.mckevin.roombooking.adapter.in.web.ApiDtos.UserDto;
import com.mckevin.roombooking.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
class AuthController {

    private final AuthenticationService authentication;
    private final CurrentActor currentActor;

    AuthController(AuthenticationService authentication, CurrentActor currentActor) {
        this.authentication = authentication;
        this.currentActor = currentActor;
    }

    @PostMapping("/login")
    @Operation(summary = "Ouvre une session et renvoie un jeton JWT")
    SessionDto login(@Valid @RequestBody LoginRequest request) {
        var session = authentication.login(request.email(), request.password());
        return new SessionDto(session.token().value(), session.token().expiresAt(), UserDto.of(session.account()));
    }

    @GetMapping("/me")
    @Operation(summary = "Collaborateur connecté")
    UserDto me() {
        return UserDto.of(authentication.me(currentActor.get()));
    }
}
