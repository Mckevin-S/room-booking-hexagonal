package com.mckevin.roombooking.domain.site;

import java.util.Objects;

/** Site de l'entreprise (immeuble) qui regroupe des salles. */
public record Site(SiteId id, String name, String city, OpeningHours openingHours) {

    public Site {
        Objects.requireNonNull(id);
        Objects.requireNonNull(openingHours);
    }
}
