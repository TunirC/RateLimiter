package SlidingWindowCounter;

import Core.RateLimiter;
import Core.Resolver.KeyResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SlidingWindowCounterRateLimiter implements RateLimiter {
    private final int requestLimit;
    private final long windowSizeInMs;
    private KeyResolver resolver;

    private Map<String, SlidingCounterUserBucket> userBucketMap = new ConcurrentHashMap<>();

    public SlidingWindowCounterRateLimiter(int requestLimit, int windowSizeInSeconds, KeyResolver resolver) {
        this.requestLimit = requestLimit;
        this.windowSizeInMs = windowSizeInSeconds * 1000L;
        this.resolver = resolver;
    }

    @Override
    public boolean allowRequest(String userId, String ip, long incomingTimestamp) {
        List<String> keys = resolver.resolveKey(userId, ip);
        AtomicBoolean allowed = new AtomicBoolean(true);

        for (String key : keys) {
            long currentWindowStart = (incomingTimestamp / windowSizeInMs) * windowSizeInMs;
            double progressInWindow = (double) (incomingTimestamp - currentWindowStart) / windowSizeInMs;

            userBucketMap.compute(key, (id, bucket) -> {
                if (bucket == null) {
                    bucket = new SlidingCounterUserBucket(currentWindowStart);
                }

                updateBucketForCurrentWindow(bucket, currentWindowStart);

                double estimatedRequests = (1 - progressInWindow) * bucket.getPrevWindowCounter() + bucket.getCurrentWindowCounter();

                if (estimatedRequests + 1 > requestLimit) {
                    allowed.set(false);
                    return bucket;
                }

                bucket.setCurrentWindowCounter(bucket.getCurrentWindowCounter() + 1);
                return bucket;
            });

            if (!allowed.get()) {
                break;
            }
        }

        return allowed.get();
    }

    private void updateBucketForCurrentWindow(SlidingCounterUserBucket bucket, long currentWindowStart) {
        if (bucket.getCurrentWindowSize() == currentWindowStart) {
            return;
        }

        long windowDiff = (currentWindowStart - bucket.getCurrentWindowSize()) / windowSizeInMs;

        if (windowDiff == 1) {
            bucket.setPrevWindowCounter(bucket.getCurrentWindowCounter());
            bucket.setCurrentWindowCounter(0);
        } else if (windowDiff > 1) {
            bucket.setPrevWindowCounter(0);
            bucket.setCurrentWindowCounter(0);
        }
        // If windowDiff < 1, this shouldn't happen with monotonic timestamps
        bucket.setCurrentWindowSize(currentWindowStart);
    }
}
