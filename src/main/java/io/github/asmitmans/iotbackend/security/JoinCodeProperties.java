package io.github.asmitmans.iotbackend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "join-code")
public record JoinCodeProperties(
        @DefaultValue("ABCDEFGHJKLMNPQRSTUVWXYZ23456789") String alphabet,
        @DefaultValue("8") int length,
        @DefaultValue("4") int prefixLength,
        @DefaultValue("24") long validityHours
) {}