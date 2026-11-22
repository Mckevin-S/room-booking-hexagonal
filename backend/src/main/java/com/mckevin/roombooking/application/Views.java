package com.mckevin.roombooking.application;

import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.site.Site;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Résultats des cas d'usage, indépendants du format d'exposition. */
public final class Views {

    private Views() {
    }

    public record SiteView(String id, String name, String city, LocalTime opensAt, LocalTime closesAt) {

        static SiteView of(Site site) {
            return new SiteView(site.id().value(), site.name(), site.city(), site.openingHours().opensAt(),
                    site.openingHours().closesAt());
        }
    }

    public record EquipmentView(String code, String label) {

        static EquipmentView of(Equipment equipment) {
            return new EquipmentView(equipment.name(), equipment.label());
        }
    }

    public record RoomView(String id, String siteId, String name, String floor, int seats, List<EquipmentView> equipments,
            boolean inService, String outOfServiceReason) {

        static RoomView of(Room room) {
            return new RoomView(room.id().value(), room.siteId().value(), room.name(), room.floor(),
                    room.capacity().seats(), room.equipments().stream().map(EquipmentView::of).toList(),
                    room.isInService(), room.outOfServiceReason());
        }
    }

    public record BookingView(String id, String roomId, String roomName, String siteName, String title, String organizerName,
            boolean mine, LocalDate date, LocalTime start, LocalTime end, int attendees, String status, String statusLabel,
            boolean canCheckIn, boolean canCancel, LocalDateTime checkInOpensAt, LocalDateTime releasedIfAbsentAt,
            String cancellationReason) {
    }

    public record SuggestionView(RoomView room, LocalTime start, LocalTime end) {
    }

    public record SearchView(List<RoomView> rooms, List<SuggestionView> suggestions) {
    }

    public record PlannedBooking(String id, String title, String organizerName, boolean mine, LocalTime start, LocalTime end,
            String status) {
    }

    public record RoomPlanning(RoomView room, List<PlannedBooking> bookings) {
    }

    public record PlanningView(SiteView site, LocalDate date, boolean open, List<RoomPlanning> rooms) {
    }

    public record OutOfServiceView(RoomView room, int cancelledBookings) {
    }

    public record NotificationView(long id, String message, LocalDateTime createdAt, boolean read) {
    }

    public record NotificationsView(long unread, List<NotificationView> items) {
    }
}
