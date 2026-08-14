package io.github.asmitmans.iotbackend.dto.auth;

import java.util.UUID;

public record LoginResponse(
        String token,
        String username,
        UUID accountId
) {}