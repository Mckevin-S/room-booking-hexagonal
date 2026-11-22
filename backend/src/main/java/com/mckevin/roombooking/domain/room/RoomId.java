package com.mckevin.roombooking.domain.room;

import java.util.Objects;

public record RoomId(String value) {

    public RoomId {
        Objects.requireNonNull(value, "identifiant de salle requis");
    }

    public static RoomId of(String value) {
        return new RoomId(value);
    }
}
