package io.github.asmitmans.iotbackend.dto.account;

import java.util.UUID;

public class AccountRegistrationResponse {

    private UUID publicId;
    private String name;

    public AccountRegistrationResponse() {
    }

    public AccountRegistrationResponse(UUID publicId, String name) {
        this.publicId = publicId;
        this.name = name;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}