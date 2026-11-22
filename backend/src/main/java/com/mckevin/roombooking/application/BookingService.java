package com.mckevin.roombooking.application;

import com.mckevin.roombooking.application.Views.BookingView;
import com.mckevin.roombooking.application.port.AccountRepository;
import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.application.port.EventPublisher;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.booking.BookingPolicy;
import com.mckevin.roombooking.domain.booking.BookingRequest;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.site.Site;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Réserver, annuler, confirmer sa présence, consulter ses réservations, libérer les salles inoccupées. */
public class BookingService {

    static final int PAST_LIMIT = 30;

    private final Clock clock;
    private final BookingPolicy policy;
    private final SiteRepository sites;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final AccountRepository accounts;
    private final EventPublisher events;

    public BookingService(Clock clock, BookingPolicy policy, SiteRepository sites, RoomRepository rooms,
            BookingRepository bookings, AccountRepository accounts, EventPublisher events) {
        this.clock = clock;
        this.policy = policy;
        this.sites = sites;
        this.rooms = rooms;
        this.bookings = bookings;
        this.accounts = accounts;
        this.events = events;
    }

    public BookingView book(Actor actor, BookingCommand command) {
        Room room = Lookups.room(rooms, RoomId.of(command.roomId()));
        Site site = Lookups.site(sites, room.siteId());
        TimeSlot slot = TimeSlot.of(command.date(), command.start(), command.end());
        BookingRequest request = new BookingRequest(site, room, actor.id(), command.title(), slot, command.attendees());
        Booking booking = policy.book(request,
                bookings.activeOnRooms(List.of(room.id()), slot.date()),
                bookings.activeOfOrganizer(actor.id(), slot.date()),
                now());
        bookings.save(booking);
        events.publish(booking.pullEvents());
        return view(booking, actor, room, site);
    }

    public BookingView cancel(Actor actor, BookingId id, String reason) {
        Booking booking = Lookups.booking(bookings, id);
        booking.cancel(actor.id(), actor.officeManager(), reason, now());
        bookings.save(booking);
        events.publish(booking.pullEvents());
        return view(booking, actor);
    }

    public BookingView checkIn(Actor actor, BookingId id) {
        Booking booking = Lookups.booking(bookings, id);
        booking.checkIn(actor.id(), now());
        bookings.save(booking);
        events.publish(booking.pullEvents());
        return view(booking, actor);
    }

    public List<BookingView> mine(Actor actor, boolean past) {
        LocalDateTime now = now();
        List<Booking> found = past ? bookings.pastOf(actor.id(), now, PAST_LIMIT) : bookings.upcomingOf(actor.id(), now);
        return found.stream().map(b -> view(b, actor)).toList();
    }

    /** Libère les salles réservées dont la présence n'a pas été confirmée à temps. Renvoie le nombre de salles libérées. */
    public int releaseNoShows() {
        LocalDateTime now = now();
        int released = 0;
        for (Booking booking : bookings.confirmedStartedBefore(now.minus(Booking.NO_SHOW_GRACE))) {
            if (booking.releaseIfNoShow(now)) {
                bookings.save(booking);
                events.publish(booking.pullEvents());
                released++;
            }
        }
        return released;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private BookingView view(Booking booking, Actor actor) {
        Room room = Lookups.room(rooms, booking.roomId());
        return view(booking, actor, room, Lookups.site(sites, room.siteId()));
    }

    private BookingView view(Booking booking, Actor actor, Room room, Site site) {
        Map<EmployeeId, String> names = accounts.names(List.of(booking.organizerId()));
        return toView(booking, actor, room, site, names, now());
    }

    static BookingView toView(Booking b, Actor actor, Room room, Site site, Map<EmployeeId, String> names, LocalDateTime now) {
        boolean mine = b.isOrganizedBy(actor.id());
        boolean canCancel = b.isActive() && now.isBefore(b.slot().end()) && (mine || actor.officeManager());
        return new BookingView(b.id().toString(), room.id().value(), room.name(), site.name(), b.title(),
                names.getOrDefault(b.organizerId(), "Collaborateur"), mine, b.slot().date(),
                b.slot().start().toLocalTime(), b.slot().end().toLocalTime(), b.attendees(), b.status().name(),
                b.status().label(), mine && b.canCheckIn(now), canCancel, b.checkInWindow().start(),
                b.checkInWindow().end(), b.cancellationReason());
    }

    static Map<RoomId, Room> byId(List<Room> rooms) {
        return rooms.stream().collect(Collectors.toMap(Room::id, Function.identity()));
    }
}
