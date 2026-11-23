package com.mckevin.roombooking.adapter.out.persistence.demo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Données de démonstration, datées par rapport au jour courant : deux sites, neuf salles, quatre collaborateurs et une
 * vingtaine de réservations (passées, du jour et des prochains jours ouvrés). Chargées une seule fois, sur une base vide.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String PASSWORD = "demo1234";
    private static final String WEEKDAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
    private static final LocalTime OPENS = LocalTime.of(8, 0);
    private static final LocalTime CLOSES = LocalTime.of(20, 0);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final Map<String, Long> users = new HashMap<>();

    DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder passwords, Clock clock) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer sites = jdbc.queryForObject("select count(*) from site", Integer.class);
        if (sites != null && sites > 0) {
            return;
        }
        site("LIL", "Lille – Euratechnologies", "Lille");
        site("PAR", "Paris – La Défense", "Puteaux");

        room("LIL-BOX", "LIL", "Box Lambersart", "3e étage", 2, "VIDEO_CONFERENCE");
        room("LIL-WAZ", "LIL", "Wazemmes", "3e étage", 4, "SCREEN");
        room("LIL-VAU", "LIL", "Vauban", "3e étage", 6, "SCREEN,WHITEBOARD");
        room("LIL-BEF", "LIL", "Beffroi", "4e étage", 8, "SCREEN,VIDEO_CONFERENCE");
        room("LIL-GPL", "LIL", "Grand-Place", "4e étage", 12, "SCREEN,VIDEO_CONFERENCE,WHITEBOARD,ACCESSIBLE");
        room("LIL-CIT", "LIL", "Citadelle", "Rez-de-chaussée", 20, "SCREEN,VIDEO_CONFERENCE,ACCESSIBLE");
        room("PAR-SEI", "PAR", "Seine", "12e étage", 6, "SCREEN");
        room("PAR-MON", "PAR", "Montmartre", "12e étage", 10, "SCREEN,VIDEO_CONFERENCE");
        room("PAR-OPE", "PAR", "Opéra", "14e étage", 16, "SCREEN,VIDEO_CONFERENCE,WHITEBOARD,ACCESSIBLE");

        user("mckevin@demo.fr", "Mckevin Kamguem", "EMPLOYEE", "LIL");
        user("julie@demo.fr", "Julie Dumont", "EMPLOYEE", "LIL");
        user("thomas@demo.fr", "Thomas Leroy", "EMPLOYEE", "PAR");
        user("nicolas@demo.fr", "Nicolas Lefebvre", "OFFICE_MANAGER", "LIL");

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = now.toLocalDate();
        LocalDate d1 = nextWorkingDay(today);
        LocalDate d2 = nextWorkingDay(d1);
        LocalDate p1 = previousWorkingDay(today);

        // Prochain jour ouvré : un planning bien rempli
        book("LIL-BOX", "julie", d1, "09:00", "09:30", "Entretien téléphonique", 1);
        book("LIL-WAZ", "nicolas", d1, "09:30", "10:30", "Point fournisseurs", 3);
        book("LIL-VAU", "mckevin", d1, "10:00", "11:00", "Revue de sprint", 6);
        book("LIL-BEF", "julie", d1, "10:00", "12:00", "Comité produit", 7);
        book("LIL-BOX", "mckevin", d1, "11:00", "11:30", "Visio recrutement", 2);
        book("LIL-VAU", "julie", d1, "13:30", "14:30", "Démonstration client", 4);
        book("LIL-GPL", "mckevin", d1, "14:00", "15:30", "Atelier conception API", 9);
        book("LIL-WAZ", "julie", d1, "15:00", "16:00", "Entretien annuel", 2);
        book("LIL-CIT", "nicolas", d1, "16:00", "18:00", "Réunion d'agence", 18);
        book("PAR-MON", "thomas", d1, "10:00", "11:00", "Point commercial", 8);
        book("PAR-SEI", "thomas", d1, "11:30", "12:30", "Revue de code", 4);
        // Jour ouvré suivant
        book("LIL-BOX", "mckevin", d2, "09:30", "10:00", "Daily élargi", 2);
        book("LIL-GPL", "nicolas", d2, "10:00", "12:00", "Formation sécurité", 10);
        book("LIL-BEF", "julie", d2, "14:00", "15:00", "Point RH", 3);
        book("PAR-OPE", "thomas", d2, "14:00", "16:00", "Séminaire commercial", 14);

        // Historique du dernier jour ouvré
        past("LIL-VAU", "mckevin", p1, "10:00", "11:00", "Rétrospective", 5, "CHECKED_IN", null);
        past("LIL-BEF", "mckevin", p1, "15:00", "16:00", "Point partenaire", 4, "RELEASED", null);
        past("LIL-GPL", "mckevin", p1, "09:00", "10:00", "Préparation salon", 6, "CANCELLED", "Visite de la direction");
        notify("mckevin", p1.atTime(9, 0).minusHours(16),
                "Votre réservation « Préparation salon » (salle Grand-Place) a été annulée. Motif : Visite de la direction");
        notify("mckevin", p1.atTime(15, 15),
                "La salle a été libérée faute de présence confirmée : « Point partenaire » (salle Beffroi).");

        // Aujourd'hui, pendant les heures d'ouverture : une réunion sur le point de commencer (présence à confirmer)
        // et une réunion commencée sans présence confirmée, que la tâche planifiée va libérer.
        if (isWorkingDay(today)) {
            LocalDateTime soon = ceilToQuarter(now.plusMinutes(5));
            if (!soon.toLocalTime().isBefore(OPENS) && !soon.plusMinutes(30).toLocalTime().isAfter(CLOSES)
                    && soon.toLocalDate().equals(today)) {
                book("LIL-WAZ", "mckevin", today, soon.toLocalTime(), soon.toLocalTime().plusMinutes(30), "Point d'équipe", 3);
            }
            LocalDateTime started = ceilToQuarter(now.minusMinutes(30));
            if (!started.toLocalTime().isBefore(OPENS) && !started.plusHours(1).toLocalTime().isAfter(CLOSES)
                    && started.toLocalDate().equals(today)) {
                book("LIL-VAU", "julie", today, started.toLocalTime(), started.toLocalTime().plusHours(1), "Brainstorming", 4);
            }
        }
        LOG.info("Données de démonstration chargées : 2 sites, 9 salles, 4 comptes (mot de passe : {})", PASSWORD);
    }

    private void site(String id, String name, String city) {
        jdbc.update("insert into site (id, name, city, opens_at, closes_at, open_days) values (?, ?, ?, ?, ?, ?)",
                id, name, city, OPENS, CLOSES, WEEKDAYS);
    }

    private void room(String id, String site, String name, String floor, int seats, String equipments) {
        jdbc.update("insert into room (id, site_id, name, floor, seats, equipments) values (?, ?, ?, ?, ?, ?)",
                id, site, name, floor, seats, equipments);
    }

    private void user(String email, String fullName, String role, String site) {
        Long id = jdbc.queryForObject("""
                insert into app_user (email, full_name, password_hash, role, home_site_id)
                values (?, ?, ?, ?, ?) returning id
                """, Long.class, email, fullName, passwords.encode(PASSWORD), role, site);
        users.put(email.substring(0, email.indexOf('@')), id);
    }

    private void book(String room, String who, LocalDate date, String from, String to, String title, int attendees) {
        book(room, who, date, LocalTime.parse(from), LocalTime.parse(to), title, attendees);
    }

    private void book(String room, String who, LocalDate date, LocalTime from, LocalTime to, String title, int attendees) {
        insert(room, who, date.atTime(from), date.atTime(to), title, attendees, "CONFIRMED", null,
                LocalDateTime.now(clock).minusDays(2).truncatedTo(ChronoUnit.MINUTES));
    }

    private void past(String room, String who, LocalDate date, String from, String to, String title, int attendees,
            String status, String reason) {
        LocalDateTime start = date.atTime(LocalTime.parse(from));
        insert(room, who, start, date.atTime(LocalTime.parse(to)), title, attendees, status, reason, start.minusDays(3));
    }

    private void insert(String room, String who, LocalDateTime start, LocalDateTime end, String title, int attendees,
            String status, String reason, LocalDateTime createdAt) {
        LocalDateTime checkedIn = "CHECKED_IN".equals(status) ? start.minusMinutes(5) : null;
        LocalDateTime closedAt = switch (status) {
            case "RELEASED" -> start.plus(Duration.ofMinutes(15));
            case "CANCELLED" -> start.minusHours(16);
            default -> null;
        };
        jdbc.update("""
                insert into booking (id, room_id, organizer_id, title, starts_at, ends_at, attendees, status, created_at,
                                     checked_in_at, closed_at, cancellation_reason)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), room, users.get(who), title, start, end, attendees, status, createdAt, checkedIn,
                closedAt, reason);
    }

    private void notify(String who, LocalDateTime at, String message) {
        jdbc.update("insert into notification (recipient_id, message, created_at, is_read) values (?, ?, ?, false)",
                users.get(who), message, at);
    }

    static boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    static LocalDate nextWorkingDay(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (!isWorkingDay(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    static LocalDate previousWorkingDay(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        while (!isWorkingDay(previous)) {
            previous = previous.minusDays(1);
        }
        return previous;
    }

    static LocalDateTime ceilToQuarter(LocalDateTime time) {
        LocalDateTime truncated = time.truncatedTo(ChronoUnit.MINUTES);
        int remainder = truncated.getMinute() % 15;
        return remainder == 0 ? truncated : truncated.plusMinutes(15L - remainder);
    }
}
