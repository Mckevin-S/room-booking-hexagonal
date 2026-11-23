package com.mckevin.roombooking.domain.booking;

import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.site.Site;

/** Demande de réservation, avant contrôle des règles. */
public record BookingRequest(Site site, Room room, EmployeeId organizer, String title, TimeSlot slot, int attendees) {

    public BookingRequest {
        if (!room.siteId().equals(site.id())) {
            throw new IllegalArgumentException("la salle " + room.id().value() + " n'appartient pas au site " + site.id().value());
        }
    }
}
