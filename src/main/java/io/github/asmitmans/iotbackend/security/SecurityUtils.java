package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getAccountId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccount() == null) {
            throw new ResourceNotFoundException("User has no associated account");
        }
        return user.getAccount().getId().longValue();
    }
}