package io.github.asmitmans.iotbackend.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AccountResolver {

    public Long resolveAccountId(UserPrincipal principal,
                                 Long requestedAccountId) {
        boolean isAdmin = principal.getAuthorities().stream()
                                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (requestedAccountId == null) {
                throw new IllegalArgumentException("Admin must provide " +
                                                           "accountId");
            }
            return requestedAccountId;
        }

        if (principal.getAccountId() == null) {
            throw new AccessDeniedException("User has no associated account");
        }

        return principal.getAccountId();
    }
}