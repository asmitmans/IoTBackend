package io.github.asmitmans.iotbackend.service;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JoinAttemptService extends AttemptLimiter {

    public JoinAttemptService() {
        super(5, Duration.ofMinutes(1), "Too many failed join attempts. Try again in a minute.");
    }
}