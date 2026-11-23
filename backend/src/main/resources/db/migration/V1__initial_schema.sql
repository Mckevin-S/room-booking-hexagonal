-- Index GiST sur des colonnes scalaires (room_id) dans la contrainte d'exclusion
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE site (
    id          VARCHAR(10)  PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    opens_at    TIME         NOT NULL,
    closes_at   TIME         NOT NULL,
    open_days   VARCHAR(80)  NOT NULL,
    CONSTRAINT site_hours_valid CHECK (opens_at < closes_at)
);

CREATE TABLE room (
    id                     VARCHAR(20)  PRIMARY KEY,
    site_id                VARCHAR(10)  NOT NULL REFERENCES site (id),
    name                   VARCHAR(80)  NOT NULL,
    floor                  VARCHAR(40),
    seats                  INTEGER      NOT NULL CHECK (seats > 0),
    equipments             VARCHAR(200) NOT NULL DEFAULT '',
    out_of_service_reason  VARCHAR(200),
    CONSTRAINT room_name_unique UNIQUE (site_id, name)
);

CREATE TABLE app_user (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(120) NOT NULL UNIQUE,
    full_name      VARCHAR(120) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('EMPLOYEE', 'OFFICE_MANAGER')),
    home_site_id   VARCHAR(10)  NOT NULL REFERENCES site (id)
);

CREATE TABLE booking (
    id                   UUID         PRIMARY KEY,
    room_id              VARCHAR(20)  NOT NULL REFERENCES room (id),
    organizer_id         BIGINT       NOT NULL REFERENCES app_user (id),
    title                VARCHAR(80)  NOT NULL,
    starts_at            TIMESTAMP    NOT NULL,
    ends_at              TIMESTAMP    NOT NULL,
    attendees            INTEGER      NOT NULL CHECK (attendees > 0),
    status               VARCHAR(20)  NOT NULL CHECK (status IN ('CONFIRMED', 'CHECKED_IN', 'CANCELLED', 'RELEASED')),
    created_at           TIMESTAMP    NOT NULL,
    checked_in_at        TIMESTAMP,
    closed_at            TIMESTAMP,
    cancellation_reason  VARCHAR(300),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT booking_slot_valid CHECK (starts_at < ends_at),
    -- Dernier rempart contre la double réservation, y compris pour deux demandes simultanées :
    -- deux réservations actives d'une même salle ne peuvent pas se chevaucher ([début, fin[).
    CONSTRAINT booking_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        tsrange(starts_at, ends_at) WITH &&
    ) WHERE (status IN ('CONFIRMED', 'CHECKED_IN'))
);

CREATE INDEX booking_organizer_idx ON booking (organizer_id, starts_at);
CREATE INDEX booking_status_start_idx ON booking (status, starts_at);

CREATE TABLE notification (
    id            BIGSERIAL    PRIMARY KEY,
    recipient_id  BIGINT       NOT NULL REFERENCES app_user (id),
    message       VARCHAR(500) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    is_read       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX notification_recipient_idx ON notification (recipient_id, created_at DESC);
