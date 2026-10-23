package io.github.asmitmans.iotbackend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.asmitmans.iotbackend.exception.TooManyAttemptsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public abstract class AttemptLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Duration lockout;
    private final String blockedMessage;
    private final Clock clock;
    private final Cache<String, AttemptState> attempts;

    protected AttemptLimiter(int maxAttempts, Duration window, Duration lockout,
                             String blockedMessage, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.lockout = lockout;
        this.blockedMessage = blockedMessage;
        this.clock = clock;
        // Eviction only bounds memory; the lockout rules below are evaluated with the Clock.
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(window.compareTo(lockout) >= 0 ? window : lockout)
                .maximumSize(10_000)
                .build();
    }

    public void checkAllowed(String key) {
        AttemptState state = attempts.getIfPresent(normalize(key));
        if (state != null && state.isLockedAt(Instant.now(clock))) {
            throw new TooManyAttemptsException(blockedMessage);
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now(clock);
        attempts.asMap().compute(normalize(key), (k, state) -> nextState(state, now));
    }

    public void recordSuccess(String key) {
        attempts.invalidate(normalize(key));
    }

    private AttemptState nextState(AttemptState state, Instant now) {
        if (state != null && state.isLockedAt(now)) {
            return state;
        }
        if (state == null || state.lockedUntil() != null || !now.isBefore(state.windowStart().plus(window))) {
            return AttemptState.firstFailureAt(now);
        }
        int failures = state.failures() + 1;
        Instant lockedUntil = failures >= maxAttempts ? now.plus(lockout) : null;
        return new AttemptState(failures, state.windowStart(), lockedUntil);
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private record AttemptState(int failures, Instant windowStart, Instant lockedUntil) {

        static AttemptState firstFailureAt(Instant now) {
            return new AttemptState(1, now, null);
        }

        boolean isLockedAt(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }
    }
}