package io.github.asmitmans.iotbackend.dto.devicemodel;

import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeResponse;
import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeResponse;

import java.util.Set;

public class DeviceModelDetailResponse {

    private Long id;
    private String name;
    private Set<MeasurementTypeResponse> measurementTypes;
    private Set<CommandTypeResponse> commandTypes;

    public DeviceModelDetailResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<MeasurementTypeResponse> getMeasurementTypes() {
        return measurementTypes;
    }

    public void setMeasurementTypes(Set<MeasurementTypeResponse> measurementTypes) {
        this.measurementTypes = measurementTypes;
    }

    public Set<CommandTypeResponse> getCommandTypes() {
        return commandTypes;
    }

    public void setCommandTypes(Set<CommandTypeResponse> commandTypes) {
        this.commandTypes = commandTypes;
    }
}
