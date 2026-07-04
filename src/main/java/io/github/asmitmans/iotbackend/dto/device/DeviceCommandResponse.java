package io.github.asmitmans.iotbackend.dto.device;

public class DeviceCommandResponse {

    private Long id;
    private String type;
    private String payload;

    public DeviceCommandResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
