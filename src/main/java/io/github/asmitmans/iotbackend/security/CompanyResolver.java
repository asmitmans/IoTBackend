package io.github.asmitmans.iotbackend.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CompanyResolver {

    public Long resolveCompanyId(UserPrincipal principal,
                                 Long requestedCompanyId) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (requestedCompanyId == null) {
                throw new IllegalArgumentException("Admin must provide " +
                                                           "companyId");
            }
            return requestedCompanyId;
        }

        if (principal.getCompanyId() == null) {
            throw new AccessDeniedException("User has no associated company");
        }

        return principal.getCompanyId();
    }
}
