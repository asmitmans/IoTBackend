package io.github.asmitmans.iotbackend.dto.device;

import java.util.UUID;

public class DeviceEnrollResponse {

    private UUID publicId;
    private String serialNumber;
    private String name;
    private String status;

    public DeviceEnrollResponse() {
    }

    public DeviceEnrollResponse(UUID publicId, String serialNumber, String name, String status) {
        this.publicId = publicId;
        this.serialNumber = serialNumber;
        this.name = name;
        this.status = status;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}