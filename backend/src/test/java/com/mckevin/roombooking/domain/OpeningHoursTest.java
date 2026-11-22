package com.mckevin.roombooking.domain;

import static com.mckevin.roombooking.support.Fixtures.SATURDAY;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.site.OpeningHours;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpeningHoursTest {

    private final OpeningHours hours = OpeningHours.weekdays(LocalTime.of(8, 0), LocalTime.of(20, 0));

    @Test
    void ouvreDuLundiAuVendredi() {
        assertThat(hours.days()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        assertThat(hours.isOpenOn(TOMORROW)).isTrue();
        assertThat(hours.isOpenOn(SATURDAY)).isFalse();
        assertThat(hours.isOpenOn(SATURDAY.plusDays(1))).isFalse();
    }

    @Test
    void couvreUnCreneauEntierementInclus() {
        assertThat(hours.covers(TimeSlot.of(TOMORROW, LocalTime.of(8, 0), LocalTime.of(20, 0)))).isTrue();
        assertThat(hours.covers(TimeSlot.of(TOMORROW, LocalTime.of(7, 45), LocalTime.of(9, 0)))).isFalse();
        assertThat(hours.covers(TimeSlot.of(TOMORROW, LocalTime.of(19, 0), LocalTime.of(20, 15)))).isFalse();
        assertThat(hours.covers(TimeSlot.of(SATURDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))).isFalse();
    }

    @Test
    void donneLaJourneeDOuverture() {
        assertThat(hours.dayOf(TOMORROW)).isEqualTo(TimeSlot.of(TOMORROW, LocalTime.of(8, 0), LocalTime.of(20, 0)));
    }

    @Test
    void refuseDesHorairesIncoherents() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OpeningHours.weekdays(LocalTime.of(20, 0), LocalTime.of(8, 0)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OpeningHours.weekdays(LocalTime.of(8, 0), LocalTime.of(8, 0)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OpeningHours(LocalTime.of(8, 0), LocalTime.of(20, 0), Set.of()));
    }
}
