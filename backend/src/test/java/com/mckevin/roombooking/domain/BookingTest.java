package com.mckevin.roombooking.domain;

import static com.mckevin.roombooking.support.Fixtures.CAMILLE_ID;
import static com.mckevin.roombooking.support.Fixtures.HUGO_ID;
import static com.mckevin.roombooking.support.Fixtures.NADIA_ID;
import static com.mckevin.roombooking.support.Fixtures.NOW;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingEvent;
import com.mckevin.roombooking.domain.booking.BookingPolicy;
import com.mckevin.roombooking.domain.booking.BookingRequest;
import com.mckevin.roombooking.domain.booking.BookingStatus;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Fixtures;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BookingTest {

    /** Demain de 10 h à 11 h. */
    private static final LocalDateTime START = TOMORROW.atTime(10, 0);
    private Booking booking;

    @BeforeEach
    void bookVauban() {
        TimeSlot slot = TimeSlot.of(TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        booking = new BookingPolicy().book(
                new BookingRequest(Fixtures.lille(), Fixtures.vauban(), CAMILLE_ID, "Point produit", slot, 4),
                List.of(), List.of(), NOW);
        booking.pullEvents();
    }

    private static void assertViolation(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo(code);
    }

    @Nested
    class Presence {

        @Test
        void seConfirmeDixMinutesAvantLeDebut() {
            assertThat(booking.canCheckIn(START.minusMinutes(11))).isFalse();
            assertThat(booking.canCheckIn(START.minusMinutes(10))).isTrue();

            booking.checkIn(CAMILLE_ID, START.minusMinutes(10));

            assertThat(booking.status()).isEqualTo(BookingStatus.CHECKED_IN);
            assertThat(booking.checkedInAt()).isEqualTo(START.minusMinutes(10));
            assertThat(booking.isActive()).isTrue();
            assertThat(booking.pullEvents()).singleElement().isInstanceOf(BookingEvent.CheckedIn.class);
        }

        @Test
        void neSeConfirmePlusQuinzeMinutesApresLeDebut() {
            assertThat(booking.canCheckIn(START.plusMinutes(14).plusSeconds(59))).isTrue();
            assertThat(booking.canCheckIn(START.plusMinutes(15))).isFalse();
            assertViolation(() -> booking.checkIn(CAMILLE_ID, START.plusMinutes(15)), "CHECK_IN_WINDOW_CLOSED");
            assertViolation(() -> booking.checkIn(CAMILLE_ID, START.minusMinutes(11)), "CHECK_IN_WINDOW_CLOSED");
        }

        @Test
        void exposeSaFenetreDeConfirmation() {
            assertThat(booking.checkInWindow()).isEqualTo(new TimeSlot(START.minusMinutes(10), START.plusMinutes(15)));
        }

        @Test
        void estReserveeALOrganisateur() {
            assertViolation(() -> booking.checkIn(HUGO_ID, START), "NOT_ORGANIZER");
        }

        @Test
        void neSeConfirmePasDeuxFois() {
            booking.checkIn(CAMILLE_ID, START);
            assertViolation(() -> booking.checkIn(CAMILLE_ID, START.plusMinutes(1)), "ALREADY_CHECKED_IN");
            assertThat(booking.canCheckIn(START.plusMinutes(1))).isFalse();
        }

        @Test
        void neSeConfirmePasSurUneReservationAnnulee() {
            booking.cancel(CAMILLE_ID, false, null, NOW);
            assertViolation(() -> booking.checkIn(CAMILLE_ID, START), "BOOKING_NOT_ACTIVE");
            assertThat(booking.canCheckIn(START)).isFalse();
        }
    }

    @Nested
    class Annulation {

        @Test
        void lOrganisateurAnnuleSansMotif() {
            booking.cancel(CAMILLE_ID, false, "   ", NOW);

            assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.isActive()).isFalse();
            assertThat(booking.closedAt()).isEqualTo(NOW);
            assertThat(booking.cancellationReason()).isNull();
            assertThat(booking.pullEvents()).singleElement().isInstanceOfSatisfying(BookingEvent.Cancelled.class, e -> {
                assertThat(e.byOrganizer()).isTrue();
                assertThat(e.reason()).isNull();
                assertThat(e.title()).isEqualTo("Point produit");
                assertThat(e.slot()).isEqualTo(booking.slot());
                assertThat(e.roomId()).isEqualTo(booking.roomId());
                assertThat(e.organizerId()).isEqualTo(CAMILLE_ID);
                assertThat(e.bookingId()).isEqualTo(booking.id());
                assertThat(e.occurredAt()).isEqualTo(NOW);
            });
        }

        @Test
        void unCollegueNePeutPasAnnuler() {
            assertViolation(() -> booking.cancel(HUGO_ID, false, "Besoin de la salle", NOW), "NOT_ORGANIZER");
            assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        void lOfficeManagerAnnuleAvecUnMotif() {
            assertViolation(() -> booking.cancel(NADIA_ID, true, "  ", NOW), "REASON_REQUIRED");
            assertViolation(() -> booking.cancel(NADIA_ID, true, null, NOW), "REASON_REQUIRED");

            booking.cancel(NADIA_ID, true, "  Visite du comité de direction ", NOW);

            assertThat(booking.cancellationReason()).isEqualTo("Visite du comité de direction");
            assertThat(booking.pullEvents()).singleElement().isInstanceOfSatisfying(BookingEvent.Cancelled.class, e -> {
                assertThat(e.byOrganizer()).isFalse();
                assertThat(e.reason()).isEqualTo("Visite du comité de direction");
            });
        }

        @Test
        void resteAnnulableJusquALaFinDeLaReunion() {
            booking.cancel(CAMILLE_ID, false, null, START.plusMinutes(59));
            assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        void uneReunionTermineeNeSAnnulePlus() {
            assertViolation(() -> booking.cancel(CAMILLE_ID, false, null, START.plusHours(1)), "BOOKING_FINISHED");
        }

        @Test
        void uneReservationAnnuleeNeSAnnulePasDeuxFois() {
            booking.cancel(CAMILLE_ID, false, null, NOW);
            assertViolation(() -> booking.cancel(CAMILLE_ID, false, null, NOW), "BOOKING_NOT_ACTIVE");
        }
    }

    @Nested
    class Absence {

        @Test
        void laSalleEstLibereeQuinzeMinutesApresLeDebut() {
            assertThat(booking.releaseIfNoShow(START.plusMinutes(14))).isFalse();
            assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);

            assertThat(booking.releaseIfNoShow(START.plusMinutes(15))).isTrue();

            assertThat(booking.status()).isEqualTo(BookingStatus.RELEASED);
            assertThat(booking.isActive()).isFalse();
            assertThat(booking.closedAt()).isEqualTo(START.plusMinutes(15));
            assertThat(booking.pullEvents()).singleElement().isInstanceOfSatisfying(BookingEvent.Released.class, e -> {
                assertThat(e.title()).isEqualTo("Point produit");
                assertThat(e.roomId()).isEqualTo(booking.roomId());
                assertThat(e.slot()).isEqualTo(booking.slot());
                assertThat(e.organizerId()).isEqualTo(CAMILLE_ID);
                assertThat(e.bookingId()).isEqualTo(booking.id());
            });
        }

        @Test
        void uneReservationAvecPresenceConfirmeeNestPasLiberee() {
            booking.checkIn(CAMILLE_ID, START);
            assertThat(booking.releaseIfNoShow(START.plusMinutes(30))).isFalse();
            assertThat(booking.status()).isEqualTo(BookingStatus.CHECKED_IN);
        }

        @Test
        void uneReservationAnnuleeNestPasLiberee() {
            booking.cancel(CAMILLE_ID, false, null, NOW);
            booking.pullEvents();
            assertThat(booking.releaseIfNoShow(START.plusMinutes(30))).isFalse();
            assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.pullEvents()).isEmpty();
        }
    }

    @Test
    void occupeLaSalleTantQuElleEstActive() {
        TimeSlot overlapping = TimeSlot.of(TOMORROW, LocalTime.of(10, 30), LocalTime.of(11, 30));
        TimeSlot after = TimeSlot.of(TOMORROW, LocalTime.of(11, 0), LocalTime.of(12, 0));
        assertThat(booking.occupies(overlapping)).isTrue();
        assertThat(booking.occupies(after)).isFalse();
        booking.cancel(CAMILLE_ID, false, null, NOW);
        assertThat(booking.occupies(overlapping)).isFalse();
    }

    @Test
    void connaitSonOrganisateur() {
        assertThat(booking.isOrganizedBy(CAMILLE_ID)).isTrue();
        assertThat(booking.isOrganizedBy(HUGO_ID)).isFalse();
    }

    @Test
    void lesStatutsActifsSontConfirmeeEtPresenceConfirmee() {
        assertThat(BookingStatus.CONFIRMED.isActive()).isTrue();
        assertThat(BookingStatus.CHECKED_IN.isActive()).isTrue();
        assertThat(BookingStatus.CANCELLED.isActive()).isFalse();
        assertThat(BookingStatus.RELEASED.isActive()).isFalse();
        assertThat(BookingStatus.RELEASED.label()).isEqualTo("Libérée (absence)");
    }
}
