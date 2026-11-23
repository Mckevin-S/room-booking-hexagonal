package com.mckevin.roombooking.adapter.out.event;

import com.mckevin.roombooking.application.port.EventPublisher;
import com.mckevin.roombooking.domain.shared.DomainEvent;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Relaie les événements du domaine sur le bus d'événements de Spring, dans la transaction en cours. */
@Component
class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(publisher::publishEvent);
    }
}
