package io.github.asmitmans.iotbackend.dto.device;

public class DeviceClaimResponse {

    private String serialNumber;
    private String status;
    private String apiKeyPlain;

    public DeviceClaimResponse() {
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApiKeyPlain() {
        return apiKeyPlain;
    }

    public void setApiKeyPlain(String apiKeyPlain) {
        this.apiKeyPlain = apiKeyPlain;
    }
}
