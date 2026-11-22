package com.mckevin.roombooking.domain;

import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;

import com.mckevin.roombooking.domain.booking.FreeSlots;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FreeSlotsTest {

    private static TimeSlot slot(int fromHour, int fromMinute, int toHour, int toMinute) {
        return TimeSlot.of(TOMORROW, LocalTime.of(fromHour, fromMinute), LocalTime.of(toHour, toMinute));
    }

    @Test
    void parcourtLaPlageParQuartDHeureEnEvitantLesCreneauxOccupes() {
        List<TimeSlot> free = FreeSlots.of(Duration.ofMinutes(30), slot(8, 0, 10, 0), List.of(slot(8, 30, 9, 0)),
                TOMORROW.atTime(8, 0));
        assertThat(free).containsExactly(slot(8, 0, 8, 30), slot(9, 0, 9, 30), slot(9, 15, 9, 45), slot(9, 30, 10, 0));
    }

    @Test
    void ignoreLesCreneauxAvantLHeureIndiquee() {
        List<TimeSlot> free = FreeSlots.of(Duration.ofMinutes(30), slot(8, 0, 10, 0), List.of(), TOMORROW.atTime(9, 10));
        assertThat(free).containsExactly(slot(9, 15, 9, 45), slot(9, 30, 10, 0));
    }

    @Test
    void neRenvoieRienSiLaDureeDepasseLaPlage() {
        assertThat(FreeSlots.of(Duration.ofHours(3), slot(8, 0, 10, 0), List.of(), TOMORROW.atTime(8, 0))).isEmpty();
    }

    @Test
    void uneDureeEgaleALaPlageDonneUnSeulCreneau() {
        assertThat(FreeSlots.of(Duration.ofHours(2), slot(8, 0, 10, 0), List.of(), TOMORROW.atTime(8, 0)))
                .containsExactly(slot(8, 0, 10, 0));
    }
}
