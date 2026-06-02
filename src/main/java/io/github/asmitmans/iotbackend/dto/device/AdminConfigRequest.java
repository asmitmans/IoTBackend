package io.github.asmitmans.iotbackend.dto.device;

import java.util.Map;

public class AdminConfigRequest {

    private Map<String, String> desired;

    public AdminConfigRequest() {
    }

    public Map<String, String> getDesired() {
        return desired;
    }

    public void setDesired(Map<String, String> desired) {
        this.desired = desired;
    }
}
