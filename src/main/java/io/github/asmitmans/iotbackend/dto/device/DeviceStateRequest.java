package io.github.asmitmans.iotbackend.dto.device;

import java.util.Map;

public class DeviceStateRequest {

    private Map<String, String> reported;

    public DeviceStateRequest() {
    }

    public Map<String, String> getReported() {
        return reported;
    }

    public void setReported(Map<String, String> reported) {
        this.reported = reported;
    }
}
