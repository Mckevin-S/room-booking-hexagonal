package com.mckevin.roombooking.domain.site;

import java.util.Objects;

public record SiteId(String value) {

    public SiteId {
        Objects.requireNonNull(value, "identifiant de site requis");
    }

    public static SiteId of(String value) {
        return new SiteId(value);
    }
}
