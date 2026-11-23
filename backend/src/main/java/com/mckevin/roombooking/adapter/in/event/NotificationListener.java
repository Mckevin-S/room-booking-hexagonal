package com.mckevin.roombooking.adapter.in.event;

import com.mckevin.roombooking.application.notification.NotificationService;
import com.mckevin.roombooking.domain.shared.DomainEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Transforme les événements du domaine en notifications, dans la même transaction que le changement. */
@Component
class NotificationListener {

    private final NotificationService notifications;

    NotificationListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @EventListener
    void on(DomainEvent event) {
        notifications.on(event);
    }
}
