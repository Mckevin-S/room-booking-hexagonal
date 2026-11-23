package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.site.SiteId;

/** Compte d'un collaborateur. */
public record Account(EmployeeId id, String email, String fullName, String passwordHash, boolean officeManager,
        SiteId homeSite) {
}
