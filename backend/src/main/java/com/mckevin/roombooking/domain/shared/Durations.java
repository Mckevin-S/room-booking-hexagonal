package com.mckevin.roombooking.domain.shared;

import java.time.Duration;

/** Affichage d'une durée en français : « 1 h 30 », « 45 min ». */
public final class Durations {

    private Durations() {
    }

    public static String fr(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours == 0) {
            return minutes + " min";
        }
        return minutes == 0 ? hours + " h" : hours + " h " + String.format("%02d", minutes);
    }
}
