package com.mckevin.roombooking.domain.booking;

import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.domain.shared.Durations;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * Règles d'acceptation d'une réservation. Les règles sont vérifiées dans l'ordre où un utilisateur les comprend :
 * la demande elle-même, puis le site, la salle, et enfin les conflits.
 */
public final class BookingPolicy {

    public static final Duration MAX_DURATION = Duration.ofHours(4);
    public static final int HORIZON_DAYS = 30;
    public static final int MAX_TITLE_LENGTH = 80;

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH'h'mm");

    /**
     * @param roomBookings      réservations de la salle ce jour-là
     * @param organizerBookings réservations de l'organisateur ce jour-là, toutes salles confondues
     */
    public Booking book(BookingRequest request, Collection<Booking> roomBookings, Collection<Booking> organizerBookings,
            LocalDateTime now) {
        TimeSlot slot = request.slot();
        String title = request.title() == null ? "" : request.title().strip();
        if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessRuleViolation("INVALID_TITLE",
                    "Donnez un objet à la réunion (" + MAX_TITLE_LENGTH + " caractères au plus).");
        }
        if (request.attendees() < 1) {
            throw new BusinessRuleViolation("INVALID_ATTENDEES", "Une réunion compte au moins une personne.");
        }
        checkSlot(slot, now);
        if (!request.site().openingHours().covers(slot)) {
            var hours = request.site().openingHours();
            throw new BusinessRuleViolation("SITE_CLOSED", "Le site " + request.site().name() + " est ouvert du lundi au vendredi de "
                    + HOUR.format(hours.opensAt()) + " à " + HOUR.format(hours.closesAt()) + ".");
        }
        if (!request.room().isInService()) {
            throw new BusinessRuleViolation("ROOM_OUT_OF_SERVICE",
                    "La salle " + request.room().name() + " est hors service : " + request.room().outOfServiceReason() + ".");
        }
        if (!request.room().capacity().fits(request.attendees())) {
            throw new BusinessRuleViolation("OVER_CAPACITY", "La salle " + request.room().name() + " compte "
                    + request.room().capacity().seats() + " places pour " + request.attendees() + " participants.");
        }
        if (roomBookings.stream().anyMatch(b -> b.roomId().equals(request.room().id()) && b.occupies(slot))) {
            throw new BusinessRuleViolation("SLOT_TAKEN", "La salle " + request.room().name() + " est déjà réservée sur ce créneau.");
        }
        if (organizerBookings.stream().anyMatch(b -> b.isOrganizedBy(request.organizer()) && b.occupies(slot))) {
            throw new BusinessRuleViolation("ORGANIZER_UNAVAILABLE",
                    "Vous avez déjà une réunion sur ce créneau dans une autre salle.");
        }
        return Booking.confirm(request.room().id(), request.organizer(), title, slot, request.attendees(), now);
    }

    /** Règles propres au créneau, communes à la réservation et à la recherche. */
    public static void checkSlot(TimeSlot slot, LocalDateTime now) {
        if (!slot.isAligned()) {
            throw new BusinessRuleViolation("SLOT_NOT_ALIGNED", "Les réservations se font par quart d'heure.");
        }
        if (slot.start().isBefore(now)) {
            throw new BusinessRuleViolation("SLOT_IN_PAST", "Ce créneau est déjà commencé ou passé.");
        }
        if (slot.date().isAfter(now.toLocalDate().plusDays(HORIZON_DAYS))) {
            throw new BusinessRuleViolation("BEYOND_HORIZON",
                    "Les salles se réservent au plus " + HORIZON_DAYS + " jours à l'avance.");
        }
        if (slot.duration().compareTo(MAX_DURATION) > 0) {
            throw new BusinessRuleViolation("TOO_LONG",
                    "Une réservation dure au plus " + Durations.fr(MAX_DURATION) + ".");
        }
    }
}
