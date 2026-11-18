package io.github.asmitmans.iotbackend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.asmitmans.iotbackend.entity.Role;
import io.github.asmitmans.iotbackend.entity.User;
import io.github.asmitmans.iotbackend.repository.RoleRepository;
import io.github.asmitmans.iotbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES;
    protected static final String PASSWORD = "integration-test-password";

    static {
        POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16.9-alpine"))
                .withDatabaseName("iotbackend_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- data ----------

    /** Unique per call, so tests never share users (nor their in-memory attempt counters). */
    protected String uniqueUsername(String base) {
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Registers a regular user through the public API. */
    protected String registerUser(String base) {
        String username = uniqueUsername(base);
        MvcTestResult result = postJson("/api/v1/users", null,
                Map.of("username", username, "password", PASSWORD));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return username;
    }

    /** Creates a platform admin directly (there is no API for it and the dev seed does not run in tests). */
    protected String createAdmin() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        User admin = new User();
        admin.setUsername(uniqueUsername("admin"));
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole));
        return userRepository.save(admin).getUsername();
    }

    // ---------- API calls ----------

    /** Logs in and returns the response body as JSON (token, username, accountId). */
    protected JsonNode login(String username) {
        MvcTestResult result = postJson("/api/v1/auth/login", null,
                Map.of("username", username, "password", PASSWORD));
        assertThat(result).hasStatusOk();
        return body(result);
    }

    protected MvcTestResult putJson(String uri, String token, Object body) {
        var request = mvc.put().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    /** POST without a body, for action endpoints such as switch, leave or promote. */
    protected MvcTestResult post(String uri, String token) {
        var request = mvc.post().uri(uri);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    protected MvcTestResult delete(String uri, String token) {
        var request = mvc.delete().uri(uri);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    protected String loginToken(String username) {
        return login(username).get("token").asText();
    }

    /** Creates an account as the given user; returns the body (publicId, name and a new scoped token). */
    protected JsonNode createAccount(String token, String name) {
        MvcTestResult result = postJson("/api/v1/accounts", token, Map.of("name", name));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return body(result);
    }

    /** A user who owns an account, with a token scoped to that account. */
    protected record Owned(String owner, String ownerToken, String accountId) {}

    /** Registers a user who creates an account; the returned token is scoped to that account. */
    protected Owned accountOwnedBy(String base) {
        String owner = registerUser(base);
        JsonNode account = createAccount(loginToken(owner), base + "'s farm");
        return new Owned(owner, account.get("token").asText(), account.get("publicId").asText());
    }

    protected String joinCode(Owned account) {
        var result = post("/api/v1/accounts/" + account.accountId() + "/join-code", account.ownerToken());
        assertThat(result).hasStatusOk();
        return body(result).get("joinCode").asText();
    }

    /** Joins an account with a code; returns the body (publicId, name and a token scoped to it). */
    protected JsonNode join(String token, String joinCode) {
        var result = postJson("/api/v1/accounts/join", token, Map.of("joinCode", joinCode));
        assertThat(result).hasStatusOk();
        return body(result);
    }

    // ---------- devices ----------

    /** Creates a device model as admin and returns its id. */
    protected long createDeviceModel(String adminToken) {
        MvcTestResult result = postJson("/api/v1/device-models", adminToken,
                Map.of("name", "Model " + UUID.randomUUID().toString().substring(0, 8)));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return body(result).get("id").asLong();
    }

    /** Registers a new UNCLAIMED device as admin; returns the body (publicId, serialNumber, status). */
    protected JsonNode registerDevice(String adminToken, long deviceModelId) {
        MvcTestResult result = postJson("/api/v1/devices", adminToken, Map.of("deviceModelId", deviceModelId));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return body(result);
    }

    /** Enrolls a device into the caller's active account (device goes to PENDING). */
    protected void enrollDevice(String userToken, String serialNumber) {
        MvcTestResult result = postJson("/api/v1/devices/enroll", userToken,
                Map.of("serialNumber", serialNumber, "name", "Sensor " + serialNumber.substring(0, 4)));
        assertThat(result).hasStatusOk();
    }

    /** Claims a PENDING device (public endpoint) and returns its plain API key. */
    protected String claimDevice(String serialNumber) {
        MvcTestResult result = postJson("/api/v1/devices/claim", null, Map.of("serialNumber", serialNumber));
        assertThat(result).hasStatusOk();
        return body(result).get("apiKeyPlain").asText();
    }

    /** GET authenticated as a device, with its API key. */
    protected MvcTestResult getAsDevice(String uri, String apiKey) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey).exchange();
    }

    // ---------- low level ----------

    protected MvcTestResult postJson(String uri, String token, Object body) {
        var request = mvc.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    protected MvcTestResult get(String uri, String token) {
        var request = mvc.get().uri(uri);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    protected JsonNode body(MvcTestResult result) {
        try {
            return objectMapper.readTree(result.getResponse().getContentAsString());
        } catch (Exception e) {
            throw new IllegalStateException("Response body is not valid JSON", e);
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}