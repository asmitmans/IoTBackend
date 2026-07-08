package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountResolver {

    private final AccountRepository accountRepository;

    public AccountResolver(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Resolves the internal account id to operate against.
     * Admins must supply requestedAccountId (public UUID); regular
     * users always resolve to their own account.
     */
    public Long resolveAccountId(UserPrincipal principal, UUID requestedAccountId) {
        boolean isAdmin = principal.getAuthorities().stream()
                                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (requestedAccountId == null) {
                throw new IllegalArgumentException("Admin must provide accountId");
            }
            return toInternalId(requestedAccountId);
        }

        if (principal.getAccountId() == null) {
            throw new AccessDeniedException("User has no associated account");
        }

        return toInternalId(principal.getAccountId());
    }

    /**
     * Resolves the principal's own internal account id, or null if the
     * principal has no associated account (e.g. admin acting without a
     * specific account context).
     */
    public Long resolveOwnAccountIdOrNull(UserPrincipal principal) {
        return principal.getAccountId() != null ? toInternalId(principal.getAccountId()) : null;
    }


    private Long toInternalId(UUID publicId) {
        return accountRepository.findByPublicId(publicId)
                                .map(Account::getId)
                                .orElseThrow(() -> new AccessDeniedException("Account not found"));
    }
}