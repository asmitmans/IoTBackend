package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.security.JoinCodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JoinCodeServiceTest {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private AccountRepository accountRepository;

    private JoinCodeService service;

    @BeforeEach
    void setUp() {
        service = new JoinCodeService(new JoinCodeProperties(ALPHABET, 8, 4, 24), apiKeyService, accountRepository);
    }

    // ---------- issue() ----------

    @Test
    void issue_generatesCodeWithConfiguredLengthAndAlphabet() {
        when(apiKeyService.hash(anyString())).thenReturn("HASH");

        JoinCodeIssuance issuance = service.issue();

        assertThat(issuance.plainCode()).matches("[" + ALPHABET + "]{8}");
    }

    @Test
    void issue_prefixIsTheFirstCharactersOfThePlainCode() {
        when(apiKeyService.hash(anyString())).thenReturn("HASH");

        JoinCodeIssuance issuance = service.issue();

        assertThat(issuance.prefix()).hasSize(4);
        assertThat(issuance.plainCode()).startsWith(issuance.prefix());
    }

    @Test
    void issue_hashesThePlainCodeAndReturnsThatHash() {
        when(apiKeyService.hash(anyString())).thenReturn("HASH");

        JoinCodeIssuance issuance = service.issue();

        verify(apiKeyService).hash(issuance.plainCode());
        assertThat(issuance.hash()).isEqualTo("HASH");
    }

    @Test
    void issue_expiresAfterTheConfiguredValidityHours() {
        when(apiKeyService.hash(anyString())).thenReturn("HASH");

        Instant before = Instant.now();
        JoinCodeIssuance issuance = service.issue();
        Instant after = Instant.now();

        assertThat(issuance.expiresAt()).isBetween(before.plus(24, ChronoUnit.HOURS), after.plus(24, ChronoUnit.HOURS));
    }

    @Test
    void issue_generatesDifferentCodesOnEachCall() {
        when(apiKeyService.hash(anyString())).thenReturn("HASH");

        String first = service.issue().plainCode();
        String second = service.issue().plainCode();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void issue_honorsCustomProperties() {
        JoinCodeService custom = new JoinCodeService(new JoinCodeProperties("AB", 6, 2, 1), apiKeyService, accountRepository);
        when(apiKeyService.hash(anyString())).thenReturn("HASH");

        Instant before = Instant.now();
        JoinCodeIssuance issuance = custom.issue();
        Instant after = Instant.now();

        assertThat(issuance.plainCode()).matches("[AB]{6}");
        assertThat(issuance.prefix()).hasSize(2);
        assertThat(issuance.expiresAt()).isBetween(before.plus(1, ChronoUnit.HOURS), after.plus(1, ChronoUnit.HOURS));
    }

    // ---------- resolveAndVerify() ----------

    @Test
    void resolveAndVerify_returnsTheAccountWhenPrefixHashAndExpirationAreValid() {
        Account account = accountWithJoinCode("HASH", Instant.now().plus(1, ChronoUnit.HOURS));
        when(accountRepository.findByJoinCodePrefix("ABCD")).thenReturn(List.of(account));
        when(apiKeyService.verify("ABCD1234", "HASH")).thenReturn(true);

        Account result = service.resolveAndVerify("ABCD1234");

        assertThat(result).isSameAs(account);
    }

    @Test
    void resolveAndVerify_throwsAccessDeniedWhenNoAccountHasThatPrefix() {
        when(accountRepository.findByJoinCodePrefix("ABCD")).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveAndVerify("ABCD1234"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid join code");
    }

    @Test
    void resolveAndVerify_throwsAccessDeniedWhenTheHashDoesNotMatch() {
        Account account = accountWithJoinCode("HASH", Instant.now().plus(1, ChronoUnit.HOURS));
        when(accountRepository.findByJoinCodePrefix("ABCD")).thenReturn(List.of(account));
        when(apiKeyService.verify("ABCD1234", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.resolveAndVerify("ABCD1234"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid join code");
    }

    @Test
    void resolveAndVerify_ignoresAccountsWithoutAStoredHash() {
        Account account = accountWithJoinCode(null, Instant.now().plus(1, ChronoUnit.HOURS));
        when(accountRepository.findByJoinCodePrefix("ABCD")).thenReturn(List.of(account));

        assertThatThrownBy(() -> service.resolveAndVerify("ABCD1234"))
                .isInstanceOf(AccessDeniedException.class);

        verify(apiKeyService, never()).verify(any(), any());
    }

    @Test
    void resolveAndVerify_picksTheMatchingAccountAmongPrefixCollisions() {
        Account other = accountWithJoinCode("OTHER_HASH", Instant.now().plus(1, ChronoUnit.HOURS));
        Account expected = accountWithJoinCode("HASH", Instant.now().plus(1, ChronoUnit.HOURS));
        when(accountRepository.findByJoinCodePrefix("ABCD")).thenReturn(List.of(other, expected));
        when(apiKeyService.verify("ABCD1234", "OTHER_HASH")).thenReturn(false);
        when(apiKeyService.verify("ABCD1234", "HASH")).thenReturn(true);

        Account result = service.resolveAndVerify("ABCD1234");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void resolveAndVerify_throwsConflictWhenTheCodeHasExpired() {
        Account account = accountWithJoinCode("HASH", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(accountRepository.findByJoinCodePrefix("ABCD")).thenReturn(List.of(account));
        when(apiKeyService.verify("ABCD1234", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.resolveAndVerify("ABCD1234"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Join code has expired");
    }

    @Test
    void resolveAndVerify_usesTheWholeCodeAsPrefixWhenItIsShorterThanThePrefixLength() {
        when(accountRepository.findByJoinCodePrefix("AB")).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveAndVerify("AB"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid join code");

        verify(accountRepository).findByJoinCodePrefix("AB");
    }

    private Account accountWithJoinCode(String hash, Instant expiresAt) {
        Account account = new Account("Finca");
        account.setJoinCodeHash(hash);
        account.setJoinCodeExpiresAt(expiresAt);
        return account;
    }
}
