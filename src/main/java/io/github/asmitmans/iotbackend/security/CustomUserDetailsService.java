package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                             .map(user -> new org.springframework.security.core.userdetails.User(
                                     user.getUsername(),
                                     user.getPassword(),
                                     user.isEnabled(),
                                     true, true, true,
                                     user.getRoles().stream()
                                         .map(role -> new SimpleGrantedAuthority(role.getName()))
                                         .collect(Collectors.toSet())
                             ))
                             .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}