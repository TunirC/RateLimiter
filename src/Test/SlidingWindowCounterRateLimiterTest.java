package Test;

import Core.Resolver.HybridKeyResolver;
import Core.Resolver.KeyResolver;
import SlidingWindowCounter.SlidingWindowCounterRateLimiter;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;
public class SlidingWindowCounterRateLimiterTest {
    private KeyResolver resolver;
    private long currentTimestampInMs;
    private final int requestLimit = 5;
    private final int windowSizeInSeconds = 60;

    @Before
    public void setUp() throws Exception {
        resolver = new HybridKeyResolver();
        currentTimestampInMs = Instant.now().toEpochMilli();
    }


    @Test
    public void allowRequestsWithinLimit() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);
        for (int i = 0; i < requestLimit; i++) {
            assertTrue("Request " + i + " should be allowed",
                    limiter.allowRequest("user1", "", currentTimestampInMs));
        }
    }

    @Test
    public void rejectRequestAfterLimitExceeded() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);
        for (int i = 0; i < requestLimit; i++) {
            assertTrue(limiter.allowRequest("user1", "", currentTimestampInMs));
        }
        assertFalse(limiter.allowRequest("user1", "", currentTimestampInMs));
    }

    @Test
    public void allowAfterWindowShift() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);
        for (int i = 0; i < requestLimit; i++) {
            assertTrue(limiter.allowRequest("user1", "", currentTimestampInMs));
        }

        long futureTimestamp = currentTimestampInMs + (windowSizeInSeconds * 1000L * 2); // 2 windows ahead
        assertTrue("Request should be allowed after complete window shift", limiter.allowRequest("user1", "", futureTimestamp));
    }

    @Test
    public void testIpBasedLimiter() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(2, windowSizeInSeconds, resolver);
        assertTrue(limiter.allowRequest("", "192.168.0.1", currentTimestampInMs));
        assertTrue(limiter.allowRequest("", "192.168.0.1", currentTimestampInMs));
        assertFalse(limiter.allowRequest("", "192.168.0.1", currentTimestampInMs));
    }

    @Test
    public void testMultipleUsers() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);
        for (int i = 0; i < requestLimit; i++) {
            assertTrue(limiter.allowRequest("user1", "", currentTimestampInMs));
            assertTrue(limiter.allowRequest("user2", "", currentTimestampInMs));
        }

        assertFalse("User1 should be at limit",
                limiter.allowRequest("user1", "", currentTimestampInMs));
        assertFalse("User2 should be at limit",
                limiter.allowRequest("user2", "", currentTimestampInMs));
    }

    @Test
    public void testSlidingWindowBehavior() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);

        long nearEndOfWindow = currentTimestampInMs + (windowSizeInSeconds * 1000L) - 1000; // 1 second before window end
        for (int i = 0; i < requestLimit; i++) {
            assertTrue("Request " + i + " should be allowed",
                    limiter.allowRequest("user1", "", nearEndOfWindow));
        }

        long justIntoNextWindow = currentTimestampInMs + (windowSizeInSeconds * 1000L) + 1000; // 1 second into next window
        assertFalse("Request should be rejected due to sliding window overlap",
                limiter.allowRequest("user1", "", justIntoNextWindow));
    }

    @Test
    public void testGradualWindowSliding() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.allowRequest("user1", "", currentTimestampInMs));
        }

        long halfwayThrough = currentTimestampInMs + (windowSizeInSeconds * 1000L / 2);
        for (int i = 0; i < 2; i++) {
            assertTrue("Request " + i + " should be allowed",
                    limiter.allowRequest("user1", "", halfwayThrough));
        }

        assertFalse("Request should be rejected - at limit",
                limiter.allowRequest("user1", "", halfwayThrough));
    }

    @Test
    public void testEdgeCaseAtWindowBoundary() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(requestLimit, windowSizeInSeconds, resolver);

        long windowSizeInMs = windowSizeInSeconds * 1000L;
        long windowStart = (currentTimestampInMs / windowSizeInMs) * windowSizeInMs;
        long windowEnd = windowStart + windowSizeInMs;

        for (int i = 0; i < requestLimit; i++) {
            assertTrue(limiter.allowRequest("user1", "", windowEnd - 1));
        }

        assertFalse("Request at window boundary should be rejected",
                limiter.allowRequest("user1", "", windowEnd));
    }

    @Test
    public void testWeightCalculationAccuracy() {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(10, 10, resolver);

        long windowSizeInMs = 10 * 1000L;
        long windowStart = (currentTimestampInMs / windowSizeInMs) * windowSizeInMs;

        long previousWindowEnd = windowStart - 1;
        for (int i = 0; i < 10; i++) {
            assertTrue("Previous window request " + i + " should be allowed",
                    limiter.allowRequest("user1", "", previousWindowEnd));
        }

        // Test at the very start of current window (weight = 0, so full previous window counts)
        // Estimated requests = (1 - 0) * 10 + 0 = 10
        // Adding 1 more would make it 11 > 10, so should be rejected
        assertFalse("Request at start of window should be rejected (full previous window weight)",
                limiter.allowRequest("user1", "", windowStart));

        // Test at 50% through current window
        long halfwayPoint = windowStart + (windowSizeInMs / 2);
        // At this point, weight = 0.5
        // Estimated requests = (1 - 0.5) * 10 + 0 = 5
        // Adding 1 more would make it 6 < 10, so should be allowed
        assertTrue("Request at 50% through window should be allowed",
                limiter.allowRequest("user1", "", halfwayPoint));

        // Now current window has 1 request, try another at 50%
        // Estimated requests = (1 - 0.5) * 10 + 1 = 6
        // Adding 1 more would make it 7 < 10, so should be allowed
        assertTrue("Second request at 50% through window should be allowed",
                limiter.allowRequest("user1", "", halfwayPoint));

        // Test at 90% through current window
        long nearEndPoint = windowStart + (windowSizeInMs * 9 / 10);
        // At this point, weight = 0.9
        // Estimated requests = (1 - 0.9) * 10 + 2 = 1 + 2 = 3
        // We should be able to make several more requests
        for (int i = 0; i < 7; i++) {
            assertTrue("Request " + i + " at 90% through window should be allowed",
                    limiter.allowRequest("user1", "", nearEndPoint));
        }

        // Now current window has 9 requests (2 + 7)
        // Estimated requests = (1 - 0.9) * 10 + 9 = 1 + 9 = 10
        // Adding 1 more would make it 11 > 10, so should be rejected
        assertFalse("Request exceeding weighted limit should be rejected",
                limiter.allowRequest("user1", "", nearEndPoint));
    }

    @Test
    public void testWeightCalculationWithPartialPreviousWindow() {
        int testLimit = 6;
        int testWindowSize = 10; // 10 seconds
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(testLimit, testWindowSize, resolver);

        long windowSizeInMs = testWindowSize * 1000L;
        long windowStart = (currentTimestampInMs / windowSizeInMs) * windowSizeInMs;

        // Make only 3 requests in previous window (not at capacity)
        long previousWindowEnd = windowStart - 1;
        for (int i = 0; i < 3; i++) {
            assertTrue("Previous window request " + i + " should be allowed",
                    limiter.allowRequest("user1", "", previousWindowEnd));
        }

        // Test at 25% through current window
        long quarterPoint = windowStart + (windowSizeInMs / 4);
        // At this point, weight = 0.25
        // Estimated requests = (1 - 0.25) * 3 + 0 = 0.75 * 3 = 2.25
        // Adding 1 more would make it 3.25 < 6, so should be allowed
        assertTrue("Request at 25% through window should be allowed",
                limiter.allowRequest("user1", "", quarterPoint));

        // Make 2 more requests at quarter point
        for (int i = 0; i < 2; i++) {
            assertTrue("Additional request " + i + " at 25% should be allowed",
                    limiter.allowRequest("user1", "", quarterPoint));
        }

        // Now current window has 3 requests
        // Estimated requests = (1 - 0.25) * 3 + 3 = 2.25 + 3 = 5.25
        // Adding 1 more would make it 6.25 > 6, so should be rejected
        assertFalse("Request exceeding weighted limit should be rejected",
                limiter.allowRequest("user1", "", quarterPoint));
    }

}