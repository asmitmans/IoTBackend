package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.exception.TooManyAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AttemptLimiterTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final Duration LOCKOUT = Duration.ofSeconds(300);
    private static final String MESSAGE = "blocked";

    private MutableClock clock;
    private AttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        limiter = new TestLimiter(clock);
    }

    // ---------- threshold ----------

    @Test
    void checkAllowed_withoutFailures_allows() {
        assertAllowed("alice");
    }

    @Test
    void checkAllowed_belowThreshold_allows() {
        fail("alice", MAX_ATTEMPTS - 1);

        assertAllowed("alice");
    }

    @Test
    void checkAllowed_atThreshold_throwsTooManyAttempts() {
        fail("alice", MAX_ATTEMPTS);

        assertThatThrownBy(() -> limiter.checkAllowed("alice"))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessage(MESSAGE);
    }

    // ---------- lockout duration ----------

    @Test
    void lockout_lastsTheFullDurationFromTheBlockingFailure() {
        fail("alice", MAX_ATTEMPTS - 1);
        clock.advance(Duration.ofSeconds(59));
        fail("alice", 1);

        clock.advance(LOCKOUT.minusSeconds(1));
        assertBlocked("alice");

        clock.advance(Duration.ofSeconds(1));
        assertAllowed("alice");
    }

    @Test
    void lockout_isNotExtendedByFailuresWhileLocked() {
        fail("alice", MAX_ATTEMPTS);
        clock.advance(Duration.ofSeconds(100));
        fail("alice", 1);

        clock.advance(LOCKOUT.minusSeconds(100));

        assertAllowed("alice");
    }

    @Test
    void afterLockoutExpires_countingStartsFromZero() {
        fail("alice", MAX_ATTEMPTS);
        clock.advance(LOCKOUT);

        fail("alice", MAX_ATTEMPTS - 1);

        assertAllowed("alice");
    }

    // ---------- observation window ----------

    @Test
    void failuresOutsideTheWindow_startANewWindow() {
        fail("alice", MAX_ATTEMPTS - 1);
        clock.advance(WINDOW);

        fail("alice", MAX_ATTEMPTS - 1);

        assertAllowed("alice");
    }

    @Test
    void failuresJustInsideTheWindow_stillCount() {
        fail("alice", MAX_ATTEMPTS - 1);
        clock.advance(WINDOW.minusSeconds(1));

        fail("alice", 1);

        assertBlocked("alice");
    }

    // ---------- recordSuccess() ----------

    @Test
    void recordSuccess_clearsPreviousFailures() {
        fail("alice", MAX_ATTEMPTS - 1);
        limiter.recordSuccess("alice");

        fail("alice", MAX_ATTEMPTS - 1);

        assertAllowed("alice");
    }

    // ---------- keys ----------

    @Test
    void keys_areNormalizedIgnoringCaseAndSurroundingSpaces() {
        limiter.recordFailure("alice");
        limiter.recordFailure("ALICE");
        limiter.recordFailure(" Alice ");
        limiter.recordFailure("aLiCe");
        limiter.recordFailure("alice  ");

        assertBlocked("Alice");
    }

    @Test
    void nullKey_isTreatedAsEmpty() {
        fail(null, MAX_ATTEMPTS);

        assertBlocked("");
    }

    @Test
    void counters_areIndependentPerKey() {
        fail("alice", MAX_ATTEMPTS);

        assertBlocked("alice");
        assertAllowed("bob");
    }

    // ---------- helpers ----------

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(key);
        }
    }

    private void assertAllowed(String key) {
        assertThatCode(() -> limiter.checkAllowed(key)).doesNotThrowAnyException();
    }

    private void assertBlocked(String key) {
        assertThatThrownBy(() -> limiter.checkAllowed(key)).isInstanceOf(TooManyAttemptsException.class);
    }

    private static final class TestLimiter extends AttemptLimiter {
        TestLimiter(Clock clock) {
            super(MAX_ATTEMPTS, WINDOW, LOCKOUT, MESSAGE, clock);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}