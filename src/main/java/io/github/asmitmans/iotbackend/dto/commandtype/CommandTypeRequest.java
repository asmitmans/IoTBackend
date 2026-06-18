package io.github.asmitmans.iotbackend.dto.commandtype;

import jakarta.validation.constraints.NotBlank;

public class CommandTypeRequest {

    @NotBlank
    private String name;

    private String description;

    public CommandTypeRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
