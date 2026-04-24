package io.github.asmitmans.iotbackend.dto.response;

public record LoginResponse(
        String token,
        String username
) {}
