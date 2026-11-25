package io.github.asmitmans.iotbackend.dto.location;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class LocationRequest {

    @NotBlank
    private String name;

    private UUID parentPublicId;

    public LocationRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getParentPublicId() {
        return parentPublicId;
    }

    public void setParentPublicId(UUID parentPublicId) {
        this.parentPublicId = parentPublicId;
    }

}
