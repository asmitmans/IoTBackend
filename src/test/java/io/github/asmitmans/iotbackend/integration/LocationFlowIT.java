package io.github.asmitmans.iotbackend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LocationFlowIT extends AbstractIntegrationTest {

    private static final String LOCATIONS = "/api/v1/locations";

    // ---------- API by public UUID ----------

    @Test
    void create_thenGetByPublicId_returnsTheLocation() {
        Owned farm = accountOwnedBy("alice");

        JsonNode created = createLocation(farm.ownerToken(), "Barn", null);
        String publicId = created.get("publicId").asText();

        assertThat(created.has("id")).isFalse();
        JsonNode read = body(get(LOCATIONS + "/" + publicId, farm.ownerToken()));
        assertThat(read.get("publicId").asText()).isEqualTo(publicId);
        assertThat(read.get("name").asText()).isEqualTo("Barn");
    }

    // ---------- isolation between accounts ----------

    @Test
    void userOfAnotherAccount_cannotListReadUpdateOrDeleteTheLocation() {
        Owned farm = accountOwnedBy("alice");
        Owned other = accountOwnedBy("bob");
        String barn = createLocation(farm.ownerToken(), "Barn", null).get("publicId").asText();

        assertThat(body(get(LOCATIONS, other.ownerToken()))).isEmpty();
        assertThat(get(LOCATIONS + "/" + barn, other.ownerToken())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(putJson(LOCATIONS + "/" + barn, other.ownerToken(), locationRequest("Renamed", null)))
            .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(delete(LOCATIONS + "/" + barn, other.ownerToken())).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(body(get(LOCATIONS + "/" + barn, farm.ownerToken())).get("name").asText()).isEqualTo("Barn");
    }

    @Test
    void create_underAParentOfAnotherAccount_returns404() {
        Owned farm = accountOwnedBy("alice");
        Owned other = accountOwnedBy("bob");
        String barn = createLocation(farm.ownerToken(), "Barn", null).get("publicId").asText();

        assertThat(postJson(LOCATIONS, other.ownerToken(), locationRequest("Stall", barn)))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    // ---------- hierarchy rules ----------

    @Test
    void create_twoRootsWithTheSameName_returns409() {
        Owned farm = accountOwnedBy("alice");
        createLocation(farm.ownerToken(), "Barn", null);

        assertThat(postJson(LOCATIONS, farm.ownerToken(), locationRequest("Barn", null)))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void update_renamingToASiblingName_returns409() {
        Owned farm = accountOwnedBy("alice");
        createLocation(farm.ownerToken(), "Barn", null);
        String shed = createLocation(farm.ownerToken(), "Shed", null).get("publicId").asText();

        assertThat(putJson(LOCATIONS + "/" + shed, farm.ownerToken(), locationRequest("Barn", null)))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void update_movingUnderItsGrandchild_returns409() {
        Owned farm = accountOwnedBy("alice");
        String barn = createLocation(farm.ownerToken(), "Barn", null).get("publicId").asText();
        String stall = createLocation(farm.ownerToken(), "Stall", barn).get("publicId").asText();
        String box = createLocation(farm.ownerToken(), "Box", stall).get("publicId").asText();

        assertThat(putJson(LOCATIONS + "/" + barn, farm.ownerToken(), locationRequest("Barn", box)))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void delete_withChildren_returns409_butALeafCanBeDeleted() {
        Owned farm = accountOwnedBy("alice");
        String barn = createLocation(farm.ownerToken(), "Barn", null).get("publicId").asText();
        String stall = createLocation(farm.ownerToken(), "Stall", barn).get("publicId").asText();

        assertThat(delete(LOCATIONS + "/" + barn, farm.ownerToken())).hasStatus(HttpStatus.CONFLICT);
        assertThat(delete(LOCATIONS + "/" + stall, farm.ownerToken())).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(delete(LOCATIONS + "/" + barn, farm.ownerToken())).hasStatus(HttpStatus.NO_CONTENT);
    }

    // ---------- helpers ----------

    private JsonNode createLocation(String token, String name, String parentPublicId) {
        var result = postJson(LOCATIONS, token, locationRequest(name, parentPublicId));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return body(result);
    }

    private static Map<String, Object> locationRequest(String name, String parentPublicId) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("parentPublicId", parentPublicId);
        return request;
    }

}