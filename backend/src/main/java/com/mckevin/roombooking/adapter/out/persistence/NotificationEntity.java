package com.mckevin.roombooking.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "recipient_id")
    long recipientId;
    String message;
    @Column(name = "created_at")
    LocalDateTime createdAt;
    @Column(name = "is_read")
    boolean read;

    protected NotificationEntity() {
    }

    NotificationEntity(long recipientId, String message, LocalDateTime createdAt) {
        this.recipientId = recipientId;
        this.message = message;
        this.createdAt = createdAt;
    }
}
