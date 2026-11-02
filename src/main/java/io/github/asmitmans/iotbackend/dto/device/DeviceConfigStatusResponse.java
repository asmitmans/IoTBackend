package io.github.asmitmans.iotbackend.dto.device;

import java.time.Instant;

public class DeviceConfigStatusResponse {

    private String key;
    private String desiredValue;
    private String reportedValue;
    private String status;
    private Instant updatedAt;

    public DeviceConfigStatusResponse() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDesiredValue() {
        return desiredValue;
    }

    public void setDesiredValue(String desiredValue) {
        this.desiredValue = desiredValue;
    }

    public String getReportedValue() {
        return reportedValue;
    }

    public void setReportedValue(String reportedValue) {
        this.reportedValue = reportedValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}