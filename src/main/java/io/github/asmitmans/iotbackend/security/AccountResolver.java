package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountResolver {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMembershipRepository accountMembershipRepository;

    public AccountResolver(AccountRepository accountRepository, UserRepository userRepository, AccountMembershipRepository accountMembershipRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMembershipRepository = accountMembershipRepository;
    }

    /**
     * Resolves the internal account id to operate against.
     * Admins must supply requestedAccountId (public UUID); regular
     * users always resolve to their own account. Non-admin resolution
     * revalidates membership against the DB on every call — the JWT's
     * accountId claim is a hint from issuance time, not proof of current
     * access.
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

        Long accountId = toInternalId(principal.getAccountId());
        assertStillMember(principal.getUsername(), accountId);
        return accountId;
    }

    /**
     * Resolves the principal's own internal account id, or null if the
     * principal has no associated account. Also revalidated per request.
     */
    public Long resolveOwnAccountIdOrNull(UserPrincipal principal) {
        if (principal.getAccountId() == null) {
            return null;
        }
        Long accountId = toInternalId(principal.getAccountId());
        assertStillMember(principal.getUsername(), accountId);
        return accountId;
    }

    private void assertStillMember(String username, Long accountId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (!accountMembershipRepository.existsByUserIdAndAccountId(user.getId(), accountId)) {
            throw new AccessDeniedException("User is no longer a member of this account");
        }
    }

    private Long toInternalId(UUID publicId) {
        return accountRepository.findByPublicId(publicId)
                .map(Account::getId)
                .orElseThrow(() -> new AccessDeniedException("Account not found"));
    }
}