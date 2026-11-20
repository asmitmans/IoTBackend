package io.github.asmitmans.iotbackend.dto.location;

import java.util.UUID;

public class LocationResponse {

    private UUID publicId;
    private String name;
    private UUID parentPublicId;

    public LocationResponse() {
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
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
