package Test;

import Core.Resolver.HybridKeyResolver;
import Core.Resolver.KeyResolver;
import TokenBucket.TokenBucketRateLimiter;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;
    private KeyResolver resolver;
    private long currentTimeStampInMs;
    private final long tokenFillRateDefault = 1;
    private final int capacity = 5;

    @Before
    public void setUp() throws Exception {
        resolver = new HybridKeyResolver();
        currentTimeStampInMs = Instant.now().toEpochMilli();
        rateLimiter = new TokenBucketRateLimiter(resolver, tokenFillRateDefault, capacity);
    }

    @Test
    public void testAllowWithinCapacity() {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        }
    }

    @Test
    public void testRejectAfterExceedingCapacity() {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        }
        assertFalse(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
    }

    @Test
    public void testRefillAfterWait() throws InterruptedException {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        }

        // Exceeding token limit
        assertFalse(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));

        // Wait 2000 ms to gain 2 tokens
        long newTime = currentTimeStampInMs + 2000;
        assertTrue(rateLimiter.allowRequest("user1", "", newTime));
        assertTrue(rateLimiter.allowRequest("user1", "", newTime));
        assertFalse(rateLimiter.allowRequest("user1", "", newTime));
    }

    @Test
    public void testMultipleUsersHaveSeparateBuckets() {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
            assertTrue(rateLimiter.allowRequest("user2", "", currentTimeStampInMs));
        }

        // Both should be rejected now
        assertFalse(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        assertFalse(rateLimiter.allowRequest("user2", "", currentTimeStampInMs));
    }

    @Test
    public void testOutOfOrderTimestamps() {
        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        // Send a timestamp earlier than last
        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs - 1000)); // Should not refill
        for (int i = 0; i < capacity - 2; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        }
        assertFalse(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
    }

    @Test
    public void testMaxCapacityNeverExceeded() {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        }

        long later = currentTimeStampInMs + 10000;
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequest("user1", "", later));
        }
        assertFalse(rateLimiter.allowRequest("user1", "", later));
    }

    @Test
    public void debugMaxCapacityNeverExceeded() {
        System.out.println("=== Testing Max Capacity Never Exceeded ===");

        // First, consume all tokens
        for (int i = 0; i < capacity; i++) {
            boolean result = rateLimiter.allowRequest("user1", "", currentTimeStampInMs);
            System.out.println("Initial request " + (i+1) + ": " + result);
        }

        // Wait 10 seconds
        long later = currentTimeStampInMs + 10000;
        System.out.println("Waiting 10000ms. Time difference: " + (later - currentTimeStampInMs));
        System.out.println("Expected tokens to add: " + (10000 * (tokenFillRateDefault / 1000.0)));
        System.out.println("Tokens after refill (before cap): " + (0 + 10000 * (tokenFillRateDefault / 1000.0)));
        System.out.println("Tokens after Math.min with capacity: " + Math.min(capacity, 0 + 10000 * (tokenFillRateDefault / 1000.0)));

        // Should be able to make exactly 'capacity' requests
        for (int i = 0; i < capacity; i++) {
            boolean result = rateLimiter.allowRequest("user1", "", later);
            System.out.println("After wait request " + (i+1) + ": " + result);
            if (!result) {
                System.out.println("FAILED at request " + (i+1));
                break;
            }
        }

        // This should fail
        boolean shouldFail = rateLimiter.allowRequest("user1", "", later);
        System.out.println("Should fail: " + shouldFail);
    }

}