package com.mckevin.roombooking.domain.booking;

import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.domain.shared.DomainEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Réservation d'une salle (agrégat). Elle naît confirmée via {@link BookingPolicy}, puis l'organisateur
 * confirme sa présence ; sans présence confirmée 15 minutes après le début, la salle est libérée.
 */
public final class Booking {

    /** La présence peut être confirmée 10 minutes avant le début. */
    public static final Duration CHECK_IN_OPENS_BEFORE = Duration.ofMinutes(10);
    /** Au-delà de 15 minutes après le début sans présence confirmée, la salle est libérée. */
    public static final Duration NO_SHOW_GRACE = Duration.ofMinutes(15);

    private final BookingId id;
    private final RoomId roomId;
    private final EmployeeId organizerId;
    private final String title;
    private final TimeSlot slot;
    private final int attendees;
    private final LocalDateTime createdAt;
    private BookingStatus status;
    private LocalDateTime checkedInAt;
    private LocalDateTime closedAt;
    private String cancellationReason;
    private final List<DomainEvent> events = new ArrayList<>();

    /** Reconstitution depuis le stockage. */
    public Booking(BookingId id, RoomId roomId, EmployeeId organizerId, String title, TimeSlot slot, int attendees,
            LocalDateTime createdAt, BookingStatus status, LocalDateTime checkedInAt, LocalDateTime closedAt,
            String cancellationReason) {
        this.id = Objects.requireNonNull(id);
        this.roomId = Objects.requireNonNull(roomId);
        this.organizerId = Objects.requireNonNull(organizerId);
        this.title = Objects.requireNonNull(title);
        this.slot = Objects.requireNonNull(slot);
        this.attendees = attendees;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = Objects.requireNonNull(status);
        this.checkedInAt = checkedInAt;
        this.closedAt = closedAt;
        this.cancellationReason = cancellationReason;
    }

    static Booking confirm(RoomId roomId, EmployeeId organizerId, String title, TimeSlot slot, int attendees,
            LocalDateTime now) {
        Booking booking = new Booking(BookingId.newId(), roomId, organizerId, title, slot, attendees, now,
                BookingStatus.CONFIRMED, null, null, null);
        booking.events.add(new BookingEvent.Confirmed(booking.id, organizerId, roomId, slot, now));
        return booking;
    }

    public TimeSlot checkInWindow() {
        return new TimeSlot(slot.start().minus(CHECK_IN_OPENS_BEFORE), slot.start().plus(NO_SHOW_GRACE));
    }

    public boolean canCheckIn(LocalDateTime now) {
        return status == BookingStatus.CONFIRMED && checkInWindow().contains(now);
    }

    public void checkIn(EmployeeId by, LocalDateTime now) {
        requireOrganizer(by);
        if (status == BookingStatus.CHECKED_IN) {
            throw new BusinessRuleViolation("ALREADY_CHECKED_IN", "Votre présence est déjà confirmée.");
        }
        requireActive();
        if (!checkInWindow().contains(now)) {
            throw new BusinessRuleViolation("CHECK_IN_WINDOW_CLOSED",
                    "La présence se confirme entre 10 minutes avant et 15 minutes après le début de la réunion.");
        }
        status = BookingStatus.CHECKED_IN;
        checkedInAt = now;
        events.add(new BookingEvent.CheckedIn(id, organizerId, now));
    }

    /**
     * Annule la réservation. L'organisateur annule librement tant que la réunion n'est pas terminée ;
     * un office manager peut annuler à sa place, avec un motif.
     */
    public void cancel(EmployeeId by, boolean officeManager, String reason, LocalDateTime now) {
        boolean byOrganizer = organizerId.equals(by);
        if (!byOrganizer && !officeManager) {
            throw new BusinessRuleViolation("NOT_ORGANIZER", "Seul l'organisateur peut annuler cette réservation.");
        }
        requireActive();
        if (!now.isBefore(slot.end())) {
            throw new BusinessRuleViolation("BOOKING_FINISHED", "Une réunion terminée ne peut plus être annulée.");
        }
        String cleanReason = reason == null || reason.isBlank() ? null : reason.strip();
        if (!byOrganizer && cleanReason == null) {
            throw new BusinessRuleViolation("REASON_REQUIRED",
                    "Indiquez le motif : l'organisateur en sera informé.");
        }
        status = BookingStatus.CANCELLED;
        closedAt = now;
        cancellationReason = cleanReason;
        events.add(new BookingEvent.Cancelled(id, organizerId, roomId, title, slot, byOrganizer, cleanReason, now));
    }

    /** Libère la salle si la présence n'a pas été confirmée à temps. Renvoie vrai si la salle a été libérée. */
    public boolean releaseIfNoShow(LocalDateTime now) {
        if (status != BookingStatus.CONFIRMED || now.isBefore(slot.start().plus(NO_SHOW_GRACE))) {
            return false;
        }
        status = BookingStatus.RELEASED;
        closedAt = now;
        events.add(new BookingEvent.Released(id, organizerId, roomId, title, slot, now));
        return true;
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean occupies(TimeSlot other) {
        return isActive() && slot.overlaps(other);
    }

    public boolean isOrganizedBy(EmployeeId employee) {
        return organizerId.equals(employee);
    }

    /** Événements produits depuis le dernier appel, à publier une fois l'agrégat enregistré. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    private void requireOrganizer(EmployeeId by) {
        if (!organizerId.equals(by)) {
            throw new BusinessRuleViolation("NOT_ORGANIZER", "Seul l'organisateur peut confirmer sa présence.");
        }
    }

    private void requireActive() {
        if (!isActive()) {
            throw new BusinessRuleViolation("BOOKING_NOT_ACTIVE",
                    "Cette réservation est " + status.label().toLowerCase() + ".");
        }
    }

    public BookingId id() {
        return id;
    }

    public RoomId roomId() {
        return roomId;
    }

    public EmployeeId organizerId() {
        return organizerId;
    }

    public String title() {
        return title;
    }

    public TimeSlot slot() {
        return slot;
    }

    public int attendees() {
        return attendees;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public BookingStatus status() {
        return status;
    }

    public LocalDateTime checkedInAt() {
        return checkedInAt;
    }

    public LocalDateTime closedAt() {
        return closedAt;
    }

    public String cancellationReason() {
        return cancellationReason;
    }
}
