package io.github.asmitmans.iotbackend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountFlowIT extends AbstractIntegrationTest {

    // ---------- create ----------

    @Test
    void createAccount_makesTheCreatorItsOwner() {
        String alice = registerUser("alice");
        JsonNode account = createAccount(loginToken(alice), "Farm");

        JsonNode mine = body(get("/api/v1/accounts/mine", account.get("token").asText()));

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("publicId").asText()).isEqualTo(account.get("publicId").asText());
        assertThat(mine.get(0).get("accountRole").asText()).isEqualTo("OWNER");
    }

    // ---------- invite and join ----------

    @Test
    void joinWithCode_addsTheUserAsMember() {
        Owned farm = accountOwnedBy("alice");
        String bob = registerUser("bob");

        JsonNode joined = join(loginToken(bob), joinCode(farm));

        assertThat(joined.get("publicId").asText()).isEqualTo(farm.accountId());
        JsonNode mine = body(get("/api/v1/accounts/mine", joined.get("token").asText()));
        assertThat(mine.get(0).get("accountRole").asText()).isEqualTo("MEMBER");
    }

    @Test
    void generateJoinCode_asMember_returns403() {
        Owned farm = accountOwnedBy("alice");
        String bobToken = join(loginToken(registerUser("bob")), joinCode(farm)).get("token").asText();

        assertThat(post("/api/v1/accounts/" + farm.accountId() + "/join-code", bobToken))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    // ---------- switch ----------

    @Test
    void switchAccount_toAnAccountOfTheUser_returnsANewToken() {
        String alice = registerUser("alice");
        String token = loginToken(alice);
        createAccount(token, "Farm");
        JsonNode home = createAccount(token, "Home");

        JsonNode switched = body(post("/api/v1/accounts/" + home.get("publicId").asText() + "/switch", token));

        assertThat(switched.get("publicId").asText()).isEqualTo(home.get("publicId").asText());
        assertThat(switched.get("token").asText()).isNotBlank();
    }

    @Test
    void switchAccount_toAnAccountTheUserDoesNotBelongTo_returns403() {
        Owned farm = accountOwnedBy("alice");
        String bobToken = loginToken(registerUser("bob"));

        assertThat(post("/api/v1/accounts/" + farm.accountId() + "/switch", bobToken))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    // ---------- promote, leave, remove ----------

    @Test
    void lastOwner_cannotLeave() {
        Owned farm = accountOwnedBy("alice");

        assertThat(post("/api/v1/accounts/" + farm.accountId() + "/leave", farm.ownerToken()))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void ownerCanLeave_afterPromotingAnotherMember() {
        Owned farm = accountOwnedBy("alice");
        String bob = registerUser("bob");
        join(loginToken(bob), joinCode(farm));

        assertThat(post("/api/v1/accounts/" + farm.accountId() + "/members/" + bob + "/promote", farm.ownerToken()))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(post("/api/v1/accounts/" + farm.accountId() + "/leave", farm.ownerToken()))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(body(get("/api/v1/accounts/mine", loginToken(farm.owner())))).isEmpty();
    }

    // ---------- revocation ----------

    @Test
    void removedMember_losesAccessImmediately_evenWithAStillValidToken() {
        Owned farm = accountOwnedBy("alice");
        String bob = registerUser("bob");
        String bobToken = join(loginToken(bob), joinCode(farm)).get("token").asText();
        assertThat(get("/api/v1/locations", bobToken)).hasStatusOk();

        assertThat(delete("/api/v1/accounts/" + farm.accountId() + "/members/" + bob, farm.ownerToken()))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(get("/api/v1/locations", bobToken)).hasStatus(HttpStatus.FORBIDDEN);
    }

    // ---------- platform admin ----------

    @Test
    void admin_cannotUseAccountSelfServiceEndpoints() {
        String adminToken = loginToken(createAdmin());

        assertThat(get("/api/v1/accounts/mine", adminToken)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(postJson("/api/v1/accounts", adminToken, Map.of("name", "Admin account")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    // ---------- authentication ----------

    @Test
    void request_withoutToken_returns401() {
        assertThat(get("/api/v1/accounts/mine", null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void request_withInvalidToken_returns401() {
        assertThat(get("/api/v1/accounts/mine", "not-a-jwt")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
