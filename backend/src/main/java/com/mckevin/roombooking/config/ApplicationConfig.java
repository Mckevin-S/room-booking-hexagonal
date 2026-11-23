package com.mckevin.roombooking.config;

import com.mckevin.roombooking.application.AuthenticationService;
import com.mckevin.roombooking.application.AvailabilityService;
import com.mckevin.roombooking.application.BookingService;
import com.mckevin.roombooking.application.RoomService;
import com.mckevin.roombooking.application.notification.NotificationService;
import com.mckevin.roombooking.application.port.AccountRepository;
import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.application.port.EventPublisher;
import com.mckevin.roombooking.application.port.NotificationRepository;
import com.mckevin.roombooking.application.port.PasswordVerifier;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.application.port.TokenIssuer;
import com.mckevin.roombooking.domain.booking.AvailabilitySearch;
import com.mckevin.roombooking.domain.booking.BookingPolicy;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Assemblage de l'hexagone : les cas d'usage ne sont pas des composants Spring,
 * ils sont construits ici à partir des adaptateurs qui implémentent leurs ports.
 */
@Configuration
@EnableScheduling
public class ApplicationConfig {

    /** Toutes les règles horaires s'appliquent à l'heure locale des sites. */
    @Bean
    public Clock clock(@Value("${app.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public BookingPolicy bookingPolicy() {
        return new BookingPolicy();
    }

    @Bean
    public AvailabilitySearch availabilitySearch() {
        return new AvailabilitySearch();
    }

    @Bean
    public BookingService bookingService(Clock clock, BookingPolicy policy, SiteRepository sites, RoomRepository rooms,
            BookingRepository bookings, AccountRepository accounts, EventPublisher events) {
        return new BookingService(clock, policy, sites, rooms, bookings, accounts, events);
    }

    @Bean
    public AvailabilityService availabilityService(Clock clock, AvailabilitySearch search, SiteRepository sites,
            RoomRepository rooms, BookingRepository bookings, AccountRepository accounts) {
        return new AvailabilityService(clock, search, sites, rooms, bookings, accounts);
    }

    @Bean
    public RoomService roomService(Clock clock, SiteRepository sites, RoomRepository rooms, BookingRepository bookings,
            EventPublisher events) {
        return new RoomService(clock, sites, rooms, bookings, events);
    }

    @Bean
    public NotificationService notificationService(NotificationRepository notifications, RoomRepository rooms) {
        return new NotificationService(notifications, rooms);
    }

    @Bean
    public AuthenticationService authenticationService(AccountRepository accounts, PasswordVerifier passwords,
            TokenIssuer tokens) {
        return new AuthenticationService(accounts, passwords, tokens);
    }
}
