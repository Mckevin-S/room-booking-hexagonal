package com.mckevin.roombooking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.mckevin.roombooking.domain.shared.Durations;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationsTest {

    @Test
    void afficheLesDureesEnFrancais() {
        assertThat(Durations.fr(Duration.ofMinutes(45))).isEqualTo("45 min");
        assertThat(Durations.fr(Duration.ofHours(4))).isEqualTo("4 h");
        assertThat(Durations.fr(Duration.ofMinutes(90))).isEqualTo("1 h 30");
        assertThat(Durations.fr(Duration.ofMinutes(65))).isEqualTo("1 h 05");
    }
}
