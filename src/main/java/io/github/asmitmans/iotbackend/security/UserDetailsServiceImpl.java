package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                             .map(user -> new UserPrincipal(
                                     user.getUsername(),
                                     user.getPassword(),
                                     user.isEnabled(),
                                     user.getAccount() != null ? user.getAccount().getId().longValue() : null,
                                     user.getRoles().stream()
                                         .map(role -> new SimpleGrantedAuthority(role.getName()))
                                         .collect(Collectors.toSet())
                             ))
                             .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}