package com.mckevin.roombooking.application;

import static com.mckevin.roombooking.support.Fixtures.CAMILLE;
import static com.mckevin.roombooking.support.Fixtures.CAMILLE_ID;
import static com.mckevin.roombooking.support.Fixtures.HUGO;
import static com.mckevin.roombooking.support.Fixtures.HUGO_ID;
import static com.mckevin.roombooking.support.Fixtures.NADIA;
import static com.mckevin.roombooking.support.Fixtures.TODAY;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.application.Views.RoomView;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.booking.BookingStatus;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Agency;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RoomServiceTest {

    private final Agency agency = new Agency();

    private String book(Actor actor, LocalDate date, int from, int to) {
        return agency.booking.book(actor, new BookingCommand("LIL-VAU", date, LocalTime.of(from, 0), LocalTime.of(to, 0),
                "Réunion", 2)).id();
    }

    @Test
    void listeLesSallesDuSiteParCapacite() {
        assertThat(agency.roomAdmin.rooms("LIL")).extracting(RoomView::name)
                .containsExactly("Box Lambersart", "Vauban", "Beffroi", "Grand-Place");
    }

    @Test
    void laMiseHorsServiceAnnuleLesReservationsAVenirEtPrevientLesOrganisateurs() {
        String past = book(CAMILLE, TODAY, 9, 10);
        String ongoing = book(HUGO, TODAY, 10, 12);
        String tomorrow = book(CAMILLE, TOMORROW, 10, 11);
        agency.clock.set(TODAY.atTime(11, 0));

        var result = agency.roomAdmin.takeOutOfService(NADIA, "LIL-VAU", "Dégât des eaux");

        assertThat(result.cancelledBookings()).isEqualTo(2);
        assertThat(result.room().inService()).isFalse();
        assertThat(result.room().outOfServiceReason()).isEqualTo("Dégât des eaux");
        assertThat(status(past)).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(status(ongoing)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(status(tomorrow)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(agency.notificationStore.of(CAMILLE_ID)).singleElement()
                .satisfies(n -> assertThat(n.message()).endsWith("Motif : Salle Vauban hors service : Dégât des eaux"));
        assertThat(agency.notificationStore.of(HUGO_ID)).hasSize(1);
        assertThatThrownBy(() -> book(CAMILLE, TOMORROW, 14, 15))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("ROOM_OUT_OF_SERVICE");
    }

    @Test
    void laRemiseEnServiceRendLaSalleReservable() {
        agency.roomAdmin.takeOutOfService(NADIA, "LIL-VAU", "Travaux");

        RoomView room = agency.roomAdmin.putBackInService(NADIA, "LIL-VAU");

        assertThat(room.inService()).isTrue();
        assertThat(book(CAMILLE, TOMORROW, 14, 15)).isNotBlank();
    }

    @Test
    void estReserveeAuxOfficeManagers() {
        assertThatThrownBy(() -> agency.roomAdmin.takeOutOfService(CAMILLE, "LIL-VAU", "Travaux"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> agency.roomAdmin.putBackInService(HUGO, "LIL-VAU"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(agency.rooms.find(com.mckevin.roombooking.domain.room.RoomId.of("LIL-VAU")).orElseThrow().isInService())
                .isTrue();
    }

    @Test
    void signaleUnSiteOuUneSalleInconnus() {
        assertThatThrownBy(() -> agency.roomAdmin.rooms("XXX")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> agency.roomAdmin.takeOutOfService(NADIA, "LIL-XXX", "Travaux"))
                .isInstanceOf(NotFoundException.class);
    }

    private BookingStatus status(String id) {
        return agency.bookings.find(BookingId.of(id)).orElseThrow().status();
    }
}
