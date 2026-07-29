package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.entity.AccountMembership;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountMembershipRepository accountMembershipRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, AccountMembershipRepository accountMembershipRepository) {
        this.userRepository = userRepository;
        this.accountMembershipRepository = accountMembershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        UUID activeAccountId = resolveActiveAccountId(user.getId());

        return new UserPrincipal(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                activeAccountId,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toSet())
        );
    }

    private UUID resolveActiveAccountId(Integer userId) {
        List<AccountMembership> memberships = accountMembershipRepository.findByUserId(userId);

        if (memberships.size() == 1) {
            return memberships.get(0).getAccount().getPublicId();
        }

        return null;
    }
}