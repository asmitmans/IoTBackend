package io.github.asmitmans.iotbackend.dto.account;

import java.util.UUID;

public class AccountRegistrationResponse {

    private UUID publicId;
    private String name;
    private String token;

    public AccountRegistrationResponse() {
    }

    public AccountRegistrationResponse(UUID publicId, String name, String token) {
        this.publicId = publicId;
        this.name = name;
        this.token = token;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}