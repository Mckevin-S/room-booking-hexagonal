package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.application.Views.NotificationsView;
import com.mckevin.roombooking.application.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
class NotificationController {

    private final NotificationService notifications;
    private final CurrentActor currentActor;

    NotificationController(NotificationService notifications, CurrentActor currentActor) {
        this.notifications = notifications;
        this.currentActor = currentActor;
    }

    @GetMapping
    @Operation(summary = "Mes 20 dernières notifications et le nombre de non lues")
    NotificationsView mine() {
        return notifications.of(currentActor.get());
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Tout marquer comme lu")
    void markAllRead() {
        notifications.markAllRead(currentActor.get());
    }
}
