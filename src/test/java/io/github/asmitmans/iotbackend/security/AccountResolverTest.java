package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountResolverTest {

    private static final UUID ACCOUNT_PUBLIC_ID = UUID.randomUUID();
    private static final Long ACCOUNT_ID = 10L;
    private static final Integer USER_ID = 7;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMembershipRepository accountMembershipRepository;

    private AccountResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AccountResolver(accountRepository, userRepository, accountMembershipRepository);
    }

    // ---------- resolveAccountId(): admin ----------

    @Test
    void resolveAccountId_asAdminWithRequestedAccount_returnsItsInternalIdWithoutMembershipCheck() {
        givenAccountExists();

        Long result = resolver.resolveAccountId(admin(), ACCOUNT_PUBLIC_ID);

        assertThat(result).isEqualTo(ACCOUNT_ID);
        verifyNoInteractions(userRepository, accountMembershipRepository);
    }

    @Test
    void resolveAccountId_asAdminWithoutRequestedAccount_throwsIllegalArgument() {
        assertThatThrownBy(() -> resolver.resolveAccountId(admin(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin must provide accountId");

        verifyNoInteractions(accountRepository);
    }

    @Test
    void resolveAccountId_asAdminWithUnknownAccount_throwsAccessDenied() {
        when(accountRepository.findByPublicId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveAccountId(admin(), ACCOUNT_PUBLIC_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Account not found");
    }

    // ---------- resolveAccountId(): user ----------

    @Test
    void resolveAccountId_asUserStillMember_returnsTheAccountFromTheToken() {
        givenAccountExists();
        givenUserExists();
        when(accountMembershipRepository.existsByUserIdAndAccountId(USER_ID, ACCOUNT_ID)).thenReturn(true);

        Long result = resolver.resolveAccountId(user(ACCOUNT_PUBLIC_ID), null);

        assertThat(result).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void resolveAccountId_asUser_ignoresTheRequestedAccountAndUsesTheToken() {
        givenAccountExists();
        givenUserExists();
        when(accountMembershipRepository.existsByUserIdAndAccountId(USER_ID, ACCOUNT_ID)).thenReturn(true);

        Long result = resolver.resolveAccountId(user(ACCOUNT_PUBLIC_ID), UUID.randomUUID());

        assertThat(result).isEqualTo(ACCOUNT_ID);
        verify(accountRepository).findByPublicId(ACCOUNT_PUBLIC_ID);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void resolveAccountId_asUserNoLongerMember_throwsAccessDeniedEvenWithAValidToken() {
        givenAccountExists();
        givenUserExists();
        when(accountMembershipRepository.existsByUserIdAndAccountId(USER_ID, ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolveAccountId(user(ACCOUNT_PUBLIC_ID), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is no longer a member of this account");
    }

    @Test
    void resolveAccountId_asUserWithoutAccountInToken_throwsAccessDenied() {
        assertThatThrownBy(() -> resolver.resolveAccountId(user(null), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User has no associated account");

        verifyNoInteractions(accountRepository, userRepository, accountMembershipRepository);
    }

    @Test
    void resolveAccountId_asUserWhoseAccountWasDeleted_throwsAccessDenied() {
        when(accountRepository.findByPublicId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveAccountId(user(ACCOUNT_PUBLIC_ID), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Account not found");
    }

    @Test
    void resolveAccountId_asUserThatWasDeleted_throwsAccessDenied() {
        givenAccountExists();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveAccountId(user(ACCOUNT_PUBLIC_ID), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User not found");
    }

    // ---------- resolveOwnAccountIdOrNull() ----------

    @Test
    void resolveOwnAccountIdOrNull_withoutAccountInToken_returnsNull() {
        assertThat(resolver.resolveOwnAccountIdOrNull(user(null))).isNull();

        verifyNoInteractions(accountRepository, userRepository, accountMembershipRepository);
    }

    @Test
    void resolveOwnAccountIdOrNull_whenStillMember_returnsTheInternalId() {
        givenAccountExists();
        givenUserExists();
        when(accountMembershipRepository.existsByUserIdAndAccountId(USER_ID, ACCOUNT_ID)).thenReturn(true);

        assertThat(resolver.resolveOwnAccountIdOrNull(user(ACCOUNT_PUBLIC_ID))).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void resolveOwnAccountIdOrNull_whenNoLongerMember_throwsAccessDenied() {
        givenAccountExists();
        givenUserExists();
        when(accountMembershipRepository.existsByUserIdAndAccountId(USER_ID, ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolveOwnAccountIdOrNull(user(ACCOUNT_PUBLIC_ID)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is no longer a member of this account");
    }

    // ---------- helpers ----------

    private void givenAccountExists() {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        when(accountRepository.findByPublicId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account));
    }

    private void givenUserExists() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    private UserPrincipal admin() {
        return new UserPrincipal("root", "pw", true, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UserPrincipal user(UUID accountId) {
        return new UserPrincipal("alice", "pw", true, accountId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}