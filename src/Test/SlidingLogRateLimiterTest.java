package Test;

import Core.Resolver.HybridKeyResolver;
import Core.Resolver.KeyResolver;
import SlidingWindowLog.SlidingLogRateLimiter;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class SlidingLogRateLimiterTest {

    private KeyResolver resolver;
    private long currentTimeStampInSecond;

    @Before
    public void setUp() throws Exception {
        resolver = new HybridKeyResolver();
        currentTimeStampInSecond = Instant.now().getEpochSecond();
    }

    @Test
    public void allowRequestWithinWindow() {
        SlidingLogRateLimiter rateLimiter = new SlidingLogRateLimiter(3, 60, resolver);

        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInSecond));
        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInSecond + 5));
        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInSecond + 7));
    }

    @Test
    public void rejectRequestOverLimit() {
        SlidingLogRateLimiter rateLimiter = new SlidingLogRateLimiter(3, 60, resolver);

        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 1));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 3));
        assertFalse(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 8));
    }

    @Test
    public void allowRequestAfterExceedingWindowLimit() {
        SlidingLogRateLimiter rateLimiter = new SlidingLogRateLimiter(4, 60, resolver);

        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 1));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 3));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 5));
        assertFalse(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 14));

        assertFalse(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 60));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 61));
    }

    @Test
    public void testMultipleUsersWithDifferentTimestamps() {
        SlidingLogRateLimiter rateLimiter = new SlidingLogRateLimiter(4, 60, resolver);

        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond));
        assertTrue(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 1));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 3));
        assertTrue(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 5));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 14));
        assertTrue(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 15));
        assertTrue(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 16));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 20));

        assertFalse(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 25));
        assertFalse(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 28));
        assertFalse(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 29));

        assertTrue(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 65));
        assertTrue(rateLimiter.allowRequest("User2", "", currentTimeStampInSecond + 77));
        assertTrue(rateLimiter.allowRequest("User1", "", currentTimeStampInSecond + 80));
    }

    @Test
    public void testMultipleUsersWithHybridLimit() {
        SlidingLogRateLimiter rateLimiter = new SlidingLogRateLimiter(3, 60, resolver);

        assertTrue(rateLimiter.allowRequest("user1", "192.168.1.1", currentTimeStampInSecond));
        assertTrue(rateLimiter.allowRequest("user1", "192.168.1.1", currentTimeStampInSecond + 1));
        assertTrue(rateLimiter.allowRequest("user2", "192.168.1.1", currentTimeStampInSecond + 2));
        assertFalse(rateLimiter.allowRequest("user3", "192.168.1.1", currentTimeStampInSecond + 3));

        assertTrue(rateLimiter.allowRequest("user3", "192.168.1.1", currentTimeStampInSecond + 62));
    }


}