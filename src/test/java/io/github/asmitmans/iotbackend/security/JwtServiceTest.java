package io.github.asmitmans.iotbackend.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha-512-signing-0123456789";
    private static final String OTHER_SECRET = "another-secret-key-that-is-long-enough-for-hmac-sha-512-signing-987654";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(new JwtProperties(SECRET, ONE_HOUR_MS));
    }

    // ---------- generateToken() + extract*() ----------

    @Test
    void generatedToken_carriesUsernameAccountIdAndAuthorities() {
        UUID accountId = UUID.randomUUID();
        String token = service.generateToken(principal("alice", accountId, "ROLE_USER", "ROLE_ADMIN"));

        assertThat(service.extractUsername(token)).isEqualTo("alice");
        assertThat(service.extractAccountId(token)).isEqualTo(accountId);
        assertThat(service.extractAuthorities(token)).isEqualTo("ROLE_USER,ROLE_ADMIN");
    }

    @Test
    void generatedToken_withoutAccount_hasNullAccountId() {
        String token = service.generateToken(principal("alice", null, "ROLE_USER"));

        assertThat(service.extractAccountId(token)).isNull();
    }

    // ---------- isValid() ----------

    @Test
    void isValid_withFreshToken_returnsTrue() {
        String token = service.generateToken(principal("alice", null, "ROLE_USER"));

        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() {
        JwtService expiringService = new JwtService(new JwtProperties(SECRET, -1_000L));
        String token = expiringService.generateToken(principal("alice", null, "ROLE_USER"));

        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void isValid_withTokenSignedByAnotherKey_returnsFalse() {
        JwtService otherService = new JwtService(new JwtProperties(OTHER_SECRET, ONE_HOUR_MS));
        String token = otherService.generateToken(principal("alice", null, "ROLE_ADMIN"));

        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void isValid_withTamperedPayload_returnsFalse() {
        String token = service.generateToken(principal("alice", null, "ROLE_USER"));
        String[] parts = token.split("\\.");
        String forgedPayload = service.generateToken(principal("alice", null, "ROLE_ADMIN")).split("\\.")[1];
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(service.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_withMalformedOrMissingToken_returnsFalse() {
        assertThat(service.isValid("not-a-jwt")).isFalse();
        assertThat(service.isValid("")).isFalse();
        assertThat(service.isValid(null)).isFalse();
    }

    @Test
    void extractUsername_withInvalidToken_throws() {
        assertThatThrownBy(() -> service.extractUsername("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    // ---------- helpers ----------

    private UserPrincipal principal(String username, UUID accountId, String... authorities) {
        return new UserPrincipal(username, "pw", true, accountId,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}