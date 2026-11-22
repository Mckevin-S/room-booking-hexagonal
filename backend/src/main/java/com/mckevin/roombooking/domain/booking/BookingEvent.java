package com.mckevin.roombooking.domain.booking;

import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.shared.DomainEvent;
import java.time.LocalDateTime;

/** Événements du cycle de vie d'une réservation. */
public sealed interface BookingEvent extends DomainEvent {

    BookingId bookingId();

    EmployeeId organizerId();

    record Confirmed(BookingId bookingId, EmployeeId organizerId, RoomId roomId, TimeSlot slot, LocalDateTime occurredAt)
            implements BookingEvent {
    }

    record CheckedIn(BookingId bookingId, EmployeeId organizerId, LocalDateTime occurredAt) implements BookingEvent {
    }

    /** Annulation ; {@code byOrganizer} est faux quand un office manager annule à sa place. */
    record Cancelled(BookingId bookingId, EmployeeId organizerId, RoomId roomId, String title, TimeSlot slot, boolean byOrganizer,
            String reason, LocalDateTime occurredAt) implements BookingEvent {
    }

    /** Salle libérée faute de présence confirmée à temps. */
    record Released(BookingId bookingId, EmployeeId organizerId, RoomId roomId, String title, TimeSlot slot,
            LocalDateTime occurredAt)
            implements BookingEvent {
    }
}
