package com.mckevin.roombooking.domain;

import static com.mckevin.roombooking.support.Fixtures.HUGO_ID;
import static com.mckevin.roombooking.support.Fixtures.NOW;
import static com.mckevin.roombooking.support.Fixtures.SATURDAY;
import static com.mckevin.roombooking.support.Fixtures.TODAY;
import static com.mckevin.roombooking.support.Fixtures.TOMORROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.mckevin.roombooking.domain.booking.AvailabilitySearch;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.booking.BookingStatus;
import com.mckevin.roombooking.domain.booking.SearchCriteria;
import com.mckevin.roombooking.domain.booking.TimeSlot;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Fixtures;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AvailabilitySearchTest {

    private final AvailabilitySearch search = new AvailabilitySearch();
    private final Room box = Fixtures.box();
    private final Room vauban = Fixtures.vauban();
    private final Room beffroi = Fixtures.beffroi();
    private final Room grandPlace = Fixtures.grandPlace();
    private final List<Room> rooms = List.of(grandPlace, box, Fixtures.seine(), beffroi, vauban);
    private final List<Booking> bookings = new ArrayList<>();

    private static TimeSlot slot(LocalDate date, int fromHour, int toHour) {
        return TimeSlot.of(date, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0));
    }

    /** Occupation existante, reconstituée telle quelle : elle peut dépasser les limites d'une nouvelle demande. */
    private void booked(Room room, TimeSlot slot) {
        bookings.add(new Booking(BookingId.newId(), room.id(), HUGO_ID, "Déjà réservée", slot, 1, NOW,
                BookingStatus.CONFIRMED, null, null, null));
    }

    private AvailabilitySearch.Result search(TimeSlot slot, int attendees, Set<Equipment> equipments) {
        return search.search(Fixtures.lille(), rooms, bookings, new SearchCriteria(slot, attendees, equipments), NOW);
    }

    @Test
    void proposeLesSallesDeLaMieuxAjusteeALaMoinsBienAjustee() {
        var result = search(slot(TOMORROW, 10, 11), 5, Set.of());

        assertThat(result.rooms()).extracting(Room::name).containsExactly("Vauban", "Beffroi", "Grand-Place");
        assertThat(result.suggestions()).isEmpty();
    }

    @Test
    void departageLesSallesDeMemeCapaciteParLeurNom() {
        Room lille = Room.inService(com.mckevin.roombooking.domain.room.RoomId.of("LIL-AAA"), Fixtures.LILLE, "Abbaye", "1er",
                6, EnumSet.noneOf(Equipment.class));
        var result = search.search(Fixtures.lille(), List.of(vauban, lille), List.of(),
                new SearchCriteria(slot(TOMORROW, 10, 11), 6, null), NOW);
        assertThat(result.rooms()).extracting(Room::name).containsExactly("Abbaye", "Vauban");
    }

    @Test
    void filtreSurLesEquipementsDemandes() {
        var result = search(slot(TOMORROW, 10, 11), 2, EnumSet.of(Equipment.VIDEO_CONFERENCE));
        assertThat(result.rooms()).extracting(Room::name).containsExactly("Box Lambersart", "Beffroi", "Grand-Place");

        var both = search(slot(TOMORROW, 10, 11), 2, EnumSet.of(Equipment.VIDEO_CONFERENCE, Equipment.WHITEBOARD));
        assertThat(both.rooms()).extracting(Room::name).containsExactly("Grand-Place");
    }

    @Test
    void ecarteLesSallesTropPetitesHorsServiceOuDUnAutreSite() {
        vauban.takeOutOfService("Vidéoprojecteur en panne");
        var result = search(slot(TOMORROW, 10, 11), 3, Set.of());
        assertThat(result.rooms()).extracting(Room::name).containsExactly("Beffroi", "Grand-Place");
    }

    @Test
    void ecarteLesSallesOccupeesSurLeCreneau() {
        booked(vauban, TimeSlot.of(TOMORROW, LocalTime.of(10, 30), LocalTime.of(11, 30)));
        booked(beffroi, slot(TOMORROW, 11, 12));
        var result = search(slot(TOMORROW, 10, 11), 5, Set.of());
        assertThat(result.rooms()).extracting(Room::name).containsExactly("Beffroi", "Grand-Place");
    }

    @Test
    void uneReservationAnnuleeLibereLaSalle() {
        booked(vauban, slot(TOMORROW, 10, 11));
        bookings.get(0).cancel(HUGO_ID, false, null, NOW);
        var result = search(slot(TOMORROW, 10, 11), 6, EnumSet.of(Equipment.WHITEBOARD));
        assertThat(result.rooms()).extracting(Room::name).containsExactly("Vauban", "Grand-Place");
    }

    @Test
    void proposeLesCreneauxLibresLesPlusProchesQuandToutEstPris() {
        booked(vauban, slot(TOMORROW, 10, 11));
        booked(grandPlace, slot(TOMORROW, 9, 12));

        var result = search(slot(TOMORROW, 10, 11), 6, EnumSet.of(Equipment.WHITEBOARD));

        assertThat(result.rooms()).isEmpty();
        assertThat(result.suggestions())
                .extracting(s -> s.room().name(), s -> s.slot())
                .containsExactly(
                        tuple("Vauban", slot(TOMORROW, 9, 10)),
                        tuple("Vauban", slot(TOMORROW, 11, 12)),
                        tuple("Vauban", TimeSlot.of(TOMORROW, LocalTime.of(8, 45), LocalTime.of(9, 45))));
    }

    @Test
    void lesSuggestionsDepartagentLesSallesParAjustement() {
        booked(vauban, slot(TOMORROW, 8, 13));
        booked(grandPlace, slot(TOMORROW, 8, 13));

        var result = search(slot(TOMORROW, 10, 11), 6, EnumSet.of(Equipment.WHITEBOARD));

        assertThat(result.suggestions())
                .extracting(s -> s.room().name(), s -> s.slot().start().toLocalTime())
                .containsExactly(
                        tuple("Vauban", LocalTime.of(13, 0)),
                        tuple("Grand-Place", LocalTime.of(13, 0)),
                        tuple("Vauban", LocalTime.of(13, 15)));
    }

    @Test
    void neProposeJamaisUnCreneauDejaCommence() {
        booked(vauban, slot(TODAY, 9, 12));
        booked(grandPlace, slot(TODAY, 9, 12));

        var result = search(slot(TODAY, 10, 11), 6, EnumSet.of(Equipment.WHITEBOARD));

        assertThat(result.suggestions()).isNotEmpty()
                .allSatisfy(s -> assertThat(s.slot().start()).isAfterOrEqualTo(NOW))
                .first().satisfies(s -> assertThat(s.slot()).isEqualTo(slot(TODAY, 12, 13)));
    }

    @Test
    void neProposeRienSiAucuneSalleNeConvient() {
        var result = search(slot(TOMORROW, 10, 11), 30, Set.of());
        assertThat(result.rooms()).isEmpty();
        assertThat(result.suggestions()).isEmpty();
    }

    @Test
    void limiteLesSuggestionsATrois() {
        booked(box, slot(TOMORROW, 10, 11));
        var result = search(slot(TOMORROW, 10, 11), 2, EnumSet.of(Equipment.VIDEO_CONFERENCE, Equipment.SCREEN,
                Equipment.WHITEBOARD, Equipment.ACCESSIBLE));
        assertThat(result.rooms()).extracting(Room::name).containsExactly("Grand-Place");
        booked(grandPlace, slot(TOMORROW, 8, 20));
        assertThat(search(slot(TOMORROW, 10, 11), 2, EnumSet.allOf(Equipment.class)).suggestions()).isEmpty();
        booked(beffroi, slot(TOMORROW, 10, 11));
        assertThat(search(slot(TOMORROW, 10, 11), 2, EnumSet.of(Equipment.VIDEO_CONFERENCE)).suggestions())
                .hasSize(AvailabilitySearch.MAX_SUGGESTIONS);
    }

    @Test
    void refuseUneRechercheSurUnSiteFerme() {
        assertThatThrownBy(() -> search(slot(SATURDAY, 10, 11), 2, Set.of()))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("SITE_CLOSED");
        assertThatThrownBy(() -> search(slot(TOMORROW, 19, 21), 2, Set.of()))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("SITE_CLOSED");
    }

    @Test
    void appliqueLesReglesDeCreneau() {
        assertThatThrownBy(() -> search(slot(TODAY, 8, 9), 2, Set.of()))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("SLOT_IN_PAST");
    }

    @Test
    void exigeAuMoinsUnParticipant() {
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new SearchCriteria(slot(TOMORROW, 10, 11), 0, Set.of()));
        assertThat(new SearchCriteria(slot(TOMORROW, 10, 11), 1, null).equipments()).isEmpty();
    }
}
