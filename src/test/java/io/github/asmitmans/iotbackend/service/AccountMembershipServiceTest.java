package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationResponse;
import io.github.asmitmans.iotbackend.dto.account.JoinCodeResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.AccountMembership;
import io.github.asmitmans.iotbackend.entity.AccountRole;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.exception.TooManyAttemptsException;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import io.github.asmitmans.iotbackend.security.JoinAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountMembershipServiceTest {

    private static final UUID ACCOUNT_PUBLIC_ID = UUID.randomUUID();
    private static final long ACCOUNT_ID = 10L;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMembershipRepository accountMembershipRepository;

    @Mock
    private JoinCodeService joinCodeService;

    @Mock
    private JoinAttemptService joinAttemptService;

    @Mock
    private ScopedTokenService scopedTokenService;

    private AccountMembershipService service;

    private Account account;

    @BeforeEach
    void setUp() {
        service = new AccountMembershipService(accountRepository, userRepository, accountMembershipRepository,
                joinCodeService, joinAttemptService, scopedTokenService);

        account = new Account("Finca");
        account.setId(ACCOUNT_ID);
        ReflectionTestUtils.setField(account, "publicId", ACCOUNT_PUBLIC_ID);
    }

    // ---------- switchAccount() ----------

    @Test
    void switchAccount_asMember_returnsATokenScopedToThatAccount() {
        AccountMembership membership = givenMembership("alice", 1, AccountRole.MEMBER);
        when(scopedTokenService.issue(membership.getUser(), account)).thenReturn("TOKEN");

        AccountRegistrationResponse response = service.switchAccount("alice", ACCOUNT_PUBLIC_ID);

        assertThat(response.getPublicId()).isEqualTo(ACCOUNT_PUBLIC_ID);
        assertThat(response.getName()).isEqualTo("Finca");
        assertThat(response.getToken()).isEqualTo("TOKEN");
    }

    @Test
    void switchAccount_notAMember_throwsAccessDeniedAndIssuesNoToken() {
        User alice = givenUser("alice", 1);
        givenAccount();
        when(accountMembershipRepository.findByUserIdAndAccountId(alice.getId(), ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.switchAccount("alice", ACCOUNT_PUBLIC_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a member of this account");

        verifyNoInteractions(scopedTokenService);
    }

    // ---------- generateJoinCode() ----------

    @Test
    void generateJoinCode_byOwner_storesHashPrefixAndExpiryAndReturnsThePlainCode() {
        givenMembership("alice", 1, AccountRole.OWNER);
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        when(joinCodeService.issue()).thenReturn(new JoinCodeIssuance("ABCD1234", "HASH", "ABCD", expiresAt));

        JoinCodeResponse response = service.generateJoinCode("alice", ACCOUNT_PUBLIC_ID);

        assertThat(account.getJoinCodeHash()).isEqualTo("HASH");
        assertThat(account.getJoinCodePrefix()).isEqualTo("ABCD");
        assertThat(account.getJoinCodeExpiresAt()).isEqualTo(expiresAt);
        verify(accountRepository).save(account);
        assertThat(response.getJoinCode()).isEqualTo("ABCD1234");
        assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void generateJoinCode_whenOneAlreadyExists_replacesIt() {
        givenMembership("alice", 1, AccountRole.OWNER);
        account.setJoinCodeHash("OLD_HASH");
        account.setJoinCodePrefix("OLDP");
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        when(joinCodeService.issue()).thenReturn(new JoinCodeIssuance("ABCD1234", "HASH", "ABCD", expiresAt));

        service.generateJoinCode("alice", ACCOUNT_PUBLIC_ID);

        assertThat(account.getJoinCodeHash()).isEqualTo("HASH");
        assertThat(account.getJoinCodePrefix()).isEqualTo("ABCD");
    }

    @Test
    void generateJoinCode_requesterIsNotOwner_throwsAccessDeniedAndIssuesNothing() {
        givenMembership("bob", 2, AccountRole.MEMBER);

        assertThatThrownBy(() -> service.generateJoinCode("bob", ACCOUNT_PUBLIC_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the account OWNER can generate a join code");

        verifyNoInteractions(joinCodeService);
        verify(accountRepository, never()).save(any());
    }

    // ---------- join() ----------

    @Test
    void join_withValidCode_addsAMemberAndReturnsAScopedToken() {
        User alice = givenUser("alice", 1);
        when(joinCodeService.resolveAndVerify("CODE")).thenReturn(account);
        when(accountMembershipRepository.existsByUserIdAndAccountId(1, ACCOUNT_ID)).thenReturn(false);
        when(scopedTokenService.issue(alice, account)).thenReturn("TOKEN");

        AccountRegistrationResponse response = service.join("alice", "CODE");

        ArgumentCaptor<AccountMembership> saved = ArgumentCaptor.forClass(AccountMembership.class);
        verify(accountMembershipRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(saved.getValue().getAccount()).isSameAs(account);
        assertThat(saved.getValue().getAccountRole()).isEqualTo(AccountRole.MEMBER);
        verify(joinAttemptService).recordSuccess("alice");
        verify(joinAttemptService, never()).recordFailure(any());
        assertThat(response.getPublicId()).isEqualTo(ACCOUNT_PUBLIC_ID);
        assertThat(response.getToken()).isEqualTo("TOKEN");
    }

    @Test
    void join_tooManyAttempts_throwsBeforeDoingAnythingElse() {
        doThrow(new TooManyAttemptsException("Too many failed join attempts. Try again in a minute."))
                .when(joinAttemptService).checkAllowed("alice");

        assertThatThrownBy(() -> service.join("alice", "CODE"))
                .isInstanceOf(TooManyAttemptsException.class);

        verifyNoInteractions(userRepository, joinCodeService);
    }

    @Test
    void join_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join("ghost", "CODE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(joinCodeService);
    }

    @Test
    void join_invalidCode_recordsAFailureAndRethrows() {
        givenUser("alice", 1);
        when(joinCodeService.resolveAndVerify("BAD")).thenThrow(new AccessDeniedException("Invalid join code"));

        assertThatThrownBy(() -> service.join("alice", "BAD"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid join code");

        verify(joinAttemptService).recordFailure("alice");
        verify(joinAttemptService, never()).recordSuccess(any());
        verify(accountMembershipRepository, never()).save(any());
    }

    @Test
    void join_expiredCode_recordsAFailureAndRethrows() {
        givenUser("alice", 1);
        when(joinCodeService.resolveAndVerify("OLD")).thenThrow(new ConflictException("Join code has expired"));

        assertThatThrownBy(() -> service.join("alice", "OLD"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Join code has expired");

        verify(joinAttemptService).recordFailure("alice");
        verify(accountMembershipRepository, never()).save(any());
    }

    @Test
    void join_alreadyAMember_throwsConflictWithoutAddingOrRecordingSuccess() {
        givenUser("alice", 1);
        when(joinCodeService.resolveAndVerify("CODE")).thenReturn(account);
        when(accountMembershipRepository.existsByUserIdAndAccountId(1, ACCOUNT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.join("alice", "CODE"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User is already a member of this account");

        verify(joinAttemptService, never()).recordSuccess(any());
        verify(accountMembershipRepository, never()).save(any());
    }

    // ---------- leave() ----------

    @Test
    void leave_asMember_deletesTheMembership() {
        AccountMembership membership = givenMembership("alice", 1, AccountRole.MEMBER);

        service.leave("alice", ACCOUNT_PUBLIC_ID);

        verify(accountMembershipRepository).delete(membership);
        verify(accountMembershipRepository, never()).countByAccountIdAndAccountRole(ACCOUNT_ID, AccountRole.OWNER);
    }

    @Test
    void leave_asOwner_deletesTheMembershipWhenAnotherOwnerRemains() {
        AccountMembership membership = givenMembership("alice", 1, AccountRole.OWNER);
        when(accountMembershipRepository.countByAccountIdAndAccountRole(ACCOUNT_ID, AccountRole.OWNER)).thenReturn(2L);

        service.leave("alice", ACCOUNT_PUBLIC_ID);

        verify(accountMembershipRepository).delete(membership);
    }

    @Test
    void leave_asOnlyOwner_throwsConflictAndKeepsTheMembership() {
        AccountMembership membership = givenMembership("alice", 1, AccountRole.OWNER);
        when(accountMembershipRepository.countByAccountIdAndAccountRole(ACCOUNT_ID, AccountRole.OWNER)).thenReturn(1L);

        assertThatThrownBy(() -> service.leave("alice", ACCOUNT_PUBLIC_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You are the only OWNER. Promote another member before leaving.");

        verify(accountMembershipRepository, never()).delete(membership);
    }

    @Test
    void leave_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leave("ghost", ACCOUNT_PUBLIC_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void leave_unknownAccount_throwsNotFound() {
        givenUser("alice", 1);
        when(accountRepository.findByPublicId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leave("alice", ACCOUNT_PUBLIC_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");
    }

    @Test
    void leave_notAMember_throwsAccessDenied() {
        User alice = givenUser("alice", 1);
        givenAccount();
        when(accountMembershipRepository.findByUserIdAndAccountId(alice.getId(), ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leave("alice", ACCOUNT_PUBLIC_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a member of this account");
    }

    // ---------- removeMember() ----------

    @Test
    void removeMember_byOwner_deletesTheTargetMembership() {
        givenMembership("alice", 1, AccountRole.OWNER);
        AccountMembership target = givenMembership("bob", 2, AccountRole.MEMBER);

        service.removeMember("alice", ACCOUNT_PUBLIC_ID, "bob");

        verify(accountMembershipRepository).delete(target);
    }

    @Test
    void removeMember_targetIsOwner_deletesItWhenAnotherOwnerRemains() {
        givenMembership("alice", 1, AccountRole.OWNER);
        AccountMembership target = givenMembership("bob", 2, AccountRole.OWNER);
        when(accountMembershipRepository.countByAccountIdAndAccountRole(ACCOUNT_ID, AccountRole.OWNER)).thenReturn(2L);

        service.removeMember("alice", ACCOUNT_PUBLIC_ID, "bob");

        verify(accountMembershipRepository).delete(target);
    }

    @Test
    void removeMember_targetIsTheOnlyOwner_throwsConflictAndKeepsTheMembership() {
        AccountMembership onlyOwner = givenMembership("alice", 1, AccountRole.OWNER);
        when(accountMembershipRepository.countByAccountIdAndAccountRole(ACCOUNT_ID, AccountRole.OWNER)).thenReturn(1L);

        assertThatThrownBy(() -> service.removeMember("alice", ACCOUNT_PUBLIC_ID, "alice"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot remove the only OWNER of an account");

        verify(accountMembershipRepository, never()).delete(onlyOwner);
    }

    @Test
    void removeMember_requesterIsNotOwner_throwsAccessDenied() {
        givenMembership("bob", 2, AccountRole.MEMBER);

        assertThatThrownBy(() -> service.removeMember("bob", ACCOUNT_PUBLIC_ID, "alice"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the account OWNER can remove members");

        verify(accountMembershipRepository, never()).delete(any());
    }

    @Test
    void removeMember_unknownTarget_throwsNotFound() {
        givenMembership("alice", 1, AccountRole.OWNER);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember("alice", ACCOUNT_PUBLIC_ID, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Target user not found");
    }

    @Test
    void removeMember_targetNotInTheAccount_throwsNotFound() {
        givenMembership("alice", 1, AccountRole.OWNER);
        User bob = givenUser("bob", 2);
        when(accountMembershipRepository.findByUserIdAndAccountId(bob.getId(), ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember("alice", ACCOUNT_PUBLIC_ID, "bob"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Target user is not a member of this account");
    }

    // ---------- promote() ----------

    @Test
    void promote_byOwner_setsTheTargetRoleToOwnerAndSavesIt() {
        givenMembership("alice", 1, AccountRole.OWNER);
        AccountMembership target = givenMembership("bob", 2, AccountRole.MEMBER);

        service.promote("alice", ACCOUNT_PUBLIC_ID, "bob");

        assertThat(target.getAccountRole()).isEqualTo(AccountRole.OWNER);
        verify(accountMembershipRepository).save(target);
    }

    @Test
    void promote_targetAlreadyOwner_throwsConflict() {
        givenMembership("alice", 1, AccountRole.OWNER);
        AccountMembership target = givenMembership("bob", 2, AccountRole.OWNER);

        assertThatThrownBy(() -> service.promote("alice", ACCOUNT_PUBLIC_ID, "bob"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User is already an OWNER of this account");

        verify(accountMembershipRepository, never()).save(target);
    }

    @Test
    void promote_requesterIsNotOwner_throwsAccessDenied() {
        givenMembership("bob", 2, AccountRole.MEMBER);

        assertThatThrownBy(() -> service.promote("bob", ACCOUNT_PUBLIC_ID, "carol"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the account OWNER can promote members");
    }

    @Test
    void promote_targetNotInTheAccount_throwsNotFound() {
        givenMembership("alice", 1, AccountRole.OWNER);
        User bob = givenUser("bob", 2);
        when(accountMembershipRepository.findByUserIdAndAccountId(bob.getId(), ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.promote("alice", ACCOUNT_PUBLIC_ID, "bob"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Target user is not a member of this account");
    }

    // ---------- helpers ----------

    private User givenUser(String username, int id) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        return user;
    }

    private void givenAccount() {
        when(accountRepository.findByPublicId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account));
    }

    // Stubs a user that is a member of the shared test account with the given role.
    // The account lookup is lenient because tests call this helper once per user (requester and target).
    private AccountMembership givenMembership(String username, int userId, AccountRole role) {
        User user = givenUser(username, userId);
        lenient().when(accountRepository.findByPublicId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account));
        AccountMembership membership = new AccountMembership(user, account, role);
        when(accountMembershipRepository.findByUserIdAndAccountId(userId, ACCOUNT_ID)).thenReturn(Optional.of(membership));
        return membership;
    }
}