package com.mckevin.roombooking.domain.booking;

import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.domain.site.Site;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Trouve les salles libres pour une réunion, de la mieux ajustée à la moins bien ajustée.
 * Si aucune ne convient, propose les créneaux libres les plus proches de l'horaire demandé.
 */
public final class AvailabilitySearch {

    public static final int MAX_SUGGESTIONS = 3;

    public record Suggestion(Room room, TimeSlot slot) {
    }

    public record Result(List<Room> rooms, List<Suggestion> suggestions) {
    }

    public Result search(Site site, Collection<Room> rooms, Collection<Booking> dayBookings, SearchCriteria criteria,
            LocalDateTime now) {
        TimeSlot slot = criteria.slot();
        BookingPolicy.checkSlot(slot, now);
        if (!site.openingHours().covers(slot)) {
            throw new BusinessRuleViolation("SITE_CLOSED", "Le site " + site.name() + " est fermé sur ce créneau.");
        }
        Comparator<Room> bestFit = Comparator.<Room>comparingInt(r -> r.capacity().unusedSeats(criteria.attendees()))
                .thenComparing(Room::name);
        List<Room> eligible = rooms.stream()
                .filter(r -> r.siteId().equals(site.id()))
                .filter(Room::isInService)
                .filter(r -> r.capacity().fits(criteria.attendees()))
                .filter(r -> r.offers(criteria.equipments()))
                .sorted(bestFit)
                .toList();
        List<Room> available = eligible.stream().filter(r -> isFree(r, slot, dayBookings)).toList();
        if (!available.isEmpty()) {
            return new Result(available, List.of());
        }
        return new Result(List.of(), suggest(site, eligible, dayBookings, slot, now, bestFit));
    }

    private static boolean isFree(Room room, TimeSlot slot, Collection<Booking> bookings) {
        return bookings.stream().noneMatch(b -> b.roomId().equals(room.id()) && b.occupies(slot));
    }

    private static List<Suggestion> suggest(Site site, List<Room> eligible, Collection<Booking> bookings, TimeSlot wanted,
            LocalDateTime now, Comparator<Room> bestFit) {
        TimeSlot day = site.openingHours().dayOf(wanted.date());
        Comparator<Suggestion> closestFirst = Comparator
                .<Suggestion>comparingLong(s -> Math.abs(Duration.between(wanted.start(), s.slot().start()).toMinutes()))
                .thenComparing(s -> s.slot().start())
                .thenComparing(Suggestion::room, bestFit);
        return eligible.stream()
                .flatMap(room -> FreeSlots.of(wanted.duration(), day, busySlots(room, bookings), now).stream()
                        .map(slot -> new Suggestion(room, slot)))
                .sorted(closestFirst)
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private static List<TimeSlot> busySlots(Room room, Collection<Booking> bookings) {
        return bookings.stream().filter(b -> b.roomId().equals(room.id()) && b.isActive()).map(Booking::slot).toList();
    }
}
