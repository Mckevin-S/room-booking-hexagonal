package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.domain.booking.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "booking")
/** L'identifiant est attribué par le domaine : {@link Persistable} indique à Spring Data s'il faut insérer ou mettre à jour. */
class BookingEntity implements Persistable<UUID> {

    @Id
    UUID id;
    @Column(name = "room_id")
    String roomId;
    @Column(name = "organizer_id")
    long organizerId;
    String title;
    @Column(name = "starts_at")
    LocalDateTime startsAt;
    @Column(name = "ends_at")
    LocalDateTime endsAt;
    int attendees;
    @Enumerated(EnumType.STRING)
    BookingStatus status;
    @Column(name = "created_at")
    LocalDateTime createdAt;
    @Column(name = "checked_in_at")
    LocalDateTime checkedInAt;
    @Column(name = "closed_at")
    LocalDateTime closedAt;
    @Column(name = "cancellation_reason")
    String cancellationReason;
    /** Verrou optimiste : deux modifications concurrentes d'une même réservation ne s'écrasent pas. */
    @Version
    long version;

    @Transient
    private boolean fresh;

    protected BookingEntity() {
    }

    BookingEntity(UUID id) {
        this.id = id;
        this.fresh = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return fresh;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        fresh = false;
    }
}
