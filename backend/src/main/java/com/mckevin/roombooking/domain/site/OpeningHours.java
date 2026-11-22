package com.mckevin.roombooking.domain.site;

import com.mckevin.roombooking.domain.booking.TimeSlot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/** Jours et heures d'ouverture d'un site. Un créneau doit y tenir entièrement. */
public record OpeningHours(LocalTime opensAt, LocalTime closesAt, Set<DayOfWeek> days) {

    public OpeningHours {
        if (!opensAt.isBefore(closesAt)) {
            throw new IllegalArgumentException("l'ouverture doit précéder la fermeture");
        }
        if (days.isEmpty()) {
            throw new IllegalArgumentException("au moins un jour d'ouverture");
        }
        days = Set.copyOf(days);
    }

    /** Du lundi au vendredi. */
    public static OpeningHours weekdays(LocalTime opensAt, LocalTime closesAt) {
        return new OpeningHours(opensAt, closesAt, EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
    }

    public boolean isOpenOn(LocalDate date) {
        return days.contains(date.getDayOfWeek());
    }

    public boolean covers(TimeSlot slot) {
        return isOpenOn(slot.date())
                && !slot.start().toLocalTime().isBefore(opensAt)
                && !slot.end().toLocalTime().isAfter(closesAt);
    }

    /** Le créneau d'ouverture complet d'une journée (pour la recherche de créneaux libres). */
    public TimeSlot dayOf(LocalDate date) {
        return new TimeSlot(date.atTime(opensAt), date.atTime(closesAt));
    }
}
