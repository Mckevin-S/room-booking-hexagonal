package com.mckevin.roombooking.domain;

import static com.mckevin.roombooking.support.Fixtures.CAMILLE_ID;
import static com.mckevin.roombooking.support.Fixtures.HUGO_ID;
import static com.mckevin.roombooking.support.Fixtures.NOW;
import static com.mckevin.roombooking.support.Fixtures.SATURDAY;
import static com.mckevin.roombooking.support.Fixtures.TODAY;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingEvent;
import com.mckevin.roombooking.domain.booking.BookingPolicy;
import com.mckevin.roombooking.domain.booking.BookingRequest;
import com.mckevin.roombooking.domain.booking.BookingStatus;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Fixtures;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class BookingPolicyTest {

    private final BookingPolicy policy = new BookingPolicy();
    private final Room vauban = Fixtures.vauban();

    private static TimeSlot slot(LocalDate date, int fromHour, int fromMinute, int toHour, int toMinute) {
        return TimeSlot.of(date, LocalTime.of(fromHour, fromMinute), LocalTime.of(toHour, toMinute));
    }

    private BookingRequest request(Room room, EmployeeId organizer, String title, TimeSlot slot, int attendees) {
        return new BookingRequest(Fixtures.lille(), room, organizer, title, slot, attendees);
    }

    private Booking book(BookingRequest request, List<Booking> roomBookings, List<Booking> organizerBookings) {
        return policy.book(request, roomBookings, organizerBookings, NOW);
    }

    private Booking book(BookingRequest request) {
        return book(request, List.of(), List.of());
    }

    private static void assertViolation(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo(code);
    }

    @Test
    void confirmeUneDemandeValide() {
        Booking booking = book(request(vauban, CAMILLE_ID, "  Point produit  ", slot(TOMORROW, 10, 0, 11, 0), 5));

        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.title()).isEqualTo("Point produit");
        assertThat(booking.roomId()).isEqualTo(vauban.id());
        assertThat(booking.organizerId()).isEqualTo(CAMILLE_ID);
        assertThat(booking.attendees()).isEqualTo(5);
        assertThat(booking.createdAt()).isEqualTo(NOW);
        assertThat(booking.pullEvents()).singleElement().isInstanceOfSatisfying(BookingEvent.Confirmed.class, e -> {
            assertThat(e.bookingId()).isEqualTo(booking.id());
            assertThat(e.organizerId()).isEqualTo(CAMILLE_ID);
            assertThat(e.roomId()).isEqualTo(vauban.id());
            assertThat(e.slot()).isEqualTo(booking.slot());
            assertThat(e.occurredAt()).isEqualTo(NOW);
        });
        assertThat(booking.pullEvents()).isEmpty();
    }

    @Test
    void exigeUnObjetDeReunion() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "   ", slot(TOMORROW, 10, 0, 11, 0), 2)), "INVALID_TITLE");
        assertViolation(() -> book(request(vauban, CAMILLE_ID, null, slot(TOMORROW, 10, 0, 11, 0), 2)), "INVALID_TITLE");
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "x".repeat(81), slot(TOMORROW, 10, 0, 11, 0), 2)),
                "INVALID_TITLE");
        assertThat(book(request(vauban, CAMILLE_ID, "x".repeat(80), slot(TOMORROW, 10, 0, 11, 0), 2)).title()).hasSize(80);
    }

    @Test
    void exigeAuMoinsUnParticipant() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 0)), "INVALID_ATTENDEES");
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 1)).attendees()).isEqualTo(1);
    }

    @Test
    void reserveParQuartDHeure() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 5, 11, 0), 2)), "SLOT_NOT_ALIGNED");
    }

    @Test
    void refuseUnCreneauCommenceOuPasse() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TODAY, 8, 45, 9, 30), 2)), "SLOT_IN_PAST");
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TODAY, 9, 0, 9, 30), 2))).isNotNull();
    }

    @Test
    void reserveAuPlusTrenteJoursALAvance() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TODAY.plusDays(31), 10, 0, 11, 0), 2)),
                "BEYOND_HORIZON");
        // 31 décembre : un jeudi, 30 jours après le 1er
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TODAY.plusDays(30), 10, 0, 11, 0), 2))).isNotNull();
    }

    @Test
    void limiteLaDureeAQuatreHeures() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Atelier", slot(TOMORROW, 9, 0, 13, 15), 2)), "TOO_LONG");
        assertThat(book(request(vauban, CAMILLE_ID, "Atelier", slot(TOMORROW, 9, 0, 13, 0), 2))).isNotNull();
    }

    @Test
    void respecteLesHorairesDuSite() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 7, 45, 8, 30), 2)), "SITE_CLOSED");
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 19, 30, 20, 15), 2)), "SITE_CLOSED");
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(SATURDAY, 10, 0, 11, 0), 2)), "SITE_CLOSED");
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 8, 0, 9, 0), 2))).isNotNull();
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 19, 0, 20, 0), 2))).isNotNull();
    }

    @Test
    void leMessageDeFermetureIndiqueLesHoraires() {
        assertThatThrownBy(() -> book(request(vauban, CAMILLE_ID, "Point", slot(SATURDAY, 10, 0, 11, 0), 2)))
                .hasMessageContaining("08h00").hasMessageContaining("20h00");
    }

    @Test
    void refuseUneSalleHorsService() {
        Room broken = Fixtures.vauban();
        broken.takeOutOfService("Fuite d'eau");
        assertThatThrownBy(() -> book(request(broken, CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 2)))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("Fuite d'eau")
                .extracting("code").isEqualTo("ROOM_OUT_OF_SERVICE");
    }

    @Test
    void respecteLaCapacite() {
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Comité", slot(TOMORROW, 10, 0, 11, 0), 7)), "OVER_CAPACITY");
        assertThat(book(request(vauban, CAMILLE_ID, "Comité", slot(TOMORROW, 10, 0, 11, 0), 6))).isNotNull();
    }

    @Test
    void refuseUnCreneauDejaPrisDansLaSalle() {
        Booking existing = book(request(vauban, HUGO_ID, "Rétro", slot(TOMORROW, 10, 0, 11, 0), 4));
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 30, 11, 30), 2),
                List.of(existing), List.of()), "SLOT_TAKEN");
    }

    @Test
    void accepteUneReunionQuiSuitLaPrecedente() {
        Booking existing = book(request(vauban, HUGO_ID, "Rétro", slot(TOMORROW, 10, 0, 11, 0), 4));
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 11, 0, 12, 0), 2), List.of(existing), List.of()))
                .isNotNull();
    }

    @Test
    void ignoreLesReservationsAnnulees() {
        Booking cancelled = book(request(vauban, HUGO_ID, "Rétro", slot(TOMORROW, 10, 0, 11, 0), 4));
        cancelled.cancel(HUGO_ID, false, null, NOW);
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 2), List.of(cancelled), List.of()))
                .isNotNull();
    }

    @Test
    void ignoreLesReservationsDAutresSalles() {
        Booking elsewhere = book(request(Fixtures.beffroi(), HUGO_ID, "Rétro", slot(TOMORROW, 10, 0, 11, 0), 4));
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 2), List.of(elsewhere), List.of()))
                .isNotNull();
    }

    @Test
    void lOrganisateurNePeutEtreDansDeuxSallesALaFois() {
        Booking mine = book(request(Fixtures.beffroi(), CAMILLE_ID, "Client", slot(TOMORROW, 10, 0, 11, 0), 4));
        assertViolation(() -> book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 30, 11, 0), 2),
                List.of(), List.of(mine)), "ORGANIZER_UNAVAILABLE");
    }

    @Test
    void uneReunionDUnCollegueNeBloquePasLOrganisateur() {
        Booking hugo = book(request(Fixtures.beffroi(), HUGO_ID, "Client", slot(TOMORROW, 10, 0, 11, 0), 4));
        assertThat(book(request(vauban, CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 2), List.of(), List.of(hugo)))
                .isNotNull();
    }

    @Test
    void laSalleDoitAppartenirAuSite() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BookingRequest(Fixtures.lille(), Fixtures.seine(),
                CAMILLE_ID, "Point", slot(TOMORROW, 10, 0, 11, 0), 2));
    }

    @Test
    void controleLeCreneauAvantLaSalle() {
        Room broken = Fixtures.vauban();
        broken.takeOutOfService("Travaux");
        assertViolation(() -> book(request(broken, CAMILLE_ID, "Point", slot(TOMORROW, 10, 5, 11, 0), 50)), "SLOT_NOT_ALIGNED");
    }
}
