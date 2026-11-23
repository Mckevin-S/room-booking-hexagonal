package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.domain.site.Site;
import com.mckevin.roombooking.domain.site.SiteId;
import java.util.List;
import java.util.Optional;

public interface SiteRepository {

    List<Site> all();

    Optional<Site> find(SiteId id);
}
