package io.github.asmitmans.iotbackend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.asmitmans.iotbackend.exception.TooManyAttemptsException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AttemptLimiter {

    private final int maxAttempts;
    private final String blockedMessage;
    private final Cache<String, AtomicInteger> attempts;

    protected AttemptLimiter(int maxAttempts, Duration window, String blockedMessage) {
        this.maxAttempts = maxAttempts;
        this.blockedMessage = blockedMessage;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(10_000)
                .build();
    }

    public void checkAllowed(String key) {
        AtomicInteger count = attempts.getIfPresent(normalize(key));
        if (count != null && count.get() >= maxAttempts) {
            throw new TooManyAttemptsException(blockedMessage);
        }
    }

    public void recordFailure(String key) {
        attempts.asMap()
                .computeIfAbsent(normalize(key), k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void recordSuccess(String key) {
        attempts.invalidate(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
