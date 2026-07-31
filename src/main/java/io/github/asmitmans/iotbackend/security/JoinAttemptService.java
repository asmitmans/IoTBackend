package io.github.asmitmans.iotbackend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.asmitmans.iotbackend.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JoinAttemptService {

    private final int MAX_ATTEMPTS = 5;
    private final long WINDOW_MINUTES = 1;

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void checkAllowed(String username) {
        AtomicInteger count = attempts.getIfPresent(normalize(username));
        if (count != null && count.get() >= MAX_ATTEMPTS) {
            throw new TooManyAttemptsException("Too many failed join attempts. Try again in a minute.");
        }
    }

    public void recordFailure(String username) {
        attempts.asMap()
                .computeIfAbsent(normalize(username), k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void recordSuccess(String username) {
        attempts.invalidate(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
