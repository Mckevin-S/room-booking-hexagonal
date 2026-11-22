package com.mckevin.roombooking.domain.booking;

import com.mckevin.roombooking.domain.room.Equipment;
import java.util.EnumSet;
import java.util.Set;

public record SearchCriteria(TimeSlot slot, int attendees, Set<Equipment> equipments) {

    public SearchCriteria {
        if (attendees < 1) {
            throw new IllegalArgumentException("au moins un participant");
        }
        equipments = equipments == null || equipments.isEmpty() ? EnumSet.noneOf(Equipment.class) : EnumSet.copyOf(equipments);
    }
}
