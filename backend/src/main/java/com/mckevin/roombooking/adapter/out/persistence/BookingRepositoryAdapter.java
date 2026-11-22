package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.booking.BookingStatus;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class BookingRepositoryAdapter implements BookingRepository {

    static final String NO_OVERLAP_CONSTRAINT = "booking_no_overlap";
    private static final Set<BookingStatus> ACTIVE = EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    private final BookingJpaRepository bookings;

    BookingRepositoryAdapter(BookingJpaRepository bookings) {
        this.bookings = bookings;
    }

    /**
     * Enregistre et écrit immédiatement en base : si une demande concurrente a pris le créneau entre la vérification
     * et l'écriture, la contrainte d'exclusion de PostgreSQL la rejette et le conflit est rendu en règle métier.
     */
    @Override
    public void save(Booking booking) {
        BookingEntity entity = bookings.findById(booking.id().value()).orElseGet(() -> new BookingEntity(booking.id().value()));
        entity.roomId = booking.roomId().value();
        entity.organizerId = booking.organizerId().value();
        entity.title = booking.title();
        entity.startsAt = booking.slot().start();
        entity.endsAt = booking.slot().end();
        entity.attendees = booking.attendees();
        entity.status = booking.status();
        entity.createdAt = booking.createdAt();
        entity.checkedInAt = booking.checkedInAt();
        entity.closedAt = booking.closedAt();
        entity.cancellationReason = booking.cancellationReason();
        try {
            bookings.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            String cause = String.valueOf(e.getMostSpecificCause().getMessage());
            if (cause.contains(NO_OVERLAP_CONSTRAINT)) {
                throw new BusinessRuleViolation("SLOT_TAKEN", "La salle vient d'être réservée sur ce créneau par quelqu'un d'autre.");
            }
            throw e;
        }
    }

    @Override
    public Optional<Booking> find(BookingId id) {
        return bookings.findById(id.value()).map(BookingRepositoryAdapter::toDomain);
    }

    @Override
    public List<Booking> activeOnRooms(Collection<RoomId> rooms, LocalDate date) {
        if (rooms.isEmpty()) {
            return List.of();
        }
        return toDomain(bookings.onRoomsBetween(rooms.stream().map(RoomId::value).toList(), ACTIVE, date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()));
    }

    @Override
    public List<Booking> activeOfOrganizer(EmployeeId organizer, LocalDate date) {
        return toDomain(bookings.ofOrganizerBetween(organizer.value(), ACTIVE, date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
    }

    @Override
    public List<Booking> upcomingOf(EmployeeId organizer, LocalDateTime now) {
        return toDomain(bookings.findByOrganizerIdAndEndsAtAfterOrderByStartsAtAsc(organizer.value(), now));
    }

    @Override
    public List<Booking> pastOf(EmployeeId organizer, LocalDateTime now, int limit) {
        return toDomain(bookings.findByOrganizerIdAndEndsAtLessThanEqualOrderByStartsAtDesc(organizer.value(), now,
                PageRequest.of(0, limit)));
    }

    @Override
    public List<Booking> confirmedStartedBefore(LocalDateTime threshold) {
        return toDomain(bookings.findByStatusAndStartsAtLessThanEqual(BookingStatus.CONFIRMED, threshold));
    }

    @Override
    public List<Booking> activeOnRoomFrom(RoomId room, LocalDateTime now) {
        return toDomain(bookings.findByRoomIdAndStatusInAndEndsAtAfter(room.value(), ACTIVE, now));
    }

    private static List<Booking> toDomain(List<BookingEntity> entities) {
        return entities.stream().map(BookingRepositoryAdapter::toDomain).toList();
    }

    static Booking toDomain(BookingEntity e) {
        return new Booking(new BookingId(e.id), RoomId.of(e.roomId), EmployeeId.of(e.organizerId), e.title,
                new TimeSlot(e.startsAt, e.endsAt), e.attendees, e.createdAt, e.status, e.checkedInAt, e.closedAt,
                e.cancellationReason);
    }
}
