package com.mckevin.roombooking.application;

import com.mckevin.roombooking.domain.employee.EmployeeId;

/** Collaborateur authentifié à l'origine d'un cas d'usage. */
public record Actor(EmployeeId id, String fullName, boolean officeManager) {
}
