package com.mckevin.roombooking.domain;

import static com.mckevin.roombooking.support.Fixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.mckevin.roombooking.domain.booking.TimeSlot;
import java.time.Duration;
import java.time.LocalTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

    private static TimeSlot slot(int fromHour, int fromMinute, int toHour, int toMinute) {
        return TimeSlot.of(TODAY, LocalTime.of(fromHour, fromMinute), LocalTime.of(toHour, toMinute));
    }

    @Nested
    class Construction {

        @Test
        void refuseUnCreneauVideOuInverse() {
            assertThatIllegalArgumentException().isThrownBy(() -> slot(10, 0, 10, 0));
            assertThatIllegalArgumentException().isThrownBy(() -> slot(11, 0, 10, 0));
        }

        @Test
        void refuseUnCreneauSurDeuxJours() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new TimeSlot(TODAY.atTime(23, 0), TODAY.plusDays(1).atTime(1, 0)));
        }

        @Test
        void accepteUnCreneauQuiFinitAMinuit() {
            TimeSlot slot = new TimeSlot(TODAY.atTime(23, 0), TODAY.plusDays(1).atStartOfDay());
            assertThat(slot.duration()).isEqualTo(Duration.ofHours(1));
            assertThat(slot.date()).isEqualTo(TODAY);
        }

        @Test
        void seConstruitDepuisUnDebutEtUneDuree() {
            assertThat(TimeSlot.starting(TODAY.atTime(9, 30), Duration.ofMinutes(45))).isEqualTo(slot(9, 30, 10, 15));
        }
    }

    @Nested
    class Chevauchement {

        @Test
        void deuxCreneauxQuiSeCroisentSeChevauchent() {
            assertThat(slot(10, 0, 11, 0).overlaps(slot(10, 30, 11, 30))).isTrue();
            assertThat(slot(10, 30, 11, 30).overlaps(slot(10, 0, 11, 0))).isTrue();
        }

        @Test
        void unCreneauInclusChevaucheLeCreneauEnglobant() {
            assertThat(slot(9, 0, 12, 0).overlaps(slot(10, 0, 10, 15))).isTrue();
            assertThat(slot(10, 0, 10, 15).overlaps(slot(9, 0, 12, 0))).isTrue();
        }

        @Test
        void deuxReunionsQuiSeSuiventNeSeChevauchentPas() {
            assertThat(slot(10, 0, 11, 0).overlaps(slot(11, 0, 12, 0))).isFalse();
            assertThat(slot(11, 0, 12, 0).overlaps(slot(10, 0, 11, 0))).isFalse();
        }

        @Test
        void deuxCreneauxDistinctsNeSeChevauchentPas() {
            assertThat(slot(8, 0, 9, 0).overlaps(slot(14, 0, 15, 0))).isFalse();
        }

        @Test
        void deuxJoursDifferentsNeSeChevauchentPas() {
            TimeSlot tomorrow = TimeSlot.of(TODAY.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0));
            assertThat(slot(10, 0, 11, 0).overlaps(tomorrow)).isFalse();
        }
    }

    @Test
    void contientSonDebutMaisPasSaFin() {
        TimeSlot slot = slot(10, 0, 11, 0);
        assertThat(slot.contains(TODAY.atTime(10, 0))).isTrue();
        assertThat(slot.contains(TODAY.atTime(10, 59))).isTrue();
        assertThat(slot.contains(TODAY.atTime(11, 0))).isFalse();
        assertThat(slot.contains(TODAY.atTime(9, 59))).isFalse();
    }

    @Test
    void estAligneSurLeQuartDHeure() {
        assertThat(slot(10, 15, 11, 45).isAligned()).isTrue();
        assertThat(slot(10, 10, 11, 0).isAligned()).isFalse();
        assertThat(slot(10, 0, 11, 5).isAligned()).isFalse();
        assertThat(new TimeSlot(TODAY.atTime(10, 0, 30), TODAY.atTime(11, 0)).isAligned()).isFalse();
        assertThat(new TimeSlot(TODAY.atTime(10, 0), TODAY.atTime(11, 0, 0, 1)).isAligned()).isFalse();
    }

    @Test
    void seDecale() {
        assertThat(slot(10, 0, 11, 0).shiftedBy(Duration.ofMinutes(30))).isEqualTo(slot(10, 30, 11, 30));
    }

    @Test
    void seTrieParDebutPuisParFin() {
        assertThat(slot(10, 0, 11, 0)).isLessThan(slot(10, 30, 11, 0));
        assertThat(slot(10, 0, 10, 30)).isLessThan(slot(10, 0, 11, 0));
        assertThat(slot(10, 0, 11, 0).compareTo(slot(10, 0, 11, 0))).isZero();
    }

    @Test
    void sAfficheLisiblement() {
        assertThat(slot(10, 0, 11, 30)).hasToString("2026-12-01 10:00-11:30");
    }
}
