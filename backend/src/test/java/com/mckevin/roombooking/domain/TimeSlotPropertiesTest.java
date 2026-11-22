package com.mckevin.roombooking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.mckevin.roombooking.domain.booking.TimeSlot;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Propriétés du chevauchement, vérifiées sur des milliers de créneaux générés.
 * Le modèle de référence compare des minutes entières : [a, b[ et [c, d[ se croisent si et seulement si a < d et c < b.
 */
class TimeSlotPropertiesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);

    @Provide
    Arbitrary<TimeSlot> slots() {
        Arbitrary<Integer> startQuarter = Arbitraries.integers().between(32, 79); // 8h00 → 19h45
        Arbitrary<Integer> lengthQuarters = Arbitraries.integers().between(1, 16); // 15 min → 4 h
        return Combinators.combine(startQuarter, lengthQuarters).as((start, length) -> {
            LocalDateTime from = DAY.atStartOfDay().plusMinutes(15L * start);
            return TimeSlot.starting(from, Duration.ofMinutes(15L * Math.min(length, 80 - start)));
        });
    }

    @Property
    void leChevauchementEstSymetrique(@ForAll("slots") TimeSlot a, @ForAll("slots") TimeSlot b) {
        assertThat(a.overlaps(b)).isEqualTo(b.overlaps(a));
    }

    @Property
    void unCreneauSeChevaucheLuiMeme(@ForAll("slots") TimeSlot a) {
        assertThat(a.overlaps(a)).isTrue();
    }

    @Property
    void leCreneauSuivantNeChevauchePas(@ForAll("slots") TimeSlot a) {
        assertThat(a.overlaps(a.shiftedBy(a.duration()))).isFalse();
        assertThat(a.shiftedBy(a.duration()).overlaps(a)).isFalse();
    }

    @Property
    void correspondAuModeleEnMinutes(@ForAll("slots") TimeSlot a, @ForAll("slots") TimeSlot b) {
        long a1 = minutes(a.start());
        long a2 = minutes(a.end());
        long b1 = minutes(b.start());
        long b2 = minutes(b.end());
        boolean intersect = Math.max(a1, b1) < Math.min(a2, b2);
        assertThat(a.overlaps(b)).isEqualTo(intersect);
    }

    @Property
    void seChevauchentSiEtSeulementSiUnInstantCommunExiste(@ForAll("slots") TimeSlot a, @ForAll("slots") TimeSlot b) {
        boolean commonInstant = a.contains(b.start()) || b.contains(a.start());
        assertThat(a.overlaps(b)).isEqualTo(commonInstant);
    }

    @Property
    void lesCreneauxGeneresSontAlignes(@ForAll("slots") TimeSlot a) {
        assertThat(a.isAligned()).isTrue();
        assertThat(a.duration()).isPositive();
    }

    private static long minutes(LocalDateTime instant) {
        return Duration.between(DAY.atStartOfDay(), instant).toMinutes();
    }
}
