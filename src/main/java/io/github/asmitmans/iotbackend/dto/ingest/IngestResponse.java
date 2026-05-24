package io.github.asmitmans.iotbackend.dto.ingest;

public class IngestResponse {

    private String status;
    private boolean configPending;
    private boolean commandPending;

    public IngestResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isConfigPending() {
        return configPending;
    }

    public void setConfigPending(boolean configPending) {
        this.configPending = configPending;
    }

    public boolean isCommandPending() {
        return commandPending;
    }

    public void setCommandPending(boolean commandPending) {
        this.commandPending = commandPending;
    }
}
