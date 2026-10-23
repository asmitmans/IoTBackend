package io.github.asmitmans.iotbackend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class JoinAttemptService extends AttemptLimiter {

    public JoinAttemptService(Clock clock) {
        super(5, Duration.ofMinutes(1), Duration.ofMinutes(1),
                "Too many failed join attempts. Try again in a minute.", clock);
    }
}