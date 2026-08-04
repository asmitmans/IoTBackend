package io.github.asmitmans.iotbackend.dto.account;

import jakarta.validation.constraints.NotBlank;

public class AccountJoinRequest {

    @NotBlank
    private String joinCode;

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }
}