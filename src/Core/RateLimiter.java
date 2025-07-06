package Core;

public interface RateLimiter {
    boolean allowRequest(String userId, String ip, long incomingTimestamp);
}
