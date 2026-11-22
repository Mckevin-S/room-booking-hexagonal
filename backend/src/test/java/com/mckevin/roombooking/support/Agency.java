package com.mckevin.roombooking.support;

import com.mckevin.roombooking.application.AuthenticationService;
import com.mckevin.roombooking.application.AvailabilityService;
import com.mckevin.roombooking.application.BookingService;
import com.mckevin.roombooking.application.RoomService;
import com.mckevin.roombooking.application.notification.NotificationService;
import com.mckevin.roombooking.application.port.TokenIssuer;
import com.mckevin.roombooking.domain.booking.AvailabilitySearch;
import com.mckevin.roombooking.domain.booking.BookingPolicy;
import java.time.Instant;

/** Application complète assemblée sur les adaptateurs en mémoire. */
public final class Agency {

    public final MutableClock clock = new MutableClock(Fixtures.NOW);
    public final InMemory.Sites sites = new InMemory.Sites().add(Fixtures.lille()).add(Fixtures.paris());
    public final InMemory.Rooms rooms = new InMemory.Rooms()
            .add(Fixtures.box()).add(Fixtures.vauban()).add(Fixtures.beffroi()).add(Fixtures.grandPlace()).add(Fixtures.seine());
    public final InMemory.Bookings bookings = new InMemory.Bookings();
    public final InMemory.Accounts accounts = new InMemory.Accounts()
            .add(Fixtures.account(Fixtures.CAMILLE)).add(Fixtures.account(Fixtures.HUGO)).add(Fixtures.account(Fixtures.NADIA));
    public final InMemory.Notifications notificationStore = new InMemory.Notifications();
    public final NotificationService notifications = new NotificationService(notificationStore, rooms);
    public final InMemory.Events events = new InMemory.Events().subscribe(notifications::on);

    public final BookingService booking = new BookingService(clock, new BookingPolicy(), sites, rooms, bookings, accounts, events);
    public final AvailabilityService availability =
            new AvailabilityService(clock, new AvailabilitySearch(), sites, rooms, bookings, accounts);
    public final RoomService roomAdmin = new RoomService(clock, sites, rooms, bookings, events);
    public final AuthenticationService authentication = new AuthenticationService(accounts,
            (raw, hash) -> hash.equals("{noop}" + raw),
            account -> new TokenIssuer.Token("jeton-" + account.id().value(), Instant.parse("2030-01-01T00:00:00Z")));
}
