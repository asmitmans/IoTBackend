package io.github.asmitmans.iotbackend.dto.location;

import jakarta.validation.constraints.NotBlank;

public class LocationRequest {

    @NotBlank
    private String name;

    private Long parentId;

    public LocationRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
