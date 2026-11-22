package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.application.port.Account;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Formats d'échange de l'API REST. */
final class ApiDtos {

    private ApiDtos() {
    }

    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    record UserDto(long id, String email, String fullName, String initials, boolean officeManager, String homeSiteId) {

        static UserDto of(Account account) {
            return new UserDto(account.id().value(), account.email(), account.fullName(), initials(account.fullName()),
                    account.officeManager(), account.homeSite().value());
        }
    }

    record SessionDto(String token, Instant expiresAt, UserDto user) {
    }

    record BookingRequest(
            @NotBlank String roomId,
            @NotNull LocalDate date,
            @NotNull LocalTime start,
            @NotNull LocalTime end,
            @NotBlank @Size(max = 80) String title,
            @Min(1) int attendees) {
    }

    record ReasonRequest(@Size(max = 200) String reason) {
    }

    record OutOfServiceRequest(@NotBlank @Size(max = 200) String reason) {
    }

    static String initials(String fullName) {
        return Arrays.stream(fullName.trim().split("\\s+"))
                .filter(part -> !part.isEmpty())
                .limit(2)
                .map(part -> part.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }
}
