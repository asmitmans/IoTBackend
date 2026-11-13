package io.github.asmitmans.iotbackend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthFlowIT extends AbstractIntegrationTest {

    // ---------- registration ----------

    @Test
    void register_withDuplicateUsername_returns409() {
        String username = registerUser("alice");

        assertThat(postJson("/api/v1/users", null, Map.of("username", username, "password", PASSWORD)))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void register_withTooShortPassword_returns400() {
        assertThat(postJson("/api/v1/users", null, Map.of("username", uniqueUsername("alice"), "password", "short")))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    // ---------- login: active account policy (0 / 1 / 2+) ----------

    @Test
    void login_withoutAccounts_returnsTokenWithNoActiveAccount() {
        String username = registerUser("alice");

        JsonNode response = login(username);

        assertThat(response.get("token").asText()).isNotBlank();
        assertThat(response.get("username").asText()).isEqualTo(username);
        assertThat(response.get("accountId").isNull()).isTrue();
    }

    @Test
    void login_withOneAccount_selectsItAutomatically() {
        String username = registerUser("alice");
        JsonNode account = createAccount(loginToken(username), "Farm");

        JsonNode response = login(username);

        assertThat(response.get("accountId").asText()).isEqualTo(account.get("publicId").asText());
    }

    @Test
    void login_withTwoAccounts_leavesTheActiveAccountUnselected() {
        String username = registerUser("alice");
        String token = loginToken(username);
        createAccount(token, "Farm");
        createAccount(token, "Home");

        JsonNode response = login(username);

        assertThat(response.get("accountId").isNull()).isTrue();
    }

    // ---------- login: failures and rate limit ----------

    @Test
    void login_withWrongPassword_returns401() {
        String username = registerUser("alice");

        assertThat(postJson("/api/v1/auth/login", null, Map.of("username", username, "password", "wrong-password")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_afterFiveFailures_returns429EvenWithTheCorrectPassword() {
        String username = registerUser("alice");
        for (int i = 0; i < 5; i++) {
            assertThat(postJson("/api/v1/auth/login", null, Map.of("username", username, "password", "wrong-password")))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        assertThat(postJson("/api/v1/auth/login", null, Map.of("username", username, "password", PASSWORD)))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void login_lockoutOfOneUser_doesNotAffectOthers() {
        String locked = registerUser("alice");
        String other = registerUser("bob");
        for (int i = 0; i < 5; i++) {
            postJson("/api/v1/auth/login", null, Map.of("username", locked, "password", "wrong-password"));
        }

        assertThat(login(other).get("token").asText()).isNotBlank();
    }
}