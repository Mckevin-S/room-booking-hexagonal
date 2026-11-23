package com.mckevin.roombooking.domain.booking;

public enum BookingStatus {
    CONFIRMED("Confirmée"),
    CHECKED_IN("Présence confirmée"),
    CANCELLED("Annulée"),
    RELEASED("Libérée (absence)");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Une réservation active occupe la salle. */
    public boolean isActive() {
        return this == CONFIRMED || this == CHECKED_IN;
    }
}
