package com.mckevin.roombooking.application;

import static com.mckevin.roombooking.support.Fixtures.CAMILLE;
import static com.mckevin.roombooking.support.Fixtures.HUGO;
import static com.mckevin.roombooking.support.Fixtures.NADIA;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;

import com.mckevin.roombooking.application.Views.NotificationView;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.support.Agency;
import java.time.Duration;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private final Agency agency = new Agency();

    private BookingId book(Actor actor, int from) {
        return BookingId.of(agency.booking.book(actor, new BookingCommand("LIL-VAU", TOMORROW, LocalTime.of(from, 0),
                LocalTime.of(from + 1, 0), "Réunion de " + from + "h", 2)).id());
    }

    @Test
    void listeLesNotificationsDuCollaborateurDeLaPlusRecenteALaPlusAncienne() {
        BookingId first = book(CAMILLE, 9);
        BookingId second = book(CAMILLE, 11);
        book(HUGO, 14);
        agency.booking.cancel(NADIA, first, "Travaux");
        agency.clock.advance(Duration.ofMinutes(5));
        agency.booking.cancel(NADIA, second, "Visite");

        var view = agency.notifications.of(CAMILLE);

        assertThat(view.unread()).isEqualTo(2);
        assertThat(view.items()).extracting(NotificationView::message)
                .satisfiesExactly(m -> assertThat(m).contains("Réunion de 11h"), m -> assertThat(m).contains("Réunion de 9h"));
        assertThat(view.items()).allSatisfy(n -> assertThat(n.read()).isFalse());
        assertThat(view.items().get(0).createdAt()).isAfter(view.items().get(1).createdAt());
        assertThat(agency.notifications.of(HUGO).items()).isEmpty();
    }

    @Test
    void marqueToutCommeLu() {
        agency.booking.cancel(NADIA, book(CAMILLE, 9), "Travaux");

        agency.notifications.markAllRead(CAMILLE);

        var view = agency.notifications.of(CAMILLE);
        assertThat(view.unread()).isZero();
        assertThat(view.items()).singleElement().satisfies(n -> assertThat(n.read()).isTrue());
    }

    @Test
    void neNotifiePasLesActionsDeLOrganisateurLuiMeme() {
        BookingId id = book(CAMILLE, 9);
        agency.booking.cancel(CAMILLE, id, "Plus besoin");
        assertThat(agency.notifications.of(CAMILLE).items()).isEmpty();
    }
}
