package com.mckevin.roombooking.application.notification;

import com.mckevin.roombooking.application.Actor;
import com.mckevin.roombooking.application.Views.NotificationView;
import com.mckevin.roombooking.application.Views.NotificationsView;
import com.mckevin.roombooking.application.port.NotificationRepository;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.domain.booking.BookingEvent;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.shared.DomainEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Informe l'organisateur de ce qui arrive à sa réservation sans son intervention :
 * annulation par un office manager, salle libérée faute de présence confirmée.
 */
public class NotificationService {

    static final int RECENT_LIMIT = 20;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH'h'mm");

    private final NotificationRepository notifications;
    private final RoomRepository rooms;

    public NotificationService(NotificationRepository notifications, RoomRepository rooms) {
        this.notifications = notifications;
        this.rooms = rooms;
    }

    public void on(DomainEvent event) {
        if (event instanceof BookingEvent.Cancelled cancelled && !cancelled.byOrganizer()) {
            notifications.save(Notification.unread(cancelled.organizerId(),
                    "Votre réservation « " + cancelled.title() + " » (" + describe(cancelled.roomId(), cancelled.slot())
                            + ") a été annulée. Motif : " + cancelled.reason(),
                    cancelled.occurredAt()));
        } else if (event instanceof BookingEvent.Released released) {
            notifications.save(Notification.unread(released.organizerId(),
                    "La salle a été libérée faute de présence confirmée : « " + released.title() + " » ("
                            + describe(released.roomId(), released.slot()) + ").",
                    released.occurredAt()));
        }
    }

    public NotificationsView of(Actor actor) {
        return new NotificationsView(notifications.unreadCount(actor.id()),
                notifications.recentOf(actor.id(), RECENT_LIMIT).stream()
                        .map(n -> new NotificationView(n.id(), n.message(), n.createdAt(), n.read()))
                        .toList());
    }

    public void markAllRead(Actor actor) {
        notifications.markAllRead(actor.id());
    }

    private String describe(RoomId roomId, TimeSlot slot) {
        String room = rooms.find(roomId).map(Room::name).orElse(roomId.value());
        return "salle " + room + ", " + DAY.format(slot.date()) + " de " + HOUR.format(slot.start()) + " à "
                + HOUR.format(slot.end());
    }
}
