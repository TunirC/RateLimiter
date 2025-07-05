package FixedWindow;

import core.RateLimiter;
import core.UserBucket;

import java.util.HashMap;
import java.util.Map;

public class FixedWindowRateLimiter implements RateLimiter {
    Map<String, UserBucket> userRequests = new HashMap<>();

    @Override
    public boolean allowRequest() {


        return false;
    }
}
