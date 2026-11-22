package com.mckevin.roombooking.domain.room;

import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import com.mckevin.roombooking.domain.site.SiteId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Salle de réunion. Une salle hors service ne peut plus être réservée. */
public final class Room {

    private final RoomId id;
    private final SiteId siteId;
    private final String name;
    private final String floor;
    private final Capacity capacity;
    private final EnumSet<Equipment> equipments;
    private String outOfServiceReason;

    public Room(RoomId id, SiteId siteId, String name, String floor, Capacity capacity, Set<Equipment> equipments,
            String outOfServiceReason) {
        this.id = Objects.requireNonNull(id);
        this.siteId = Objects.requireNonNull(siteId);
        this.name = Objects.requireNonNull(name);
        this.floor = floor;
        this.capacity = Objects.requireNonNull(capacity);
        this.equipments = equipments.isEmpty() ? EnumSet.noneOf(Equipment.class) : EnumSet.copyOf(equipments);
        this.outOfServiceReason = outOfServiceReason;
    }

    public static Room inService(RoomId id, SiteId siteId, String name, String floor, int seats, Set<Equipment> equipments) {
        return new Room(id, siteId, name, floor, Capacity.of(seats), equipments, null);
    }

    public boolean offers(Set<Equipment> required) {
        return equipments.containsAll(required);
    }

    public boolean isInService() {
        return outOfServiceReason == null;
    }

    public void takeOutOfService(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleViolation("REASON_REQUIRED", "Indiquez pourquoi la salle est mise hors service.");
        }
        if (!isInService()) {
            throw new BusinessRuleViolation("ROOM_ALREADY_OUT_OF_SERVICE", "La salle " + name + " est déjà hors service.");
        }
        this.outOfServiceReason = reason.strip();
    }

    public void putBackInService() {
        if (isInService()) {
            throw new BusinessRuleViolation("ROOM_ALREADY_IN_SERVICE", "La salle " + name + " est déjà en service.");
        }
        this.outOfServiceReason = null;
    }

    public RoomId id() {
        return id;
    }

    public SiteId siteId() {
        return siteId;
    }

    public String name() {
        return name;
    }

    public String floor() {
        return floor;
    }

    public Capacity capacity() {
        return capacity;
    }

    public Set<Equipment> equipments() {
        return EnumSet.copyOf(equipments);
    }

    public String outOfServiceReason() {
        return outOfServiceReason;
    }
}
