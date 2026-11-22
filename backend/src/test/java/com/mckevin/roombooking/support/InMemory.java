package com.mckevin.roombooking.support;

import com.mckevin.roombooking.application.notification.Notification;
import com.mckevin.roombooking.application.port.Account;
import com.mckevin.roombooking.application.port.AccountRepository;
import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.application.port.EventPublisher;
import com.mckevin.roombooking.application.port.NotificationRepository;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.domain.shared.DomainEvent;
import com.mckevin.roombooking.domain.site.Site;
import com.mckevin.roombooking.domain.site.SiteId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Adaptateurs en mémoire, pour les tests des cas d'usage et les scénarios métier. */
public final class InMemory {

    private InMemory() {
    }

    public static final class Sites implements SiteRepository {

        private final Map<SiteId, Site> sites = new LinkedHashMap<>();

        public Sites add(Site site) {
            sites.put(site.id(), site);
            return this;
        }

        @Override
        public List<Site> all() {
            return List.copyOf(sites.values());
        }

        @Override
        public Optional<Site> find(SiteId id) {
            return Optional.ofNullable(sites.get(id));
        }
    }

    public static final class Rooms implements RoomRepository {

        private final Map<RoomId, Room> rooms = new LinkedHashMap<>();

        public Rooms add(Room room) {
            rooms.put(room.id(), room);
            return this;
        }

        @Override
        public Optional<Room> find(RoomId id) {
            return Optional.ofNullable(rooms.get(id));
        }

        @Override
        public List<Room> ofSite(SiteId site) {
            return rooms.values().stream().filter(r -> r.siteId().equals(site)).toList();
        }

        @Override
        public void save(Room room) {
            rooms.put(room.id(), room);
        }
    }

    /** Reproduit la contrainte d'exclusion de la base : pas deux réservations actives qui se chevauchent sur une salle. */
    public static final class Bookings implements BookingRepository {

        private final Map<BookingId, Booking> bookings = new LinkedHashMap<>();

        @Override
        public void save(Booking booking) {
            boolean conflict = booking.isActive() && bookings.values().stream()
                    .anyMatch(other -> !other.id().equals(booking.id()) && other.roomId().equals(booking.roomId())
                            && other.occupies(booking.slot()));
            if (conflict) {
                throw new BusinessRuleViolation("SLOT_TAKEN", "La salle vient d'être réservée sur ce créneau.");
            }
            bookings.put(booking.id(), booking);
        }

        @Override
        public Optional<Booking> find(BookingId id) {
            return Optional.ofNullable(bookings.get(id));
        }

        @Override
        public List<Booking> activeOnRooms(Collection<RoomId> rooms, LocalDate date) {
            return bookings.values().stream()
                    .filter(b -> b.isActive() && rooms.contains(b.roomId()) && b.slot().date().equals(date))
                    .toList();
        }

        @Override
        public List<Booking> activeOfOrganizer(EmployeeId organizer, LocalDate date) {
            return bookings.values().stream()
                    .filter(b -> b.isActive() && b.isOrganizedBy(organizer) && b.slot().date().equals(date))
                    .toList();
        }

        @Override
        public List<Booking> upcomingOf(EmployeeId organizer, LocalDateTime now) {
            return bookings.values().stream()
                    .filter(b -> b.isOrganizedBy(organizer) && b.slot().end().isAfter(now))
                    .sorted(Comparator.comparing(Booking::slot))
                    .toList();
        }

        @Override
        public List<Booking> pastOf(EmployeeId organizer, LocalDateTime now, int limit) {
            return bookings.values().stream()
                    .filter(b -> b.isOrganizedBy(organizer) && !b.slot().end().isAfter(now))
                    .sorted(Comparator.comparing(Booking::slot).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<Booking> confirmedStartedBefore(LocalDateTime threshold) {
            return bookings.values().stream()
                    .filter(b -> b.status() == com.mckevin.roombooking.domain.booking.BookingStatus.CONFIRMED
                            && !b.slot().start().isAfter(threshold))
                    .toList();
        }

        @Override
        public List<Booking> activeOnRoomFrom(RoomId room, LocalDateTime now) {
            return bookings.values().stream()
                    .filter(b -> b.isActive() && b.roomId().equals(room) && b.slot().end().isAfter(now))
                    .toList();
        }

        public List<Booking> all() {
            return List.copyOf(bookings.values());
        }
    }

    public static final class Accounts implements AccountRepository {

        private final Map<EmployeeId, Account> accounts = new LinkedHashMap<>();

        public Accounts add(Account account) {
            accounts.put(account.id(), account);
            return this;
        }

        @Override
        public Optional<Account> byEmail(String email) {
            return accounts.values().stream().filter(a -> a.email().equals(email)).findFirst();
        }

        @Override
        public Optional<Account> byId(EmployeeId id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public Map<EmployeeId, String> names(Collection<EmployeeId> ids) {
            return accounts.values().stream().filter(a -> ids.contains(a.id()))
                    .collect(Collectors.toMap(Account::id, Account::fullName));
        }
    }

    public static final class Notifications implements NotificationRepository {

        private final List<Notification> notifications = new ArrayList<>();

        @Override
        public void save(Notification notification) {
            notifications.add(new Notification((long) notifications.size() + 1, notification.recipient(),
                    notification.message(), notification.createdAt(), notification.read()));
        }

        @Override
        public List<Notification> recentOf(EmployeeId recipient, int limit) {
            return notifications.stream().filter(n -> n.recipient().equals(recipient))
                    .sorted(Comparator.comparing(Notification::createdAt).thenComparing(Notification::id).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public long unreadCount(EmployeeId recipient) {
            return notifications.stream().filter(n -> n.recipient().equals(recipient) && !n.read()).count();
        }

        @Override
        public void markAllRead(EmployeeId recipient) {
            notifications.replaceAll(n -> n.recipient().equals(recipient)
                    ? new Notification(n.id(), n.recipient(), n.message(), n.createdAt(), true) : n);
        }

        public List<Notification> of(EmployeeId recipient) {
            return notifications.stream().filter(n -> n.recipient().equals(recipient)).toList();
        }
    }

    /** Enregistre les événements publiés et les transmet aux abonnés, comme le ferait Spring. */
    public static final class Events implements EventPublisher {

        private final List<DomainEvent> published = new ArrayList<>();
        private final List<Consumer<DomainEvent>> subscribers = new ArrayList<>();

        public Events subscribe(Consumer<DomainEvent> subscriber) {
            subscribers.add(subscriber);
            return this;
        }

        @Override
        public void publish(List<DomainEvent> events) {
            for (DomainEvent event : events) {
                published.add(event);
                subscribers.forEach(s -> s.accept(event));
            }
        }

        public List<DomainEvent> published() {
            return List.copyOf(published);
        }
    }
}
