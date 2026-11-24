package io.github.asmitmans.iotbackend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceFlowIT extends AbstractIntegrationTest {

    private String adminToken;
    private long modelId;

    @BeforeEach
    void setUpCatalog() {
        adminToken = loginToken(createAdmin());
        modelId = createDeviceModel(adminToken);
    }

    // ---------- provisioning: register -> enroll -> claim ----------

    @Test
    void provisioning_registerEnrollAndClaim_leavesTheDeviceActiveInTheAccount() {
        Owned farm = accountOwnedBy("alice");
        JsonNode device = registerDevice(adminToken, modelId);
        assertThat(device.get("status").asText()).isEqualTo("UNCLAIMED");

        enrollDevice(farm.ownerToken(), serial(device));
        String apiKey = claimDevice(serial(device));

        assertThat(apiKey).startsWith("iotdev_");
        JsonNode detail = body(get("/api/v1/devices/" + publicId(device), farm.ownerToken()));
        assertThat(detail.get("status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void claim_twice_returns409() {
        Owned farm = accountOwnedBy("alice");
        JsonNode device = registerDevice(adminToken, modelId);
        enrollDevice(farm.ownerToken(), serial(device));
        claimDevice(serial(device));

        assertThat(postJson("/api/v1/devices/claim", null, Map.of("serialNumber", serial(device))))
                .hasStatus(HttpStatus.CONFLICT);
    }

    // ---------- device authentication (API key) ----------

    @Test
    void claimedDevice_authenticatesWithItsApiKey() {
        Owned farm = accountOwnedBy("alice");
        JsonNode device = registerDevice(adminToken, modelId);
        enrollDevice(farm.ownerToken(), serial(device));
        String apiKey = claimDevice(serial(device));

        assertThat(getAsDevice("/api/v1/devices/" + publicId(device) + "/config", apiKey)).hasStatusOk();
    }

    @Test
    void unknownOrMalformedApiKey_returns401() {
        String unknownKey = "iotdev_" + "0".repeat(64);

        assertThat(getAsDevice("/api/v1/devices/any/config", unknownKey)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(getAsDevice("/api/v1/devices/any/config", "iotdev_")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    // ---------- roles ----------

    @Test
    void registerDevice_asUser_returns403() {
        Owned farm = accountOwnedBy("alice");

        assertThat(postJson("/api/v1/devices", farm.ownerToken(), Map.of("deviceModelId", modelId)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void device_cannotUseUserEndpoints() {
        Owned farm = accountOwnedBy("alice");
        JsonNode device = registerDevice(adminToken, modelId);
        enrollDevice(farm.ownerToken(), serial(device));
        String apiKey = claimDevice(serial(device));

        assertThat(getAsDevice("/api/v1/devices", apiKey)).hasStatus(HttpStatus.FORBIDDEN);
    }

    // ---------- isolation between accounts ----------

    @Test
    void userOfAnotherAccount_cannotSeeTheDevice() {
        Owned farm = accountOwnedBy("alice");
        Owned other = accountOwnedBy("bob");
        JsonNode device = registerDevice(adminToken, modelId);
        enrollDevice(farm.ownerToken(), serial(device));

        assertThat(get("/api/v1/devices/" + publicId(device), other.ownerToken()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(body(get("/api/v1/devices", other.ownerToken())).get("content")).isEmpty();
    }

    @Test
    void enroll_ofADeviceAlreadyEnrolledElsewhere_returns409() {
        Owned farm = accountOwnedBy("alice");
        Owned other = accountOwnedBy("bob");
        JsonNode device = registerDevice(adminToken, modelId);
        enrollDevice(farm.ownerToken(), serial(device));

        assertThat(postJson("/api/v1/devices/enroll", other.ownerToken(),
                Map.of("serialNumber", serial(device), "name", "Stolen")))
                .hasStatus(HttpStatus.CONFLICT);
    }

    // ---------- revocation ----------

    @Test
    void removedMember_cannotEnrollIntoTheAccount() {
        Owned farm = accountOwnedBy("alice");
        String bob = registerUser("bob");
        String bobToken = join(loginToken(bob), joinCode(farm)).get("token").asText();
        delete("/api/v1/accounts/" + farm.accountId() + "/members/" + bob, farm.ownerToken());
        JsonNode device = registerDevice(adminToken, modelId);

        assertThat(postJson("/api/v1/devices/enroll", bobToken,
                Map.of("serialNumber", serial(device), "name", "Sensor")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    // ---------- desired config (user side) ----------

    @Test
    void setDesiredConfig_leavesTheKeyPendingAndFlagsTheDevice() {
        Owned farm = accountOwnedBy("alice");
        String id = activeDeviceOf(farm);

        assertThat(putJson("/api/v1/devices/" + id + "/config", farm.ownerToken(),
                Map.of("desired", Map.of("interval", "60"))))
                .hasStatusOk();

        JsonNode status = body(get("/api/v1/devices/" + id + "/config/status", farm.ownerToken()));
        assertThat(status).hasSize(1);
        assertThat(status.get(0).get("key").asText()).isEqualTo("interval");
        assertThat(status.get(0).get("desiredValue").asText()).isEqualTo("60");
        assertThat(status.get(0).get("status").asText()).isEqualTo("PENDING");
        assertThat(body(get("/api/v1/devices/" + id, farm.ownerToken())).get("configPending").asBoolean()).isTrue();
    }

    @Test
    void setDesiredConfig_withEmptyMap_returns400() {
        Owned farm = accountOwnedBy("alice");
        String id = activeDeviceOf(farm);

        assertThat(putJson("/api/v1/devices/" + id + "/config", farm.ownerToken(), Map.of("desired", Map.of())))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    // ---------- commands (user side) ----------

    @Test
    void queueCommand_returns202AndFlagsTheDevice() {
        Owned farm = accountOwnedBy("alice");
        String id = activeDeviceOf(farm);

        assertThat(postJson("/api/v1/devices/" + id + "/commands", farm.ownerToken(), Map.of("type", "REBOOT")))
                .hasStatus(HttpStatus.ACCEPTED);

        assertThat(body(get("/api/v1/devices/" + id, farm.ownerToken())).get("commandPending").asBoolean()).isTrue();
    }

    @Test
    void queueCommand_asAdminForADeviceWithoutAccount_returns409() {
        JsonNode device = registerDevice(adminToken, modelId);

        assertThat(postJson("/api/v1/devices/" + publicId(device) + "/commands", adminToken, Map.of("type", "REBOOT")))
                .hasStatus(HttpStatus.CONFLICT);
    }

    // ---------- isolation for config and commands ----------

    @Test
    void userOfAnotherAccount_cannotConfigureOrCommandTheDevice() {
        Owned farm = accountOwnedBy("alice");
        Owned other = accountOwnedBy("bob");
        String id = activeDeviceOf(farm);

        assertThat(putJson("/api/v1/devices/" + id + "/config", other.ownerToken(),
                Map.of("desired", Map.of("interval", "60"))))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get("/api/v1/devices/" + id + "/config/status", other.ownerToken()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(postJson("/api/v1/devices/" + id + "/commands", other.ownerToken(), Map.of("type", "REBOOT")))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    // ---------- catalog ----------

    @Test
    void deleteDeviceModel_notInUse_returns204() {
        assertThat(delete("/api/v1/device-models/" + modelId, adminToken)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteDeviceModel_usedByADevice_returns409() {
        registerDevice(adminToken, modelId);

        assertThat(delete("/api/v1/device-models/" + modelId, adminToken)).hasStatus(HttpStatus.CONFLICT);
    }

    // ---------- helpers ----------

    /** Registers, enrolls into the account and claims a device; returns its publicId. */
    private String activeDeviceOf(Owned account) {
        JsonNode device = registerDevice(adminToken, modelId);
        enrollDevice(account.ownerToken(), serial(device));
        claimDevice(serial(device));
        return publicId(device);
    }

    private static String serial(JsonNode device) {
        return device.get("serialNumber").asText();
    }

    private static String publicId(JsonNode device) {
        return device.get("publicId").asText();
    }
}
