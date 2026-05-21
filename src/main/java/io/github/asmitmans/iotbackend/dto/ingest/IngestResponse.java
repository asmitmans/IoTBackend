package io.github.asmitmans.iotbackend.dto.ingest;

import java.util.Map;

public class IngestResponse {

    private String status;
    private String configAction;
    private java.util.Map<String, String> config;

    public IngestResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfigAction() {
        return configAction;
    }

    public void setConfigAction(String configAction) {
        this.configAction = configAction;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
