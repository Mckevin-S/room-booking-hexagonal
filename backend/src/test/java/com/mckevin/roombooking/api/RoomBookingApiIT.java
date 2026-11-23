package com.mckevin.roombooking.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.mckevin.roombooking.application.BookingService;
import com.mckevin.roombooking.support.MutableClock;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** API complète sur une vraie base PostgreSQL : sécurité, règles, contrainte d'exclusion, notifications. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {"app.demo.enabled=false", "app.no-show.check-interval=PT1H",
        "app.no-show.initial-delay=PT1H"})
class RoomBookingApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    /** Mardi prochain à 9 h : les règles horaires ne dépendent pas du jour où les tests tournent. */
    static final LocalDate TUESDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
    static final MutableClock CLOCK = new MutableClock(TUESDAY.atTime(9, 0));

    @TestConfiguration
    static class FixedClock {

        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }

    @Autowired
    TestRestTemplate http;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwords;
    @Autowired
    BookingService bookingService;

    @BeforeEach
    void resetData() {
        CLOCK.set(TUESDAY.atTime(9, 0));
        jdbc.execute("truncate notification, booking, app_user, room, site restart identity cascade");
        jdbc.update("insert into site values ('LIL', 'Lille – Euratechnologies', 'Lille', '08:00', '20:00', "
                + "'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY')");
        jdbc.update("insert into room (id, site_id, name, floor, seats, equipments) values "
                + "('LIL-VAU', 'LIL', 'Vauban', '3e étage', 6, 'SCREEN,WHITEBOARD'), "
                + "('LIL-BEF', 'LIL', 'Beffroi', '4e étage', 8, 'SCREEN,VIDEO_CONFERENCE')");
        String hash = passwords.encode("demo1234");
        jdbc.update("insert into app_user (email, full_name, password_hash, role, home_site_id) values "
                + "('camille@demo.fr', 'Camille Martin', ?, 'EMPLOYEE', 'LIL'), "
                + "('hugo@demo.fr', 'Hugo Petit', ?, 'EMPLOYEE', 'LIL'), "
                + "('nadia@demo.fr', 'Nadia Benali', ?, 'OFFICE_MANAGER', 'LIL')", hash, hash, hash);
    }

    // --- outils ---------------------------------------------------------------------------------------------

    private String login(String user) {
        ResponseEntity<JsonNode> response = http.postForEntity("/api/auth/login",
                Map.of("email", user + "@demo.fr", "password", "demo1234"), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("token").asText();
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String url, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> book(String token, String room, LocalDate date, String start, String end) {
        return call(HttpMethod.POST, "/api/bookings", token,
                Map.of("roomId", room, "date", date.toString(), "start", start, "end", end, "title", "Réunion", "attendees", 4));
    }

    // --- scénarios ------------------------------------------------------------------------------------------

    @Test
    void lAuthentificationEstObligatoire() {
        assertThat(call(HttpMethod.GET, "/api/sites", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ResponseEntity<JsonNode> refused = http.postForEntity("/api/auth/login",
                Map.of("email", "camille@demo.fr", "password", "faux"), JsonNode.class);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode me = call(HttpMethod.GET, "/api/auth/me", login("camille"), null).getBody();
        assertThat(me.get("fullName").asText()).isEqualTo("Camille Martin");
        assertThat(me.get("initials").asText()).isEqualTo("CM");
        assertThat(me.get("officeManager").asBoolean()).isFalse();
    }

    @Test
    void chercheReserveEtAfficheLePlanning() {
        String camille = login("camille");
        LocalDate wednesday = TUESDAY.plusDays(1);

        JsonNode search = call(HttpMethod.GET, "/api/sites/LIL/availability?date=" + wednesday
                + "&start=10:00&end=11:00&attendees=5&equipments=SCREEN", camille, null).getBody();
        assertThat(search.get("rooms")).extracting(r -> r.get("name").asText()).containsExactly("Vauban", "Beffroi");

        ResponseEntity<JsonNode> booked = book(camille, "LIL-VAU", wednesday, "10:00", "11:00");
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(booked.getBody().get("start").asText()).isEqualTo("10:00");
        assertThat(booked.getBody().get("status").asText()).isEqualTo("CONFIRMED");

        JsonNode planning = call(HttpMethod.GET, "/api/sites/LIL/planning?date=" + wednesday, login("hugo"), null).getBody();
        JsonNode vauban = planning.get("rooms").get(0);
        assertThat(vauban.get("room").get("name").asText()).isEqualTo("Vauban");
        assertThat(vauban.get("bookings").get(0).get("organizerName").asText()).isEqualTo("Camille Martin");
        assertThat(vauban.get("bookings").get(0).get("mine").asBoolean()).isFalse();

        JsonNode mine = call(HttpMethod.GET, "/api/bookings/mine", camille, null).getBody();
        assertThat(mine).hasSize(1);
    }

    @Test
    void refuseLesConflitsAvecUnCode() {
        book(login("hugo"), "LIL-VAU", TUESDAY, "10:00", "11:00");

        ResponseEntity<JsonNode> conflict = book(login("camille"), "LIL-VAU", TUESDAY, "10:30", "11:30");
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody().get("code").asText()).isEqualTo("SLOT_TAKEN");

        ResponseEntity<JsonNode> tooMany = call(HttpMethod.POST, "/api/bookings", login("camille"), Map.of("roomId", "LIL-BEF",
                "date", TUESDAY.toString(), "start", "14:00", "end", "15:00", "title", "Comité", "attendees", 9));
        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(tooMany.getBody().get("code").asText()).isEqualTo("OVER_CAPACITY");

        ResponseEntity<JsonNode> invalid = call(HttpMethod.POST, "/api/bookings", login("camille"), Map.of("roomId", "LIL-BEF"));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deuxDemandesSimultaneesNObtiennentPasLaMemeSalle() throws Exception {
        List<String> tokens = List.of(login("camille"), login("hugo"), login("nadia"));
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<ResponseEntity<JsonNode>>> attempts = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                String token = tokens.get(i % tokens.size());
                attempts.add(() -> book(token, "LIL-VAU", TUESDAY, "15:00", "16:00"));
            }
            List<HttpStatus> statuses = new ArrayList<>();
            for (Future<ResponseEntity<JsonNode>> result : pool.invokeAll(attempts)) {
                statuses.add(HttpStatus.valueOf(result.get().getStatusCode().value()));
            }
            assertThat(statuses).filteredOn(s -> s == HttpStatus.CREATED).hasSize(1);
            assertThat(statuses).filteredOn(s -> s == HttpStatus.CONFLICT).hasSize(5);
        } finally {
            pool.shutdown();
        }
        Integer active = jdbc.queryForObject(
                "select count(*) from booking where room_id = 'LIL-VAU' and status in ('CONFIRMED', 'CHECKED_IN')", Integer.class);
        assertThat(active).isEqualTo(1);
    }

    @Test
    void confirmeLaPresenceOuLibereLaSalle() {
        String camille = login("camille");
        String present = book(camille, "LIL-VAU", TUESDAY, "10:00", "11:00").getBody().get("id").asText();
        String absent = book(login("hugo"), "LIL-BEF", TUESDAY, "10:00", "11:00").getBody().get("id").asText();

        CLOCK.set(TUESDAY.atTime(9, 45));
        ResponseEntity<JsonNode> early = call(HttpMethod.POST, "/api/bookings/" + present + "/check-in", camille, null);
        assertThat(early.getBody().get("code").asText()).isEqualTo("CHECK_IN_WINDOW_CLOSED");

        CLOCK.set(TUESDAY.atTime(9, 55));
        ResponseEntity<JsonNode> checkedIn = call(HttpMethod.POST, "/api/bookings/" + present + "/check-in", camille, null);
        assertThat(checkedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkedIn.getBody().get("status").asText()).isEqualTo("CHECKED_IN");

        CLOCK.advance(Duration.ofMinutes(20));
        assertThat(bookingService.releaseNoShows()).isEqualTo(1);

        assertThat(jdbc.queryForObject("select status from booking where id = ?::uuid", String.class, absent)).isEqualTo("RELEASED");
        JsonNode notifications = call(HttpMethod.GET, "/api/notifications", login("hugo"), null).getBody();
        assertThat(notifications.get("unread").asInt()).isEqualTo(1);
        assertThat(notifications.get("items").get(0).get("message").asText()).contains("libérée faute de présence confirmée");

        assertThat(book(login("nadia"), "LIL-BEF", TUESDAY, "10:30", "11:00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void lOfficeManagerMetUneSalleHorsService() {
        String camille = login("camille");
        book(camille, "LIL-VAU", TUESDAY.plusDays(1), "10:00", "11:00");

        ResponseEntity<JsonNode> forbidden = call(HttpMethod.PUT, "/api/rooms/LIL-VAU/out-of-service", camille,
                Map.of("reason", "Je préfère une autre salle"));
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String nadia = login("nadia");
        JsonNode result = call(HttpMethod.PUT, "/api/rooms/LIL-VAU/out-of-service", nadia,
                Map.of("reason", "Dégât des eaux")).getBody();
        assertThat(result.get("cancelledBookings").asInt()).isEqualTo(1);
        assertThat(result.get("room").get("inService").asBoolean()).isFalse();

        JsonNode notifications = call(HttpMethod.GET, "/api/notifications", camille, null).getBody();
        assertThat(notifications.get("items").get(0).get("message").asText()).contains("Dégât des eaux");
        assertThat(call(HttpMethod.POST, "/api/notifications/read", camille, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(call(HttpMethod.GET, "/api/notifications", camille, null).getBody().get("unread").asInt()).isZero();

        assertThat(book(camille, "LIL-VAU", TUESDAY.plusDays(1), "14:00", "15:00").getBody().get("code").asText())
                .isEqualTo("ROOM_OUT_OF_SERVICE");
        assertThat(call(HttpMethod.DELETE, "/api/rooms/LIL-VAU/out-of-service", nadia, null).getBody().get("inService")
                .asBoolean()).isTrue();
    }

    @Test
    void lOrganisateurAnnuleEtLaSalleRedevientLibre() {
        String camille = login("camille");
        String id = book(camille, "LIL-VAU", TUESDAY, "14:00", "15:00").getBody().get("id").asText();

        assertThat(call(HttpMethod.POST, "/api/bookings/" + id + "/cancel", login("hugo"), Map.of()).getBody().get("code")
                .asText()).isEqualTo("NOT_ORGANIZER");
        JsonNode cancelled = call(HttpMethod.POST, "/api/bookings/" + id + "/cancel", camille, Map.of()).getBody();
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");

        assertThat(book(login("hugo"), "LIL-VAU", TUESDAY, "14:00", "15:00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LocalDateTime now = CLOCK.now();
        assertThat(now).isEqualTo(TUESDAY.atTime(9, 0));
    }
}
