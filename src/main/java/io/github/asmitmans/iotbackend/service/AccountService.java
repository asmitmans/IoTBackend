package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.account.*;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.AccountMembership;
import io.github.asmitmans.iotbackend.entity.AccountRole;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import io.github.asmitmans.iotbackend.security.JoinAttemptService;
import io.github.asmitmans.iotbackend.security.JwtService;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 8;
    private static final int JOIN_CODE_PREFIX_LENGTH = 4;
    private static final long JOIN_CODE_VALIDITY_HOURS = 24;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMembershipRepository accountMembershipRepository;
    private final ApiKeyService apiKeyService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final JoinAttemptService joinAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AccountMembershipRepository accountMembershipRepository,
                          ApiKeyService apiKeyService,
                          UserDetailsService userDetailsService,
                          JwtService jwtService,
                          JoinAttemptService joinAttemptService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMembershipRepository = accountMembershipRepository;
        this.apiKeyService = apiKeyService;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.joinAttemptService = joinAttemptService;
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

        return buildScopedResponse(user, account);
    }

    @Transactional
    public JoinCodeResponse generateJoinCode(String username, UUID accountPublicId) {
        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByPublicId(accountPublicId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountMembership membership = accountMembershipRepository.findByUserIdAndAccountId(user.getId(), account.getId())
                                                                  .orElseThrow(() -> new AccessDeniedException("User is not a member of this account"));

        if (membership.getAccountRole() != AccountRole.OWNER) {
            throw new AccessDeniedException("Only the account OWNER can generate a join code");
        }

        String plainCode = generateCode();
        Instant expiresAt = Instant.now().plus(JOIN_CODE_VALIDITY_HOURS, ChronoUnit.HOURS);

        account.setJoinCodeHash(apiKeyService.hash(plainCode));
        account.setJoinCodePrefix(plainCode.substring(0, JOIN_CODE_PREFIX_LENGTH));
        account.setJoinCodeExpiresAt(expiresAt);
        accountRepository.save(account);

        return new JoinCodeResponse(plainCode, expiresAt);
    }

    @Transactional
    public AccountRegistrationResponse join(String username, String joinCode) {
        joinAttemptService.checkAllowed(username);

        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String prefix = joinCode.length() >= JOIN_CODE_PREFIX_LENGTH
                ? joinCode.substring(0, JOIN_CODE_PREFIX_LENGTH)
                : joinCode;

        Account account = accountRepository.findByJoinCodePrefix(prefix).stream()
                                           .filter(a -> a.getJoinCodeHash() != null)
                                           .filter(a -> apiKeyService.verify(joinCode, a.getJoinCodeHash()))
                                           .findFirst()
                                           .orElseGet(() -> {
                                               joinAttemptService.recordFailure(username);
                                               throw new AccessDeniedException("Invalid join code");
                                           });

        if (Instant.now().isAfter(account.getJoinCodeExpiresAt())) {
            joinAttemptService.recordFailure(username);
            throw new ConflictException("Join code has expired");
        }

        if (accountMembershipRepository.existsByUserIdAndAccountId(user.getId(), account.getId())) {
            throw new ConflictException("User is already a member of this account");
        }

        joinAttemptService.recordSuccess(username);
        accountMembershipRepository.save(new AccountMembership(user, account, AccountRole.MEMBER));

        return buildScopedResponse(user, account);
    }

    @Transactional
    public void leave(String username, UUID accountPublicId) {
        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByPublicId(accountPublicId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountMembership membership = accountMembershipRepository.findByUserIdAndAccountId(user.getId(), account.getId())
                                                                  .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this account"));

        if (membership.getAccountRole() == AccountRole.OWNER) {
            long ownerCount = accountMembershipRepository.countByAccountIdAndAccountRole(account.getId(), AccountRole.OWNER);
            if (ownerCount <= 1) {
                throw new ConflictException("You are the only OWNER. Promote another member before leaving.");
            }
        }

        accountMembershipRepository.delete(membership);
    }

    @Transactional
    public void removeMember(String requesterUsername, UUID accountPublicId, String targetUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                                       .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByPublicId(accountPublicId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountMembership requesterMembership = accountMembershipRepository
                .findByUserIdAndAccountId(requester.getId(), account.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this account"));

        if (requesterMembership.getAccountRole() != AccountRole.OWNER) {
            throw new AccessDeniedException("Only the account OWNER can remove members");
        }

        User target = userRepository.findByUsername(targetUsername)
                                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        AccountMembership targetMembership = accountMembershipRepository
                .findByUserIdAndAccountId(target.getId(), account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this account"));

        if (targetMembership.getAccountRole() == AccountRole.OWNER) {
            long ownerCount = accountMembershipRepository.countByAccountIdAndAccountRole(account.getId(), AccountRole.OWNER);
            if (ownerCount <= 1) {
                throw new ConflictException("Cannot remove the only OWNER of an account");
            }
        }

        accountMembershipRepository.delete(targetMembership);
    }

    @Transactional
    public void promote(String requesterUsername, UUID accountPublicId, String targetUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                                       .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByPublicId(accountPublicId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountMembership requesterMembership = accountMembershipRepository
                .findByUserIdAndAccountId(requester.getId(), account.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this account"));

        if (requesterMembership.getAccountRole() != AccountRole.OWNER) {
            throw new AccessDeniedException("Only the account OWNER can promote members");
        }

        User target = userRepository.findByUsername(targetUsername)
                                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        AccountMembership targetMembership = accountMembershipRepository
                .findByUserIdAndAccountId(target.getId(), account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this account"));

        if (targetMembership.getAccountRole() == AccountRole.OWNER) {
            throw new ConflictException("User is already an OWNER of this account");
        }

        targetMembership.setAccountRole(AccountRole.OWNER);
        accountMembershipRepository.save(targetMembership);
    }

    private AccountRegistrationResponse buildScopedResponse(User user, Account account) {
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

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            sb.append(JOIN_CODE_ALPHABET.charAt(secureRandom.nextInt(JOIN_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}