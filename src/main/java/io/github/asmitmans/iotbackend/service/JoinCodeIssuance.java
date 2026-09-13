package io.github.asmitmans.iotbackend.service;

import java.time.Instant;

public record JoinCodeIssuance(
        String plainCode,
        String hash,
        String prefix,
        Instant expiresAt
) {}