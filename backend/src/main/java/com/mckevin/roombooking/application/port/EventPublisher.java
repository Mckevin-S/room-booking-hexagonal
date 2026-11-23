package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.domain.shared.DomainEvent;
import java.util.List;

/** Publie les événements du domaine une fois l'agrégat enregistré. */
public interface EventPublisher {

    void publish(List<DomainEvent> events);
}
