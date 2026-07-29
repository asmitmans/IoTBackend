package io.github.asmitmans.iotbackend.service;

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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}