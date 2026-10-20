package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.account.AccountMembershipResponse;
import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.AccountMembership;
import io.github.asmitmans.iotbackend.entity.AccountRole;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountMembershipRepository;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import io.github.asmitmans.iotbackend.security.JwtService;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMembershipRepository accountMembershipRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    private AccountService service;

    private User alice;

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository, userRepository, accountMembershipRepository,
                userDetailsService, jwtService);

        alice = new User();
        alice.setId(1);
        alice.setUsername("alice");
    }

    // ---------- create() ----------

    @Test
    void create_withName_savesTheAccountAndAnOwnerMembership() {
        givenAliceExists();
        givenSaveReturnsTheSameAccount();
        givenTokenForRefreshedPrincipal("TOKEN");

        AccountRegistrationResponse response = service.create("alice", requestNamed("Finca"));

        ArgumentCaptor<Account> savedAccount = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedAccount.capture());
        assertThat(savedAccount.getValue().getName()).isEqualTo("Finca");

        ArgumentCaptor<AccountMembership> savedMembership = ArgumentCaptor.forClass(AccountMembership.class);
        verify(accountMembershipRepository).save(savedMembership.capture());
        assertThat(savedMembership.getValue().getUser()).isSameAs(alice);
        assertThat(savedMembership.getValue().getAccount()).isSameAs(savedAccount.getValue());
        assertThat(savedMembership.getValue().getAccountRole()).isEqualTo(AccountRole.OWNER);

        assertThat(response.getName()).isEqualTo("Finca");
    }

    @Test
    void create_returnsATokenGeneratedFromTheRefreshedPrincipal() {
        givenAliceExists();
        givenSaveReturnsTheSameAccount();
        givenTokenForRefreshedPrincipal("TOKEN");

        AccountRegistrationResponse response = service.create("alice", requestNamed("Finca"));

        assertThat(response.getToken()).isEqualTo("TOKEN");
    }

    @Test
    void create_withoutName_usesTheDefaultName() {
        givenAliceExists();
        givenSaveReturnsTheSameAccount();
        givenTokenForRefreshedPrincipal("TOKEN");

        AccountRegistrationResponse response = service.create("alice", requestNamed(null));

        assertThat(response.getName()).isEqualTo("alice's Account");
    }

    @Test
    void create_withBlankName_usesTheDefaultName() {
        givenAliceExists();
        givenSaveReturnsTheSameAccount();
        givenTokenForRefreshedPrincipal("TOKEN");

        AccountRegistrationResponse response = service.create("alice", requestNamed("   "));

        assertThat(response.getName()).isEqualTo("alice's Account");
    }

    @Test
    void create_unknownUser_throwsNotFoundAndSavesNothing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("ghost", requestNamed("Finca")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(accountRepository, accountMembershipRepository, jwtService);
    }

    // ---------- getMine() ----------

    @Test
    void getMine_returnsOneEntryPerMembership() {
        givenAliceExists();
        UUID fincaId = UUID.randomUUID();
        UUID huertoId = UUID.randomUUID();
        when(accountMembershipRepository.findByUserId(1)).thenReturn(List.of(
                new AccountMembership(alice, accountNamed("Finca", fincaId), AccountRole.OWNER),
                new AccountMembership(alice, accountNamed("Huerto", huertoId), AccountRole.MEMBER)));

        List<AccountMembershipResponse> result = service.getMine("alice");

        assertThat(result)
                .extracting(AccountMembershipResponse::getPublicId,
                        AccountMembershipResponse::getName,
                        AccountMembershipResponse::getAccountRole)
                .containsExactly(
                        tuple(fincaId, "Finca", AccountRole.OWNER),
                        tuple(huertoId, "Huerto", AccountRole.MEMBER));
    }

    @Test
    void getMine_withoutMemberships_returnsAnEmptyList() {
        givenAliceExists();
        when(accountMembershipRepository.findByUserId(1)).thenReturn(List.of());

        assertThat(service.getMine("alice")).isEmpty();
    }

    @Test
    void getMine_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(accountMembershipRepository);
    }

    // ---------- helpers ----------

    private void givenAliceExists() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    private void givenSaveReturnsTheSameAccount() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenTokenForRefreshedPrincipal(String token) {
        UserPrincipal principal = new UserPrincipal("alice", "pw", true, null, List.of());
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn(token);
    }

    private AccountRegistrationRequest requestNamed(String name) {
        AccountRegistrationRequest request = new AccountRegistrationRequest();
        request.setName(name);
        return request;
    }

    private Account accountNamed(String name, UUID publicId) {
        Account account = new Account(name);
        ReflectionTestUtils.setField(account, "publicId", publicId);
        return account;
    }
}