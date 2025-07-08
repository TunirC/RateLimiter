package Test;

import Core.Resolver.HybridKeyResolver;
import Core.Resolver.KeyResolver;
import FixedWindow.FixedWindowRateLimiter;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FixedWindowRateLimiterTest {

    private KeyResolver resolver;
    private long currentTimeStampInMs;

    @Before
    public void setUp() throws Exception {
        resolver = new HybridKeyResolver();
        currentTimeStampInMs = Instant.now().toEpochMilli();
    }

    @Test
    public void allowRequestWithinLimit() {

        FixedWindowRateLimiter fixedWindowRateLimiter = new FixedWindowRateLimiter(5, 60, resolver);
        long now = Instant.now().toEpochMilli();

        for (int i = 0; i < 5; i++) {
            assertTrue(fixedWindowRateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        }
    }

    @Test
    public void rejectRequestOverLimit() {
        FixedWindowRateLimiter fixedWindowRateLimiter = new FixedWindowRateLimiter(5, 60, resolver);

        for (int i = 0; i < 5; i++) {
            fixedWindowRateLimiter.allowRequest("user1", "", currentTimeStampInMs);
        }

        assertFalse(fixedWindowRateLimiter.allowRequest("user1", "", currentTimeStampInMs));
    }

    @Test
    public void testNewWindowReset() {
        FixedWindowRateLimiter rateLimiter = new FixedWindowRateLimiter(2, 60, resolver);
        long now = Instant.now().toEpochMilli();

        assertTrue(rateLimiter.allowRequest("user1","", currentTimeStampInMs));
        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));
        assertFalse(rateLimiter.allowRequest("user1", "", currentTimeStampInMs));

        assertTrue(rateLimiter.allowRequest("user1", "", currentTimeStampInMs + 60000));
    }

    @Test
    public void testUserAndIPRateLimiting() {
        FixedWindowRateLimiter rateLimiter = new FixedWindowRateLimiter(3, 60, resolver);
        long now = Instant.now().toEpochMilli();

        assertTrue(rateLimiter.allowRequest("user1", "192.168.1.1", currentTimeStampInMs));
        assertTrue(rateLimiter.allowRequest("user1", "192.168.1.1", currentTimeStampInMs));
        assertTrue(rateLimiter.allowRequest("user1", "192.168.1.1", currentTimeStampInMs));

        assertFalse(rateLimiter.allowRequest("user1", "192.168.1.1", currentTimeStampInMs));
    }

    @Test
    public void testMultipleUsersWithSameIP() {
        FixedWindowRateLimiter fixedWindowRateLimiter = new FixedWindowRateLimiter(3, 60, resolver);
        long now = Instant.now().toEpochMilli();

        assertTrue(fixedWindowRateLimiter.allowRequest("user1", "192.168.0.10", currentTimeStampInMs));
        assertTrue(fixedWindowRateLimiter.allowRequest("user2", "192.168.0.10", currentTimeStampInMs));
        assertTrue(fixedWindowRateLimiter.allowRequest("user3", "192.168.0.10", currentTimeStampInMs));
        assertFalse(fixedWindowRateLimiter.allowRequest("user1", "192.168.0.10", currentTimeStampInMs));
    }
}