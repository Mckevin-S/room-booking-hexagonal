package com.mckevin.roombooking.domain.booking;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Créneau horaire [début, fin[ dans une même journée, en heure locale du site.
 * La fin est exclue : deux réunions qui se suivent (10:00-11:00 puis 11:00-12:00) ne se chevauchent pas.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) implements Comparable<TimeSlot> {

    /** Pas de réservation : les créneaux commencent et finissent sur un quart d'heure. */
    public static final Duration GRANULARITY = Duration.ofMinutes(15);

    public TimeSlot {
        Objects.requireNonNull(start, "début requis");
        Objects.requireNonNull(end, "fin requise");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("le début doit précéder la fin");
        }
        if (!start.toLocalDate().equals(end.toLocalDate()) && !end.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            throw new IllegalArgumentException("un créneau tient dans une seule journée");
        }
    }

    public static TimeSlot of(LocalDate date, LocalTime start, LocalTime end) {
        return new TimeSlot(date.atTime(start), date.atTime(end));
    }

    public static TimeSlot starting(LocalDateTime start, Duration duration) {
        return new TimeSlot(start, start.plus(duration));
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(LocalDateTime instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public LocalDate date() {
        return start.toLocalDate();
    }

    public boolean isAligned() {
        return isAligned(start) && isAligned(end);
    }

    private static boolean isAligned(LocalDateTime instant) {
        return instant.getMinute() % GRANULARITY.toMinutes() == 0 && instant.getSecond() == 0 && instant.getNano() == 0;
    }

    public TimeSlot shiftedBy(Duration offset) {
        return new TimeSlot(start.plus(offset), end.plus(offset));
    }

    @Override
    public int compareTo(TimeSlot other) {
        int byStart = start.compareTo(other.start);
        return byStart != 0 ? byStart : end.compareTo(other.end);
    }

    @Override
    public String toString() {
        return date() + " " + start.toLocalTime() + "-" + end.toLocalTime();
    }
}
