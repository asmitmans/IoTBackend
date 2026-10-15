package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.security.JwtService;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ScopedTokenService {

    private final JwtService jwtService;

    public ScopedTokenService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Issues a JWT scoped to the given account. Must be called inside the
     * caller's transaction: user.getRoles() is lazy-loaded.
     */
    public String issue(User user, Account account) {
        UserPrincipal principal = new UserPrincipal(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                account.getPublicId(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toSet())
                );
        return jwtService.generateToken(principal);
    }
}
