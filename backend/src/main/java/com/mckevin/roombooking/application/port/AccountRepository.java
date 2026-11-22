package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.domain.employee.EmployeeId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface AccountRepository {

    Optional<Account> byEmail(String email);

    Optional<Account> byId(EmployeeId id);

    /** Noms complets des collaborateurs, pour l'affichage des organisateurs. */
    Map<EmployeeId, String> names(Collection<EmployeeId> ids);
}
