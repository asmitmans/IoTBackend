package io.github.asmitmans.iotbackend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class LoginAttemptService extends AttemptLimiter {

    public LoginAttemptService(Clock clock) {
        super(5, Duration.ofMinutes(1), Duration.ofMinutes(1),
                "Too many failed login attempts. Try again in a minute.", clock);
    }
}