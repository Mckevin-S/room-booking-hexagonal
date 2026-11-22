package com.mckevin.roombooking.domain.booking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Créneaux libres d'une durée donnée dans une plage, par pas d'un quart d'heure. */
public final class FreeSlots {

    private FreeSlots() {
    }

    public static List<TimeSlot> of(Duration duration, TimeSlot range, Collection<TimeSlot> busy, LocalDateTime notBefore) {
        List<TimeSlot> free = new ArrayList<>();
        LocalDateTime start = range.start();
        while (!start.plus(duration).isAfter(range.end())) {
            TimeSlot candidate = TimeSlot.starting(start, duration);
            if (!start.isBefore(notBefore) && busy.stream().noneMatch(candidate::overlaps)) {
                free.add(candidate);
            }
            start = start.plus(TimeSlot.GRANULARITY);
        }
        return free;
    }
}
