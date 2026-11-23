package com.mckevin.roombooking.domain.booking;

import java.util.Objects;
import java.util.UUID;

public record BookingId(UUID value) {

    public BookingId {
        Objects.requireNonNull(value);
    }

    public static BookingId newId() {
        return new BookingId(UUID.randomUUID());
    }

    public static BookingId of(String value) {
        return new BookingId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
