package io.github.asmitmans.iotbackend.dto.measurement;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

public class MeasurementTypeRequest {

    @NotBlank
    private String name;

    private String siUnit;

    private String category;

    @NotBlank
    private String dataType;

    public MeasurementTypeRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSiUnit() {
        return siUnit;
    }

    public void setSiUnit(String siUnit) {
        this.siUnit = siUnit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}
