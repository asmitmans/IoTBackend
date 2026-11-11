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

    protected String loginToken(String username) {
        return login(username).get("token").asText();
    }

    /** Creates an account as the given user; returns the body (publicId, name and a new scoped token). */
    protected JsonNode createAccount(String token, String name) {
        MvcTestResult result = postJson("/api/v1/accounts", token, Map.of("name", name));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return body(result);
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