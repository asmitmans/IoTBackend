package io.github.asmitmans.iotbackend.dto.ingest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class IngestRequest {

    @NotNull
    private java.time.Instant recordedAt;

    @NotBlank
    private String payload;

    private String configChecksum;
    private Long sequenceNumber;

    public IngestRequest() {
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getConfigChecksum() {
        return configChecksum;
    }

    public void setConfigChecksum(String configChecksum) {
        this.configChecksum = configChecksum;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
