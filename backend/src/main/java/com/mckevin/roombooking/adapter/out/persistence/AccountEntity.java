package com.mckevin.roombooking.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String email;
    @Column(name = "full_name")
    String fullName;
    @Column(name = "password_hash")
    String passwordHash;
    String role;
    @Column(name = "home_site_id")
    String homeSiteId;

    protected AccountEntity() {
    }

    AccountEntity(String email, String fullName, String passwordHash, String role, String homeSiteId) {
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.homeSiteId = homeSiteId;
    }
}
