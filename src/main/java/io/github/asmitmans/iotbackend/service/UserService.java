package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.user.UserRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.user.UserRegistrationResponse;
import io.github.asmitmans.iotbackend.entity.Role;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.RoleRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already taken");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                                         .orElseThrow(() -> new ResourceNotFoundException("Role not found: ROLE_USER"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setAlias(request.getAlias());
        user.setNames(request.getNames());
        user.setSurnames(request.getSurnames());
        user.setRoles(Set.of(defaultRole));

        userRepository.save(user);

        return new UserRegistrationResponse(user.getUsername());
    }
}