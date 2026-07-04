package io.github.asmitmans.iotbackend.dto.auth;

public record LoginResponse(
        String token,
        String username
) {}
