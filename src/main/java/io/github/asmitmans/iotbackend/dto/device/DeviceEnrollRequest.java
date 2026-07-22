package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotBlank;

public class DeviceEnrollRequest {

    @NotBlank
    private String serialNumber;

    @NotBlank
    private String name;

    public DeviceEnrollRequest() {
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}