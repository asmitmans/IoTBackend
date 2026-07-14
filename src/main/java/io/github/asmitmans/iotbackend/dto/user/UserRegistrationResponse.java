package io.github.asmitmans.iotbackend.dto.user;

public class UserRegistrationResponse {

    private String username;

    public UserRegistrationResponse() {
    }

    public UserRegistrationResponse(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}