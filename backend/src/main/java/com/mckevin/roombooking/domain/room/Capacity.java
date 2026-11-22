package com.mckevin.roombooking.domain.room;

/** Nombre de places assises d'une salle. */
public record Capacity(int seats) {

    public Capacity {
        if (seats < 1) {
            throw new IllegalArgumentException("une salle compte au moins une place");
        }
    }

    public static Capacity of(int seats) {
        return new Capacity(seats);
    }

    public boolean fits(int attendees) {
        return attendees <= seats;
    }

    /** Places inoccupées pour une réunion de cette taille (critère du meilleur ajustement). */
    public int unusedSeats(int attendees) {
        return seats - attendees;
    }
}
