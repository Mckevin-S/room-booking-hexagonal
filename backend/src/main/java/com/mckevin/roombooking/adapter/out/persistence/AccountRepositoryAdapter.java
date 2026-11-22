package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.application.port.Account;
import com.mckevin.roombooking.application.port.AccountRepository;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import com.mckevin.roombooking.domain.site.SiteId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class AccountRepositoryAdapter implements AccountRepository {

    static final String OFFICE_MANAGER = "OFFICE_MANAGER";

    private final AccountJpaRepository accounts;

    AccountRepositoryAdapter(AccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<Account> byEmail(String email) {
        return accounts.findByEmail(email).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Account> byId(EmployeeId id) {
        return accounts.findById(id.value()).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Map<EmployeeId, String> names(Collection<EmployeeId> ids) {
        return accounts.findAllById(ids.stream().map(EmployeeId::value).toList()).stream()
                .collect(Collectors.toMap(a -> EmployeeId.of(a.id), a -> a.fullName));
    }

    private static Account toDomain(AccountEntity e) {
        return new Account(EmployeeId.of(e.id), e.email, e.fullName, e.passwordHash, OFFICE_MANAGER.equals(e.role),
                SiteId.of(e.homeSiteId));
    }
}
