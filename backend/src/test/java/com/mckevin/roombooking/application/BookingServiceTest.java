package com.mckevin.roombooking.application;

import static com.mckevin.roombooking.support.Fixtures.CAMILLE;
import static com.mckevin.roombooking.support.Fixtures.CAMILLE_ID;
import static com.mckevin.roombooking.support.Fixtures.HUGO;
import static com.mckevin.roombooking.support.Fixtures.NADIA;
import static com.mckevin.roombooking.support.Fixtures.TODAY;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.application.Views.BookingView;
import com.mckevin.roombooking.domain.booking.BookingEvent;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.booking.BookingStatus;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Agency;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

    private final Agency agency = new Agency();

    private BookingView book(Actor actor, String roomId, LocalDate date, int from, int to, String title) {
        return agency.booking.book(actor, new BookingCommand(roomId, date, LocalTime.of(from, 0), LocalTime.of(to, 0), title, 2));
    }

    @Test
    void reserveUneSalleEtPublieLaConfirmation() {
        BookingView view = book(CAMILLE, "LIL-VAU", TOMORROW, 10, 11, "Point produit");

        assertThat(view.roomId()).isEqualTo("LIL-VAU");
        assertThat(view.roomName()).isEqualTo("Vauban");
        assertThat(view.siteName()).isEqualTo("Lille – Euratechnologies");
        assertThat(view.title()).isEqualTo("Point produit");
        assertThat(view.organizerName()).isEqualTo("Camille Martin");
        assertThat(view.mine()).isTrue();
        assertThat(view.date()).isEqualTo(TOMORROW);
        assertThat(view.start()).isEqualTo(LocalTime.of(10, 0));
        assertThat(view.end()).isEqualTo(LocalTime.of(11, 0));
        assertThat(view.attendees()).isEqualTo(2);
        assertThat(view.status()).isEqualTo("CONFIRMED");
        assertThat(view.statusLabel()).isEqualTo("Confirmée");
        assertThat(view.canCheckIn()).isFalse();
        assertThat(view.canCancel()).isTrue();
        assertThat(view.checkInOpensAt()).isEqualTo(TOMORROW.atTime(9, 50));
        assertThat(view.releasedIfAbsentAt()).isEqualTo(TOMORROW.atTime(10, 15));
        assertThat(view.cancellationReason()).isNull();
        assertThat(agency.bookings.all()).hasSize(1);
        assertThat(agency.events.published()).singleElement().isInstanceOf(BookingEvent.Confirmed.class);
    }

    @Test
    void refuseUnCreneauDejaPris() {
        book(HUGO, "LIL-VAU", TOMORROW, 10, 11, "Rétro");
        assertThatThrownBy(() -> book(CAMILLE, "LIL-VAU", TOMORROW, 10, 11, "Point"))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("SLOT_TAKEN");
    }

    @Test
    void refuseQueLOrganisateurSoitDansDeuxSalles() {
        book(CAMILLE, "LIL-BEF", TOMORROW, 10, 11, "Client");
        assertThatThrownBy(() -> book(CAMILLE, "LIL-VAU", TOMORROW, 10, 11, "Point"))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("ORGANIZER_UNAVAILABLE");
    }

    @Test
    void signaleUneSalleInconnue() {
        assertThatThrownBy(() -> book(CAMILLE, "LIL-XXX", TOMORROW, 10, 11, "Point"))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("LIL-XXX");
    }

    @Test
    void lOrganisateurConfirmeSaPresence() {
        BookingView booked = book(CAMILLE, "LIL-VAU", TODAY, 10, 11, "Point");
        agency.clock.set(TODAY.atTime(9, 55));

        assertThat(agency.booking.mine(CAMILLE, false)).singleElement().satisfies(v -> assertThat(v.canCheckIn()).isTrue());
        BookingView view = agency.booking.checkIn(CAMILLE, BookingId.of(booked.id()));

        assertThat(view.status()).isEqualTo("CHECKED_IN");
        assertThat(view.canCheckIn()).isFalse();
        assertThat(agency.events.published()).last().isInstanceOf(BookingEvent.CheckedIn.class);
    }

    @Test
    void unCollegueVoitLaReservationSansPouvoirAgir() {
        BookingView booked = book(CAMILLE, "LIL-VAU", TODAY, 10, 11, "Point");
        agency.clock.set(TODAY.atTime(9, 55));
        assertThatThrownBy(() -> agency.booking.checkIn(HUGO, BookingId.of(booked.id())))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("NOT_ORGANIZER");
        assertThatThrownBy(() -> agency.booking.cancel(HUGO, BookingId.of(booked.id()), "Besoin"))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("NOT_ORGANIZER");
    }

    @Test
    void lOrganisateurAnnuleSaReservation() {
        BookingView booked = book(CAMILLE, "LIL-VAU", TOMORROW, 10, 11, "Point");

        BookingView view = agency.booking.cancel(CAMILLE, BookingId.of(booked.id()), null);

        assertThat(view.status()).isEqualTo("CANCELLED");
        assertThat(view.canCancel()).isFalse();
        assertThat(agency.notificationStore.of(CAMILLE_ID)).isEmpty();
        assertThat(book(HUGO, "LIL-VAU", TOMORROW, 10, 11, "Rétro").status()).isEqualTo("CONFIRMED");
    }

    @Test
    void lOfficeManagerAnnuleEtLOrganisateurEstPrevenu() {
        BookingView booked = book(CAMILLE, "LIL-VAU", TOMORROW, 10, 11, "Point produit");

        BookingView asSeenByManager = agency.booking.cancel(NADIA, BookingId.of(booked.id()), "Visite du comité de direction");

        assertThat(asSeenByManager.mine()).isFalse();
        assertThat(asSeenByManager.cancellationReason()).isEqualTo("Visite du comité de direction");
        assertThat(agency.notificationStore.of(CAMILLE_ID)).singleElement()
                .satisfies(n -> assertThat(n.message())
                        .isEqualTo("Votre réservation « Point produit » (salle Vauban, mercredi 2 décembre de 10h00 à 11h00)"
                                + " a été annulée. Motif : Visite du comité de direction"));
    }

    @Test
    void lOfficeManagerPeutAnnulerLaReservationDUnCollegue() {
        BookingView booked = book(CAMILLE, "LIL-VAU", TOMORROW, 10, 11, "Point");
        BookingView seenByNadia = agency.availability.planning(NADIA, "LIL", TOMORROW).rooms().stream()
                .flatMap(r -> r.bookings().stream()).findFirst().map(b -> agency.booking.cancel(NADIA, BookingId.of(b.id()), "Travaux"))
                .orElseThrow();
        assertThat(seenByNadia.id()).isEqualTo(booked.id());
    }

    @Test
    void signaleUneReservationIntrouvable() {
        assertThatThrownBy(() -> agency.booking.cancel(CAMILLE, new BookingId(UUID.randomUUID()), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void libereLesSallesSansPresenceConfirmee() {
        BookingView absent = book(CAMILLE, "LIL-VAU", TODAY, 10, 11, "Point");
        BookingView present = book(HUGO, "LIL-BEF", TODAY, 10, 11, "Client");
        BookingView later = book(HUGO, "LIL-BEF", TODAY, 14, 15, "Atelier");
        agency.clock.set(TODAY.atTime(10, 0));
        agency.booking.checkIn(HUGO, BookingId.of(present.id()));

        agency.clock.set(TODAY.atTime(10, 14));
        assertThat(agency.booking.releaseNoShows()).isZero();

        agency.clock.advance(Duration.ofMinutes(1));
        assertThat(agency.booking.releaseNoShows()).isEqualTo(1);
        assertThat(agency.booking.releaseNoShows()).isZero();

        assertThat(status(absent)).isEqualTo(BookingStatus.RELEASED);
        assertThat(status(present)).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(status(later)).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(agency.notificationStore.of(CAMILLE_ID)).singleElement()
                .satisfies(n -> assertThat(n.message()).startsWith("La salle a été libérée faute de présence confirmée"));
        assertThat(book(HUGO, "LIL-VAU", TODAY, 11, 12, "Salle reprise").status()).isEqualTo("CONFIRMED");
    }

    @Test
    void separeLesReservationsAVenirDesReservationsPassees() {
        book(CAMILLE, "LIL-VAU", TODAY, 9, 10, "Café");
        book(CAMILLE, "LIL-VAU", TOMORROW, 14, 15, "Demain");
        book(CAMILLE, "LIL-VAU", TODAY, 16, 17, "Plus tard");
        book(HUGO, "LIL-BEF", TOMORROW, 14, 15, "Pas à moi");
        agency.clock.set(TODAY.atTime(12, 0));

        assertThat(agency.booking.mine(CAMILLE, false)).extracting(BookingView::title).containsExactly("Plus tard", "Demain");
        assertThat(agency.booking.mine(CAMILLE, true)).extracting(BookingView::title).containsExactly("Café");
    }

    private BookingStatus status(BookingView view) {
        return agency.bookings.find(BookingId.of(view.id())).orElseThrow().status();
    }
}
