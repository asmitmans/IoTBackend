package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotBlank;

public class DeviceEnrollRequest {

    @NotBlank
    private String serialNumber;

    public DeviceEnrollRequest() {
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
