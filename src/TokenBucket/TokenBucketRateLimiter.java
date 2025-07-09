package TokenBucket;

import Core.RateLimiter;
import Core.Resolver.KeyResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TokenBucketRateLimiter implements RateLimiter {
    private final Map<String, TokenBucket> tokenBucketMap;
    private final KeyResolver resolver;
    private final double tokenRefillRate;
    private final int capacity;

    public TokenBucketRateLimiter(KeyResolver resolver, long tokenRefillRateInSec, int capacity) {
        this.resolver = resolver;
        this.tokenRefillRate = tokenRefillRateInSec / 1000.0;
        this.capacity = capacity;
        this.tokenBucketMap = new ConcurrentHashMap<>();
    }


    @Override
    public boolean allowRequest(String userId, String ip, long incomingRequestTimestamp) {
        List<String> keys = resolver.resolveKey(userId, ip);
        AtomicBoolean isTokenAvailable = new AtomicBoolean(true);

        for (String key: keys) {
            tokenBucketMap.compute(key, (id, bucket) -> {
                double newTokenCount;
                long timeStampForToken;

                if (bucket == null) {
                    newTokenCount = capacity;
                    timeStampForToken = incomingRequestTimestamp;
                } else if (incomingRequestTimestamp < bucket.getLastRefillTimestamp()) {
                    newTokenCount = bucket.getTokens();
                    timeStampForToken = bucket.getLastRefillTimestamp();
                } else {
                    double requiredTokensToAdd = (incomingRequestTimestamp - bucket.getLastRefillTimestamp()) * tokenRefillRate;
                    newTokenCount = Math.min(capacity, bucket.getTokens() + requiredTokensToAdd);
                    timeStampForToken = incomingRequestTimestamp;
                }

                if (newTokenCount < 1) {
                    isTokenAvailable.set(false);
                    return bucket;
                }

                return new TokenBucket(newTokenCount - 1, timeStampForToken);
            });

            if (!isTokenAvailable.get()) break;
        }

        return isTokenAvailable.get();
    }
}
