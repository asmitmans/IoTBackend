package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class AdminConfigRequest {

    @NotEmpty
    private Map<@NotBlank @Size(max = 100) String, @NotNull @Size(max = 255) String> desired;

    public AdminConfigRequest() {
    }

    public Map<String, String> getDesired() {
        return desired;
    }

    public void setDesired(Map<String, String> desired) {
        this.desired = desired;
    }
}
