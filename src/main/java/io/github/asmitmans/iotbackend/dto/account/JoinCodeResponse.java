package io.github.asmitmans.iotbackend.dto.account;

import java.time.Instant;

public class JoinCodeResponse {

    private String joinCode;
    private Instant expiresAt;

    public JoinCodeResponse() {
    }

    public JoinCodeResponse(String joinCode, Instant expiresAt) {
        this.joinCode = joinCode;
        this.expiresAt = expiresAt;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}