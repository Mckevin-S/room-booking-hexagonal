package com.mckevin.roombooking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.domain.room.Capacity;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Fixtures;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void passeHorsServiceAvecUnMotifPuisRevientEnService() {
        Room room = Fixtures.vauban();
        assertThat(room.isInService()).isTrue();

        room.takeOutOfService("  Fuite d'eau au plafond ");
        assertThat(room.isInService()).isFalse();
        assertThat(room.outOfServiceReason()).isEqualTo("Fuite d'eau au plafond");

        room.putBackInService();
        assertThat(room.isInService()).isTrue();
        assertThat(room.outOfServiceReason()).isNull();
    }

    @Test
    void exigeUnMotifPourLaMiseHorsService() {
        Room room = Fixtures.vauban();
        assertThatThrownBy(() -> room.takeOutOfService(" ")).isInstanceOf(BusinessRuleViolation.class)
                .extracting("code").isEqualTo("REASON_REQUIRED");
        assertThatThrownBy(() -> room.takeOutOfService(null)).isInstanceOf(BusinessRuleViolation.class)
                .extracting("code").isEqualTo("REASON_REQUIRED");
    }

    @Test
    void refuseLesTransitionsInutiles() {
        Room room = Fixtures.vauban();
        assertThatThrownBy(room::putBackInService).extracting("code").isEqualTo("ROOM_ALREADY_IN_SERVICE");
        room.takeOutOfService("Travaux");
        assertThatThrownBy(() -> room.takeOutOfService("Encore")).extracting("code").isEqualTo("ROOM_ALREADY_OUT_OF_SERVICE");
        assertThat(room.outOfServiceReason()).isEqualTo("Travaux");
    }

    @Test
    void offreLesEquipementsDemandes() {
        Room room = Fixtures.vauban();
        assertThat(room.offers(Set.of())).isTrue();
        assertThat(room.offers(EnumSet.of(Equipment.SCREEN))).isTrue();
        assertThat(room.offers(EnumSet.of(Equipment.SCREEN, Equipment.WHITEBOARD))).isTrue();
        assertThat(room.offers(EnumSet.of(Equipment.SCREEN, Equipment.VIDEO_CONFERENCE))).isFalse();
    }

    @Test
    void protegeSaListeDEquipements() {
        Room room = Fixtures.vauban();
        room.equipments().clear();
        assertThat(room.equipments()).containsExactly(Equipment.SCREEN, Equipment.WHITEBOARD);
        Room bare = Room.inService(room.id(), room.siteId(), "Nue", null, 4, Set.of());
        assertThat(bare.equipments()).isEmpty();
        assertThat(bare.offers(Set.of())).isTrue();
    }

    @Test
    void exposeSesCaracteristiques() {
        Room room = Fixtures.vauban();
        assertThat(room.name()).isEqualTo("Vauban");
        assertThat(room.floor()).isEqualTo("3e étage");
        assertThat(room.siteId()).isEqualTo(Fixtures.LILLE);
        assertThat(room.capacity()).isEqualTo(Capacity.of(6));
        assertThat(Equipment.ACCESSIBLE.label()).isEqualTo("Accessible PMR");
    }

    @Test
    void laCapaciteAccueilleAuPlusSonNombreDePlaces() {
        Capacity six = Capacity.of(6);
        assertThat(six.fits(6)).isTrue();
        assertThat(six.fits(7)).isFalse();
        assertThat(six.unusedSeats(4)).isEqualTo(2);
        assertThat(Capacity.of(1).seats()).isEqualTo(1);
        assertThatIllegalArgumentException().isThrownBy(() -> Capacity.of(0));
    }
}
