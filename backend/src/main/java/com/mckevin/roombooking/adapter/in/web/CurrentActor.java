package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.application.Actor;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Collaborateur à l'origine de la requête, reconstruit à partir du jeton JWT. */
@Component
class CurrentActor {

    Actor get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return new Actor(EmployeeId.of(Long.parseLong(jwt.getSubject())), jwt.getClaimAsString("name"),
                    "OFFICE_MANAGER".equals(jwt.getClaimAsString("role")));
        }
        throw new IllegalStateException("Aucun collaborateur authentifié");
    }
}
