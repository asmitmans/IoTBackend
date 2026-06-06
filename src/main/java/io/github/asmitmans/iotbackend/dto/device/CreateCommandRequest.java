package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotBlank;

public class CreateCommandRequest {

    @NotBlank
    private String type;

    private String payload;

    public CreateCommandRequest() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
