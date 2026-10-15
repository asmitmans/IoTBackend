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
    private final JoinCodeService joinCodeService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final JoinAttemptService joinAttemptService;
    private final ScopedTokenService scopedTokenService;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AccountMembershipRepository accountMembershipRepository,
                          JoinCodeService joinCodeService,
                          UserDetailsService userDetailsService,
                          JwtService jwtService,
                          JoinAttemptService joinAttemptService, ScopedTokenService scopedTokenService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMembershipRepository = accountMembershipRepository;
        this.joinCodeService = joinCodeService;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.joinAttemptService = joinAttemptService;
        this.scopedTokenService = scopedTokenService;
    }

    @Transactional
    public AccountRegistrationResponse create(String username, AccountRegistrationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String name = (request.getName() != null && !request.getName().isBlank())
                ? request.getName()
                : username + "'s Account";

        Account account = new Account(name);
        account = accountRepository.save(account);

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
        MemberContext ctx = loadMemberContext(username, accountPublicId);

        return buildScopedResponse(ctx.user(), ctx.account());
    }

    @Transactional
    public JoinCodeResponse generateJoinCode(String username, UUID accountPublicId) {
        Account account = requireOwner(username, accountPublicId, "Only the account OWNER can generate a join code")
                .account();

        JoinCodeIssuance issuance = joinCodeService.issue();

        account.setJoinCodeHash(issuance.hash());
        account.setJoinCodePrefix(issuance.prefix());
        account.setJoinCodeExpiresAt(issuance.expiresAt());
        accountRepository.save(account);

        return new JoinCodeResponse(issuance.plainCode(), issuance.expiresAt());
    }

    @Transactional
    public AccountRegistrationResponse join(String username, String joinCode) {
        joinAttemptService.checkAllowed(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account;
        try {
            account = joinCodeService.resolveAndVerify(joinCode);
        } catch (AccessDeniedException | ConflictException e) {
            joinAttemptService.recordFailure(username);
            throw e;
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
        MemberContext ctx = loadMemberContext(username, accountPublicId);
        AccountMembership membership = ctx.membership();

        if (membership.getAccountRole() == AccountRole.OWNER) {
            long ownerCount = accountMembershipRepository.countByAccountIdAndAccountRole(ctx.account().getId(), AccountRole.OWNER);
            if (ownerCount <= 1) {
                throw new ConflictException("You are the only OWNER. Promote another member before leaving.");
            }
        }

        accountMembershipRepository.delete(membership);
    }

    @Transactional
    public void removeMember(String requesterUsername, UUID accountPublicId, String targetUsername) {
        Account account = requireOwner(requesterUsername, accountPublicId, "Only the account OWNER can remove members")
                .account();

        AccountMembership targetMembership = requireTargetMembership(targetUsername, account);

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
        Account account = requireOwner(requesterUsername, accountPublicId, "Only the account OWNER can promote members")
                .account();

        AccountMembership targetMembership = requireTargetMembership(targetUsername, account);

        if (targetMembership.getAccountRole() == AccountRole.OWNER) {
            throw new ConflictException("User is already an OWNER of this account");
        }

        targetMembership.setAccountRole(AccountRole.OWNER);
        accountMembershipRepository.save(targetMembership);
    }

    private record MemberContext(User user, Account account, AccountMembership membership) {
    }

    private MemberContext loadMemberContext(String username, UUID accountPublicId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByPublicId(accountPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountMembership membership = accountMembershipRepository.findByUserIdAndAccountId(user.getId(), account.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this account"));

        return new MemberContext(user, account, membership);
    }

    private MemberContext requireOwner(String username, UUID accountPublicId, String notOwnerMessage) {
        MemberContext ctx = loadMemberContext(username, accountPublicId);

        if (ctx.membership().getAccountRole() != AccountRole.OWNER) {
            throw new AccessDeniedException(notOwnerMessage);
        }

        return ctx;
    }

    private AccountMembership requireTargetMembership(String targetUsername, Account account) {
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        return accountMembershipRepository.findByUserIdAndAccountId(target.getId(), account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this account"));
    }

    private AccountRegistrationResponse buildScopedResponse(User user, Account account) {
        String token = scopedTokenService.issue(user, account);

        return new AccountRegistrationResponse(account.getPublicId(), account.getName(), token);
    }
}