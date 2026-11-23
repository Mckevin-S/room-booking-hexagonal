package com.mckevin.roombooking.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.mckevin.roombooking.application.Actor;
import com.mckevin.roombooking.application.BookingCommand;
import com.mckevin.roombooking.application.ForbiddenException;
import com.mckevin.roombooking.application.Views.BookingView;
import com.mckevin.roombooking.application.Views.OutOfServiceView;
import com.mckevin.roombooking.application.Views.RoomView;
import com.mckevin.roombooking.application.Views.SearchView;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.support.Agency;
import com.mckevin.roombooking.support.Fixtures;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ParameterType;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Soit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Étapes des scénarios : chaque scénario dispose d'une agence neuve, assemblée sur des adaptateurs en mémoire. */
public class RoomBookingSteps {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("H:mm");

    private final Agency agency = new Agency();
    private final Map<String, BookingView> lastBookingOf = new HashMap<>();
    private SearchView search;
    private BookingView lastBooking;
    private OutOfServiceView outOfService;
    private RuntimeException failure;

    @ParameterType("Camille|Hugo|Nadia")
    public Actor acteur(String name) {
        return switch (name) {
            case "Camille" -> Fixtures.CAMILLE;
            case "Hugo" -> Fixtures.HUGO;
            default -> Fixtures.NADIA;
        };
    }

    @ParameterType("demain|aujourd'hui|samedi|dans \\d+ jours")
    public LocalDate jour(String day) {
        LocalDate today = agency.clock.now().toLocalDate();
        return switch (day) {
            case "demain" -> today.plusDays(1);
            case "aujourd'hui" -> today;
            case "samedi" -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
            default -> today.plusDays(Long.parseLong(day.replaceAll("\\D", "")));
        };
    }

    @ParameterType("\\d{1,2}:\\d{2}")
    public LocalTime heure(String time) {
        return LocalTime.parse(time, HOUR);
    }

    @Soit("nous sommes le mardi 1er décembre 2026 à {heure}")
    public void nousSommesLe(LocalTime time) {
        agency.clock.set(Fixtures.TODAY.atTime(time));
    }

    @Soit("il est {heure}")
    public void ilEst(LocalTime time) {
        agency.clock.set(agency.clock.now().toLocalDate().atTime(time));
    }

    @Soit("{string} est réservée {jour} de {heure} à {heure} par {acteur}")
    public void estReservee(String room, LocalDate date, LocalTime from, LocalTime to, Actor actor) {
        BookingView view = agency.booking.book(actor, new BookingCommand(roomId(room), date, from, to, "Réunion", 1));
        lastBookingOf.put(actor.fullName(), view);
    }

    @Quand("{acteur} cherche une salle {jour} de {heure} à {heure} pour {int} personne(s)")
    public void cherche(Actor actor, LocalDate date, LocalTime from, LocalTime to, int attendees) {
        search = agency.availability.search("LIL", date, from, to, attendees, Set.of());
    }

    @Quand("{acteur} cherche une salle {jour} de {heure} à {heure} pour {int} personnes avec {string}")
    public void chercheAvec(Actor actor, LocalDate date, LocalTime from, LocalTime to, int attendees, String equipments) {
        Set<Equipment> wanted = EnumSet.noneOf(Equipment.class);
        for (String label : equipments.split(",\\s*")) {
            wanted.add(Arrays.stream(Equipment.values()).filter(e -> e.label().equals(label)).findFirst().orElseThrow());
        }
        search = agency.availability.search("LIL", date, from, to, attendees, wanted);
    }

    @Alors("les salles proposées sont {string}")
    public void lesSallesProposees(String names) {
        assertThat(search.rooms().stream().map(RoomView::name).collect(Collectors.joining(", "))).isEqualTo(names);
    }

    @Alors("aucune salle n'est proposée")
    public void aucuneSalle() {
        assertThat(search.rooms()).isEmpty();
    }

    @Alors("les créneaux suggérés sont :")
    public void lesCreneauxSuggeres(DataTable table) {
        List<List<String>> expected = table.asMaps().stream()
                .map(row -> List.of(row.get("salle"), row.get("début"), row.get("fin")))
                .toList();
        List<List<String>> actual = search.suggestions().stream()
                .map(s -> List.of(s.room().name(), s.start().toString(), s.end().toString()))
                .toList();
        assertThat(actual).isEqualTo(expected);
    }

    @Quand("{acteur} réserve {string} {jour} de {heure} à {heure}")
    public void reserve(Actor actor, String room, LocalDate date, LocalTime from, LocalTime to) {
        reserve(actor, room, date, from, to, 2);
    }

    @Quand("{acteur} réserve {string} {jour} de {heure} à {heure} pour {int} personnes")
    public void reserve(Actor actor, String room, LocalDate date, LocalTime from, LocalTime to, int attendees) {
        attempt(() -> {
            lastBooking = agency.booking.book(actor, new BookingCommand(roomId(room), date, from, to, "Réunion", attendees));
            lastBookingOf.put(actor.fullName(), lastBooking);
            return lastBooking;
        });
    }

    @Alors("la réservation est confirmée")
    public void confirmee() {
        assertThat(failure).isNull();
        assertThat(lastBooking.statusLabel()).isEqualTo("Confirmée");
    }

    @Alors("la réservation est refusée : {string}")
    @Alors("l'action est refusée : {string}")
    public void refusee(String code) {
        assertThat(failure).isInstanceOf(BusinessRuleViolation.class);
        assertThat(((BusinessRuleViolation) failure).code()).isEqualTo(code);
    }

    @Alors("l'action est interdite")
    public void interdite() {
        assertThat(failure).isInstanceOf(ForbiddenException.class);
    }

    @Quand("{acteur} confirme sa présence")
    public void confirmeSaPresence(Actor actor) {
        attempt(() -> agency.booking.checkIn(actor, idOf(actor)));
    }

    @Quand("les salles inoccupées sont libérées")
    public void libereLesSalles() {
        agency.booking.releaseNoShows();
    }

    @Alors("la réservation de {acteur} est {string}")
    public void statutDeLaReservation(Actor actor, String label) {
        String actual = agency.bookings.find(idOf(actor)).orElseThrow().status().label();
        assertThat(actual).isEqualTo(label);
    }

    @Alors("{acteur} est prévenu(e) : {string}")
    public void estPrevenu(Actor actor, String excerpt) {
        assertThat(agency.notifications.of(actor).items()).anySatisfy(n -> assertThat(n.message()).contains(excerpt));
    }

    @Alors("{acteur} peut réserver {string} {jour} de {heure} à {heure}")
    public void peutReserver(Actor actor, String room, LocalDate date, LocalTime from, LocalTime to) {
        reserve(actor, room, date, from, to);
        confirmee();
    }

    @Alors("{acteur} ne peut pas réserver {string} {jour} de {heure} à {heure} : {string}")
    public void nePeutPasReserver(Actor actor, String room, LocalDate date, LocalTime from, LocalTime to, String code) {
        reserve(actor, room, date, from, to);
        refusee(code);
    }

    @Quand("{acteur} met la salle {string} hors service pour {string}")
    public void horsService(Actor actor, String room, String reason) {
        attempt(() -> outOfService = agency.roomAdmin.takeOutOfService(actor, roomId(room), reason));
    }

    @Alors("{int} réservation(s) est/sont annulée(s)")
    public void reservationsAnnulees(int count) {
        assertThat(failure).isNull();
        assertThat(outOfService.cancelledBookings()).isEqualTo(count);
    }

    @Quand("{acteur} annule la réservation de {acteur} pour {string}")
    public void annule(Actor actor, Actor organizer, String reason) {
        attempt(() -> agency.booking.cancel(actor, idOf(organizer), reason));
    }

    private BookingId idOf(Actor actor) {
        return BookingId.of(lastBookingOf.get(actor.fullName()).id());
    }

    private String roomId(String name) {
        return agency.roomAdmin.rooms("LIL").stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow().id();
    }

    private void attempt(Supplier<?> action) {
        failure = null;
        try {
            action.get();
        } catch (BusinessRuleViolation | ForbiddenException e) {
            failure = e;
        }
    }
}
