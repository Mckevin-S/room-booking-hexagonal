package com.mckevin.roombooking.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room")
class RoomEntity {

    @Id
    String id;
    @Column(name = "site_id")
    String siteId;
    String name;
    String floor;
    int seats;
    /** Codes d'équipement séparés par des virgules. */
    String equipments;
    @Column(name = "out_of_service_reason")
    String outOfServiceReason;

    protected RoomEntity() {
    }
}
