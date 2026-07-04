package io.github.asmitmans.iotbackend.dto.devicemodel;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public class DeviceModelRequest {

    @NotBlank
    private String name;

    private Set<Long> measurementTypeIds;
    private Set<Long> commandTypeIds;

    public DeviceModelRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Long> getMeasurementTypeIds() {
        return measurementTypeIds;
    }

    public void setMeasurementTypeIds(Set<Long> measurementTypeIds) {
        this.measurementTypeIds = measurementTypeIds;
    }

    public Set<Long> getCommandTypeIds() {
        return commandTypeIds;
    }

    public void setCommandTypeIds(Set<Long> commandTypeIds) {
        this.commandTypeIds = commandTypeIds;
    }
}
