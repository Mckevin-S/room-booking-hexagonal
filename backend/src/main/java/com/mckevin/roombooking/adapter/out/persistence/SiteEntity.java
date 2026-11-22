package com.mckevin.roombooking.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "site")
class SiteEntity {

    @Id
    String id;
    String name;
    String city;
    @Column(name = "opens_at")
    LocalTime opensAt;
    @Column(name = "closes_at")
    LocalTime closesAt;
    @Column(name = "open_days")
    String openDays;

    protected SiteEntity() {
    }

    SiteEntity(String id, String name, String city, LocalTime opensAt, LocalTime closesAt, String openDays) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.openDays = openDays;
    }
}
