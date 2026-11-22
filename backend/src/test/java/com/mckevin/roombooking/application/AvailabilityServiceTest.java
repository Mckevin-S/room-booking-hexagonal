package com.mckevin.roombooking.application;

import static com.mckevin.roombooking.support.Fixtures.CAMILLE;
import static com.mckevin.roombooking.support.Fixtures.HUGO;
import static com.mckevin.roombooking.support.Fixtures.SATURDAY;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.application.Views.PlannedBooking;
import com.mckevin.roombooking.application.Views.RoomPlanning;
import com.mckevin.roombooking.application.Views.RoomView;
import com.mckevin.roombooking.application.Views.SiteView;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.support.Agency;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {

    private final Agency agency = new Agency();

    private void book(Actor actor, String room, int from, int to, String title) {
        agency.booking.book(actor, new BookingCommand(room, TOMORROW, LocalTime.of(from, 0), LocalTime.of(to, 0), title, 2));
    }

    @Test
    void listeLesSitesParNom() {
        assertThat(agency.availability.sites()).extracting(SiteView::id).containsExactly("LIL", "PAR");
        SiteView lille = agency.availability.sites().get(0);
        assertThat(lille.name()).isEqualTo("Lille – Euratechnologies");
        assertThat(lille.city()).isEqualTo("Lille");
        assertThat(lille.opensAt()).isEqualTo(LocalTime.of(8, 0));
        assertThat(lille.closesAt()).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void chercheLesSallesLibresDuSite() {
        book(HUGO, "LIL-BEF", 10, 11, "Client");

        var result = agency.availability.search("LIL", TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0), 4,
                EnumSet.of(Equipment.SCREEN));

        assertThat(result.rooms()).extracting(RoomView::name).containsExactly("Vauban", "Grand-Place");
        RoomView vauban = result.rooms().get(0);
        assertThat(vauban.id()).isEqualTo("LIL-VAU");
        assertThat(vauban.siteId()).isEqualTo("LIL");
        assertThat(vauban.seats()).isEqualTo(6);
        assertThat(vauban.floor()).isEqualTo("3e étage");
        assertThat(vauban.inService()).isTrue();
        assertThat(vauban.equipments()).extracting(Views.EquipmentView::label).containsExactly("Écran", "Tableau blanc");
        assertThat(result.suggestions()).isEmpty();
    }

    @Test
    void proposeDesCreneauxQuandToutEstPris() {
        book(HUGO, "LIL-GPL", 8, 12, "Séminaire");

        var result = agency.availability.search("LIL", TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0), 10, Set.of());

        assertThat(result.rooms()).isEmpty();
        assertThat(result.suggestions()).first().satisfies(s -> {
            assertThat(s.room().name()).isEqualTo("Grand-Place");
            assertThat(s.start()).isEqualTo(LocalTime.of(12, 0));
            assertThat(s.end()).isEqualTo(LocalTime.of(13, 0));
        });
    }

    @Test
    void signaleUnSiteInconnu() {
        assertThatThrownBy(() -> agency.availability.search("XXX", TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0), 2,
                Set.of())).isInstanceOf(NotFoundException.class).hasMessageContaining("XXX");
    }

    @Test
    void presenteLePlanningDeLaJourneeParSalle() {
        book(CAMILLE, "LIL-VAU", 14, 15, "Après-midi");
        book(CAMILLE, "LIL-VAU", 9, 10, "Matin");
        book(HUGO, "LIL-BEF", 10, 11, "Client");

        var planning = agency.availability.planning(CAMILLE, "LIL", TOMORROW);

        assertThat(planning.open()).isTrue();
        assertThat(planning.date()).isEqualTo(TOMORROW);
        assertThat(planning.site().id()).isEqualTo("LIL");
        assertThat(planning.rooms()).extracting(r -> r.room().name())
                .containsExactly("Box Lambersart", "Vauban", "Beffroi", "Grand-Place");
        RoomPlanning vauban = planning.rooms().get(1);
        assertThat(vauban.bookings()).extracting(PlannedBooking::title).containsExactly("Matin", "Après-midi");
        assertThat(vauban.bookings().get(0).mine()).isTrue();
        assertThat(vauban.bookings().get(0).organizerName()).isEqualTo("Camille Martin");
        assertThat(vauban.bookings().get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(vauban.bookings().get(0).end()).isEqualTo(LocalTime.of(10, 0));
        assertThat(vauban.bookings().get(0).status()).isEqualTo("CONFIRMED");
        PlannedBooking client = planning.rooms().get(2).bookings().get(0);
        assertThat(client.mine()).isFalse();
        assertThat(client.organizerName()).isEqualTo("Hugo Petit");
    }

    @Test
    void indiqueQueLeSiteEstFermeLeWeekEnd() {
        assertThat(agency.availability.planning(CAMILLE, "LIL", SATURDAY).open()).isFalse();
    }
}
