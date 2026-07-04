package io.github.asmitmans.iotbackend.dto.devicemodel;

public class DeviceModelListResponse {

    private Long id;
    private String name;

    public DeviceModelListResponse() {
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
}
