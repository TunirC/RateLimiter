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
    private final long windowSizeInMs;
    private KeyResolver keyResolver;
    Map<String, UserBucket> userRequests = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int requestLimit, long windowSizeInSeconds, KeyResolver resolver) {
        this.requestLimit = requestLimit;
        this.windowSizeInMs = windowSizeInSeconds * 1000L;
        this.keyResolver = resolver;
    }

    @Override
    public boolean allowRequest(String userId, String ip, long incomingTimestamp) {
        List<String> keys = keyResolver.resolveKey(userId, ip);
        AtomicBoolean requestAllowed = new AtomicBoolean(true);
        for (String key: keys) {
            long currentWindowStart = (incomingTimestamp / windowSizeInMs) * windowSizeInMs;
            Instant currentWindowStartInstant = Instant.ofEpochMilli(currentWindowStart);

            userRequests.compute(key, (id, bucket) -> {
                if (bucket != null && checkValidWindow(bucket.getWindowStartTime(), currentWindowStartInstant)) {
                    if (bucket.getRequestCount() >= requestLimit) {
                        requestAllowed.set(false);
                        return bucket;
                    }

                    bucket.setRequestCount(bucket.getRequestCount() + 1);
                    return bucket;
                } else {
                    return new UserBucket(currentWindowStartInstant, 1);
                }
            });

            if (!requestAllowed.get()) break;
        }

        return requestAllowed.get();
    }

    private boolean checkValidWindow(Instant bucketWindowStart, Instant currentWindowStart) {
        return bucketWindowStart.equals(currentWindowStart);
    }
}
