package SlidingWindowLog;

import Core.RateLimiter;
import Core.Resolver.KeyResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class SlidingLogRateLimiter implements RateLimiter {

    private final int requestLimit;
    private final long windowSizeInSeconds;
    private KeyResolver resolver;
    private final Map<String, ConcurrentLinkedDeque<Long>> userBucket = new ConcurrentHashMap<>();

    public SlidingLogRateLimiter(int requestLimit, long windowSizeInSeconds, KeyResolver resolver) {
        this.requestLimit = requestLimit;
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.resolver = resolver;
    }

    @Override
    public boolean allowRequest(String userId, String ip, long incomingTimestamp) {
        List<String> keys = resolver.resolveKey(userId, ip);
        AtomicBoolean setAllowRequest = new AtomicBoolean(true);


        for (String key: keys) {
            ConcurrentLinkedDeque<Long> timestampQueue = userBucket.computeIfAbsent(key, (id) -> new ConcurrentLinkedDeque<>());

            long offset = incomingTimestamp - windowSizeInSeconds;

            while (!timestampQueue.isEmpty() && timestampQueue.peekFirst() < offset) {
                timestampQueue.pollFirst();
            }

            if (timestampQueue.size() < requestLimit) {
                timestampQueue.offerLast(incomingTimestamp);
            } else {
                setAllowRequest.set(false);
            }
        }

        return setAllowRequest.get();
    }
}
