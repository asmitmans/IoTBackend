package io.github.asmitmans.iotbackend.config;

import io.github.asmitmans.iotbackend.entity.AccountRole;
import io.github.asmitmans.iotbackend.entity.Role;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.RoleRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${seed.admin.password}")
  private String adminPassword;

  @Value("${seed.user.password}")
  private String userPassword;

  public DataInitializer(UserRepository userRepository,
                         RoleRepository roleRepository,
                         AccountRepository accountRepository,
                         PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    createUserIfNotExists("admin", adminPassword, "ROLE_ADMIN", AccountRole.OWNER);
    createUserIfNotExists("user", userPassword, "ROLE_USER", AccountRole.MEMBER);
  }


  private void createUserIfNotExists(String username,
                                     String rawPassword,
                                     String roleName,
                                     AccountRole accountRole) {
    if (userRepository.findByUsername(username).isPresent()) return;

    Role role = roleRepository.findByName(roleName)
                              .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));

    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(rawPassword));
    user.setEnabled(true);
    user.setAccount(accountRepository.findAll().getFirst());
    user.setAccountRole(accountRole);
    user.setRoles(Set.of(role));

    userRepository.save(user);
  }
}