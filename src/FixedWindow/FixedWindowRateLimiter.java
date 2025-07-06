package FixedWindow;

import Core.RateLimiter;
import Core.Resolver.KeyResolver;
import Core.UserBucket;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FixedWindowRateLimiter implements RateLimiter {

    private final int requestLimit;
    private final long windowSizeInSeconds;
    private KeyResolver keyResolver;
    Map<String, UserBucket> userRequests = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int requestLimit, long windowSizeInSeconds, KeyResolver resolver) {
        this.requestLimit = requestLimit;
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.keyResolver = resolver;
    }

    @Override
    public boolean allowRequest(String userId, String ip, long incomingTimestamp) {
        List<String> keys = keyResolver.resolveKey(userId, ip);
        AtomicBoolean setLimit = new AtomicBoolean(true);

        for (String key: keys) {
            userRequests.compute(key, (id, bucket) -> {
                Instant requestInstance = Instant.ofEpochSecond(incomingTimestamp);
                if (bucket != null && checkValidWindow(bucket.getWindowStartTime(), requestInstance)) {
                    int updatedCount = bucket.getRequestCount() + 1;
                    if (updatedCount > requestLimit) setLimit.set(false);
                    bucket.setRequestCount(updatedCount);

                    return bucket;
                } else {
                    return new UserBucket(requestInstance, 1);
                }
            });
        }

        return setLimit.get();
    }

    private boolean checkValidWindow(Instant windowTime, Instant requestTime) {
        return Duration.between(windowTime, requestTime).getSeconds() < windowSizeInSeconds;
    }
}
