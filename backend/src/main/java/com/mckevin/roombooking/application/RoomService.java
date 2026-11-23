package com.mckevin.roombooking.application;

import com.mckevin.roombooking.application.Views.OutOfServiceView;
import com.mckevin.roombooking.application.Views.RoomView;
import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.application.port.EventPublisher;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.site.SiteId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Gestion des salles par l'office manager : mise hors service et remise en service. */
public class RoomService {

    private final Clock clock;
    private final SiteRepository sites;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final EventPublisher events;

    public RoomService(Clock clock, SiteRepository sites, RoomRepository rooms, BookingRepository bookings,
            EventPublisher events) {
        this.clock = clock;
        this.sites = sites;
        this.rooms = rooms;
        this.bookings = bookings;
        this.events = events;
    }

    public List<RoomView> rooms(String siteId) {
        SiteId id = Lookups.site(sites, SiteId.of(siteId)).id();
        return rooms.ofSite(id).stream()
                .sorted(Comparator.comparing((Room r) -> r.capacity().seats()).thenComparing(Room::name))
                .map(RoomView::of)
                .toList();
    }

    /**
     * Met une salle hors service. Les réservations à venir sont annulées avec le motif,
     * et leurs organisateurs en sont informés.
     */
    public OutOfServiceView takeOutOfService(Actor actor, String roomId, String reason) {
        requireOfficeManager(actor);
        Room room = Lookups.room(rooms, RoomId.of(roomId));
        room.takeOutOfService(reason);
        rooms.save(room);
        LocalDateTime now = now();
        List<Booking> affected = bookings.activeOnRoomFrom(room.id(), now);
        for (Booking booking : affected) {
            booking.cancel(actor.id(), true, "Salle " + room.name() + " hors service : " + room.outOfServiceReason(), now);
            bookings.save(booking);
            events.publish(booking.pullEvents());
        }
        return new OutOfServiceView(RoomView.of(room), affected.size());
    }

    public RoomView putBackInService(Actor actor, String roomId) {
        requireOfficeManager(actor);
        Room room = Lookups.room(rooms, RoomId.of(roomId));
        room.putBackInService();
        rooms.save(room);
        return RoomView.of(room);
    }

    private static void requireOfficeManager(Actor actor) {
        if (!actor.officeManager()) {
            throw new ForbiddenException("Action réservée aux office managers.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
