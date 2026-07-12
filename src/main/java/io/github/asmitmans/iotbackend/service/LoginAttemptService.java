package io.github.asmitmans.iotbackend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.asmitmans.iotbackend.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks failed login attempts per username to mitigate brute-force /
 * credential stuffing (OWASP API2:2023 - Broken Authentication).
 * Dedicated Caffeine instance, separate from the CacheManager beans used
 * for deviceAuth/accountAuth, since those share a 24h TTL unsuited for a
 * short lockout window and don't support atomic increments.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MINUTES = 1;

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
                                                                  .expireAfterWrite(WINDOW_MINUTES, TimeUnit.MINUTES)
                                                                  .maximumSize(10_000)
                                                                  .build();

    public void checkAllowed(String username) {
        AtomicInteger count = attempts.getIfPresent(normalize(username));
        if (count != null && count.get() >= MAX_ATTEMPTS) {
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again in a minute.");
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