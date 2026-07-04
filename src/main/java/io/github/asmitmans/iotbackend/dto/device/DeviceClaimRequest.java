package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotBlank;

public class DeviceClaimRequest {

    @NotBlank
    private String serialNumber;

    public DeviceClaimRequest() {
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
