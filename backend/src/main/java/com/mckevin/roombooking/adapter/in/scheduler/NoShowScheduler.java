package com.mckevin.roombooking.adapter.in.scheduler;

import com.mckevin.roombooking.application.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Libère régulièrement les salles réservées dont la présence n'a pas été confirmée à temps. */
@Component
class NoShowScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(NoShowScheduler.class);

    private final BookingService bookings;

    NoShowScheduler(BookingService bookings) {
        this.bookings = bookings;
    }

    @Scheduled(initialDelayString = "${app.no-show.initial-delay:PT5S}", fixedDelayString = "${app.no-show.check-interval}")
    void releaseNoShows() {
        int released = bookings.releaseNoShows();
        if (released > 0) {
            LOG.info("{} salle(s) libérée(s) faute de présence confirmée", released);
        }
    }
}
