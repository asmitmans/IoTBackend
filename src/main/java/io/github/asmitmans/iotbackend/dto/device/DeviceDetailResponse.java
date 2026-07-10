package io.github.asmitmans.iotbackend.dto.device;

import java.util.UUID;

public class DeviceDetailResponse {

    private UUID publicId;
    private String serialNumber;
    private String name;
    private String status;
    private Long deviceModelId;
    private String deviceModelName;
    private Long locationId;
    private boolean configPending;
    private boolean commandPending;

    public DeviceDetailResponse() {
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

    public Long getDeviceModelId() {
        return deviceModelId;
    }

    public void setDeviceModelId(Long deviceModelId) {
        this.deviceModelId = deviceModelId;
    }

    public String getDeviceModelName() {
        return deviceModelName;
    }

    public void setDeviceModelName(String deviceModelName) {
        this.deviceModelName = deviceModelName;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public boolean isConfigPending() {
        return configPending;
    }

    public void setConfigPending(boolean configPending) {
        this.configPending = configPending;
    }

    public boolean isCommandPending() {
        return commandPending;
    }

    public void setCommandPending(boolean commandPending) {
        this.commandPending = commandPending;
    }
}