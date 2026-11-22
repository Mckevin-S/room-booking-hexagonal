package com.mckevin.roombooking.application.notification;

import com.mckevin.roombooking.domain.employee.EmployeeId;
import java.time.LocalDateTime;

/** Message adressé à un collaborateur (annulation par un office manager, salle libérée...). */
public record Notification(Long id, EmployeeId recipient, String message, LocalDateTime createdAt, boolean read) {

    public static Notification unread(EmployeeId recipient, String message, LocalDateTime createdAt) {
        return new Notification(null, recipient, message, createdAt, false);
    }
}
