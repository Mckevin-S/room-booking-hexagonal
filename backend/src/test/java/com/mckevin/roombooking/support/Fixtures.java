package com.mckevin.roombooking.support;

import com.mckevin.roombooking.application.Actor;
import com.mckevin.roombooking.application.port.Account;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.site.OpeningHours;
import com.mckevin.roombooking.domain.site.Site;
import com.mckevin.roombooking.domain.site.SiteId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/** Jeu de données de référence : le site de Lille, ses salles et trois collaborateurs. */
public final class Fixtures {

    /** Mardi 1er décembre 2026, 9 h. */
    public static final LocalDate TODAY = LocalDate.of(2026, 12, 1);
    public static final LocalDateTime NOW = TODAY.atTime(9, 0);
    public static final LocalDate TOMORROW = TODAY.plusDays(1);
    /** Samedi 5 décembre 2026. */
    public static final LocalDate SATURDAY = LocalDate.of(2026, 12, 5);

    public static final SiteId LILLE = SiteId.of("LIL");
    public static final SiteId PARIS = SiteId.of("PAR");

    public static final EmployeeId CAMILLE_ID = EmployeeId.of(1);
    public static final EmployeeId HUGO_ID = EmployeeId.of(2);
    public static final EmployeeId NADIA_ID = EmployeeId.of(3);

    public static final Actor CAMILLE = new Actor(CAMILLE_ID, "Camille Martin", false);
    public static final Actor HUGO = new Actor(HUGO_ID, "Hugo Petit", false);
    public static final Actor NADIA = new Actor(NADIA_ID, "Nadia Benali", true);

    private Fixtures() {
    }

    public static Site lille() {
        return new Site(LILLE, "Lille – Euratechnologies", "Lille", OpeningHours.weekdays(LocalTime.of(8, 0), LocalTime.of(20, 0)));
    }

    public static Site paris() {
        return new Site(PARIS, "Paris – La Défense", "Puteaux", OpeningHours.weekdays(LocalTime.of(8, 0), LocalTime.of(20, 0)));
    }

    /** Box de 2 places, visio. */
    public static Room box() {
        return Room.inService(RoomId.of("LIL-BOX"), LILLE, "Box Lambersart", "3e étage", 2, EnumSet.of(Equipment.VIDEO_CONFERENCE));
    }

    /** 6 places, écran et tableau blanc. */
    public static Room vauban() {
        return Room.inService(RoomId.of("LIL-VAU"), LILLE, "Vauban", "3e étage", 6, EnumSet.of(Equipment.SCREEN, Equipment.WHITEBOARD));
    }

    /** 8 places, écran et visio. */
    public static Room beffroi() {
        return Room.inService(RoomId.of("LIL-BEF"), LILLE, "Beffroi", "4e étage", 8, EnumSet.of(Equipment.SCREEN, Equipment.VIDEO_CONFERENCE));
    }

    /** 12 places, tout équipée. */
    public static Room grandPlace() {
        return Room.inService(RoomId.of("LIL-GPL"), LILLE, "Grand-Place", "4e étage", 12, EnumSet.allOf(Equipment.class));
    }

    public static Room seine() {
        return Room.inService(RoomId.of("PAR-SEI"), PARIS, "Seine", "12e étage", 6, Set.of(Equipment.SCREEN));
    }

    public static Account account(Actor actor) {
        String email = actor.fullName().split(" ")[0].toLowerCase() + "@demo.fr";
        return new Account(actor.id(), email, actor.fullName(), "{noop}demo1234", actor.officeManager(), LILLE);
    }
}
