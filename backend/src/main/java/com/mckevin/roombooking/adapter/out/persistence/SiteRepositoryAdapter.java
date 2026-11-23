package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.application.port.SiteRepository;
import com.mckevin.roombooking.domain.site.OpeningHours;
import com.mckevin.roombooking.domain.site.Site;
import com.mckevin.roombooking.domain.site.SiteId;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class SiteRepositoryAdapter implements SiteRepository {

    private final SiteJpaRepository sites;

    SiteRepositoryAdapter(SiteJpaRepository sites) {
        this.sites = sites;
    }

    @Override
    public List<Site> all() {
        return sites.findAll().stream().map(SiteRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Site> find(SiteId id) {
        return sites.findById(id.value()).map(SiteRepositoryAdapter::toDomain);
    }

    static Site toDomain(SiteEntity e) {
        var days = Arrays.stream(e.openDays.split(",")).map(String::strip).map(DayOfWeek::valueOf).collect(Collectors.toSet());
        return new Site(SiteId.of(e.id), e.name, e.city, new OpeningHours(e.opensAt, e.closesAt, days));
    }
}
