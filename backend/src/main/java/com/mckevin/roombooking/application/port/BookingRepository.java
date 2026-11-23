package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.RoomId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    /**
     * Enregistre la réservation. Si une autre réservation active occupe déjà la salle sur ce créneau
     * (deux demandes simultanées), l'implémentation lève une {@code BusinessRuleViolation} « SLOT_TAKEN ».
     */
    void save(Booking booking);

    Optional<Booking> find(BookingId id);

    /** Réservations actives des salles données pour une journée. */
    List<Booking> activeOnRooms(Collection<RoomId> rooms, LocalDate date);

    /** Réservations actives d'un organisateur pour une journée, toutes salles confondues. */
    List<Booking> activeOfOrganizer(EmployeeId organizer, LocalDate date);

    /** Réservations d'un organisateur qui ne sont pas encore terminées, de la plus proche à la plus lointaine. */
    List<Booking> upcomingOf(EmployeeId organizer, LocalDateTime now);

    /** Réservations terminées ou closes d'un organisateur, de la plus récente à la plus ancienne. */
    List<Booking> pastOf(EmployeeId organizer, LocalDateTime now, int limit);

    /** Réservations confirmées (présence non confirmée) qui ont commencé avant l'instant donné. */
    List<Booking> confirmedStartedBefore(LocalDateTime threshold);

    /** Réservations actives d'une salle qui ne sont pas encore terminées. */
    List<Booking> activeOnRoomFrom(RoomId room, LocalDateTime now);
}
