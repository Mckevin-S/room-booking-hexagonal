package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.domain.booking.BookingStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    @Query("""
            select b from BookingEntity b
            where b.roomId in :rooms and b.status in :statuses and b.startsAt >= :from and b.startsAt < :to
            """)
    List<BookingEntity> onRoomsBetween(@Param("rooms") Collection<String> rooms,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select b from BookingEntity b
            where b.organizerId = :organizer and b.status in :statuses and b.startsAt >= :from and b.startsAt < :to
            """)
    List<BookingEntity> ofOrganizerBetween(@Param("organizer") long organizer,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<BookingEntity> findByOrganizerIdAndEndsAtAfterOrderByStartsAtAsc(long organizerId, LocalDateTime now);

    List<BookingEntity> findByOrganizerIdAndEndsAtLessThanEqualOrderByStartsAtDesc(long organizerId, LocalDateTime now,
            Pageable page);

    List<BookingEntity> findByStatusAndStartsAtLessThanEqual(BookingStatus status, LocalDateTime threshold);

    List<BookingEntity> findByRoomIdAndStatusInAndEndsAtAfter(String roomId, Collection<BookingStatus> statuses,
            LocalDateTime now);
}
