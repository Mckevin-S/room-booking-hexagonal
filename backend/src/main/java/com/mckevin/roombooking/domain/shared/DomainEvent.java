package com.mckevin.roombooking.domain.shared;

import java.time.LocalDateTime;

/** Fait métier passé, publié après l'enregistrement de l'agrégat. */
public interface DomainEvent {

    LocalDateTime occurredAt();
}
