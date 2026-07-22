package io.github.asmitmans.iotbackend.dto.device;

import jakarta.validation.constraints.NotNull;

public class DeviceRegistrationRequest {

    @NotNull
    private Long deviceModelId;

    private Long locationId;

    public DeviceRegistrationRequest() {
    }

    public Long getDeviceModelId() {
        return deviceModelId;
    }

    public void setDeviceModelId(Long deviceModelId) {
        this.deviceModelId = deviceModelId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
