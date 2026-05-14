package io.github.asmitmans.iotbackend.dto.device;

public class DeviceRegistrationResponse {

    private Long id;
    private String serialNumber;
    private String name;
    private String status;
    private String apiKeyPrefix;
    private String apiKeyPlain;

    public DeviceRegistrationResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getApiKeyPrefix() {
        return apiKeyPrefix;
    }

    public void setApiKeyPrefix(String apiKeyPrefix) {
        this.apiKeyPrefix = apiKeyPrefix;
    }

    public String getApiKeyPlain() {
        return apiKeyPlain;
    }

    public void setApiKeyPlain(String apiKeyPlain) {
        this.apiKeyPlain = apiKeyPlain;
    }
}
