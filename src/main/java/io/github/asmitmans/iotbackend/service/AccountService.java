package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.account.AccountMembershipResponse;
import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.AccountMembership;
import io.github.asmitmans.iotbackend.entity.AccountRole;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import io.github.asmitmans.iotbackend.security.JwtService;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMembershipRepository accountMembershipRepository;
    private final ApiKeyService apiKeyService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AccountMembershipRepository accountMembershipRepository,
                          ApiKeyService apiKeyService,
                          UserDetailsService userDetailsService,
                          JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMembershipRepository = accountMembershipRepository;
        this.apiKeyService = apiKeyService;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AccountRegistrationResponse create(String username, AccountRegistrationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccount() != null) {
            throw new ConflictException("User already belongs to an account");
        }

        String name = (request.getName() != null && !request.getName().isBlank())
                ? request.getName()
                : username + "'s Account";

        String apiKeyPlain = apiKeyService.generate("iotacc_");
        Account account = new Account(
                name,
                apiKeyService.hash(apiKeyPlain),
                apiKeyService.extractPrefix(apiKeyPlain)
        );
        account = accountRepository.save(account);

        user.setAccount(account);
        user.setAccountRole(AccountRole.OWNER);
        userRepository.save(user);

        accountMembershipRepository.save(new AccountMembership(user, account, AccountRole.OWNER));

        UserPrincipal refreshedPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        String token = jwtService.generateToken(refreshedPrincipal);

        return new AccountRegistrationResponse(account.getPublicId(), account.getName(), token);
    }

    @Transactional(readOnly = true)
    public List<AccountMembershipResponse> getMine(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return accountMembershipRepository.findByUserId(user.getId())
                .stream()
                .map(m -> new AccountMembershipResponse(
                        m.getAccount().getPublicId(),
                        m.getAccount().getName(),
                        m.getAccountRole()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountRegistrationResponse switchAccount(String username, UUID accountPublicId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByPublicId(accountPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        accountMembershipRepository.findByUserIdAndAccountId(user.getId(), account.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this account"));

        UserPrincipal principal = new UserPrincipal(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                account.getPublicId(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toSet())
        );

        String token = jwtService.generateToken(principal);

        return new AccountRegistrationResponse(account.getPublicId(), account.getName(), token);
    }
}
