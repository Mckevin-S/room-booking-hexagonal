package com.mckevin.roombooking.application;

import com.mckevin.roombooking.application.port.BookingRepository;
import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.domain.booking.Booking;
import com.mckevin.roombooking.domain.booking.BookingId;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.site.Site;
import com.mckevin.roombooking.domain.site.SiteId;

/** Chargements avec message d'erreur lisible, partagés par les cas d'usage. */
final class Lookups {

    private Lookups() {
    }

    static Site site(SiteRepository sites, SiteId id) {
        return sites.find(id).orElseThrow(() -> new NotFoundException("Site inconnu : " + id.value()));
    }

    static Room room(RoomRepository rooms, RoomId id) {
        return rooms.find(id).orElseThrow(() -> new NotFoundException("Salle inconnue : " + id.value()));
    }

    static Booking booking(BookingRepository bookings, BookingId id) {
        return bookings.find(id).orElseThrow(() -> new NotFoundException("Réservation introuvable."));
    }
}
