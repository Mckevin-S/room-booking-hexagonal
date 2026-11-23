package com.mckevin.roombooking.application;

import com.mckevin.roombooking.application.Views.PlannedBooking;
import com.mckevin.roombooking.application.Views.PlanningView;
import com.mckevin.roombooking.application.Views.RoomPlanning;
import com.mckevin.roombooking.application.Views.RoomView;
import com.mckevin.roombooking.application.Views.SearchView;
import com.mckevin.roombooking.application.Views.SiteView;
import com.mckevin.roombooking.application.Views.SuggestionView;
import com.mckevin.roombooking.application.port.AccountRepository;
import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.domain.booking.AvailabilitySearch;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.SearchCriteria;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.site.Site;
import com.mckevin.roombooking.domain.site.SiteId;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sites, recherche de salles libres et planning d'une journée. */
public class AvailabilityService {

    private final Clock clock;
    private final AvailabilitySearch search;
    private final SiteRepository sites;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final AccountRepository accounts;

    public AvailabilityService(Clock clock, AvailabilitySearch search, SiteRepository sites, RoomRepository rooms,
            BookingRepository bookings, AccountRepository accounts) {
        this.clock = clock;
        this.search = search;
        this.sites = sites;
        this.rooms = rooms;
        this.bookings = bookings;
        this.accounts = accounts;
    }

    public List<SiteView> sites() {
        return sites.all().stream().sorted(Comparator.comparing(Site::name)).map(SiteView::of).toList();
    }

    public SearchView search(String siteId, LocalDate date, LocalTime start, LocalTime end, int attendees,
            Set<Equipment> equipments) {
        Site site = Lookups.site(sites, SiteId.of(siteId));
        List<Room> siteRooms = rooms.ofSite(site.id());
        SearchCriteria criteria = new SearchCriteria(TimeSlot.of(date, start, end), attendees, equipments);
        List<Booking> dayBookings = bookings.activeOnRooms(siteRooms.stream().map(Room::id).toList(), date);
        AvailabilitySearch.Result result = search.search(site, siteRooms, dayBookings, criteria, now());
        return new SearchView(
                result.rooms().stream().map(RoomView::of).toList(),
                result.suggestions().stream()
                        .map(s -> new SuggestionView(RoomView.of(s.room()), s.slot().start().toLocalTime(),
                                s.slot().end().toLocalTime()))
                        .toList());
    }

    public PlanningView planning(Actor actor, String siteId, LocalDate date) {
        Site site = Lookups.site(sites, SiteId.of(siteId));
        List<Room> siteRooms = rooms.ofSite(site.id()).stream()
                .sorted(Comparator.comparing((Room r) -> r.capacity().seats()).thenComparing(Room::name))
                .toList();
        List<Booking> dayBookings = bookings.activeOnRooms(siteRooms.stream().map(Room::id).toList(), date);
        Map<EmployeeId, String> names = accounts.names(dayBookings.stream().map(Booking::organizerId).distinct().toList());
        List<RoomPlanning> planning = siteRooms.stream()
                .map(room -> new RoomPlanning(RoomView.of(room), dayBookings.stream()
                        .filter(b -> b.roomId().equals(room.id()))
                        .sorted(Comparator.comparing(Booking::slot))
                        .map(b -> new PlannedBooking(b.id().toString(), b.title(),
                                names.getOrDefault(b.organizerId(), "Collaborateur"), b.isOrganizedBy(actor.id()),
                                b.slot().start().toLocalTime(), b.slot().end().toLocalTime(), b.status().name()))
                        .toList()))
                .toList();
        return new PlanningView(SiteView.of(site), date, site.openingHours().isOpenOn(date), planning);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
