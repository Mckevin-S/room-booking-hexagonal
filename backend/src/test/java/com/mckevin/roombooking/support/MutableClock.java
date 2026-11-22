package com.mckevin.roombooking.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Horloge de test que l'on peut avancer. */
public final class MutableClock extends Clock {

    public static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private Instant instant;

    public MutableClock(LocalDateTime now) {
        this.instant = now.atZone(PARIS).toInstant();
    }

    public void set(LocalDateTime now) {
        this.instant = now.atZone(PARIS).toInstant();
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(duration);
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(instant, PARIS);
    }

    @Override
    public ZoneId getZone() {
        return PARIS;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
