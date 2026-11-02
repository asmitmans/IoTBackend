package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class DeviceStateRequest {

    @NotEmpty
    private Map<@NotBlank @Size(max = 100) String, @NotNull @Size(max = 255) String> reported;

    public DeviceStateRequest() {
    }

    public Map<String, String> getReported() {
        return reported;
    }

    public void setReported(Map<String, String> reported) {
        this.reported = reported;
    }
}
