package TokenBucket;

public final class TokenBucket {
    private final double tokens;
    private final long lastRefillTimestamp;

    public TokenBucket(double tokens, long lastRefillTimestamp) {
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }

    public double getTokens() {
        return tokens;
    }

    public long getLastRefillTimestamp() {
        return lastRefillTimestamp;
    }

    @Override
    public String toString() {
        return "TokenBucket{" +
                "tokens=" + tokens +
                ", lastRefillTimestamp=" + lastRefillTimestamp +
                '}';
    }
}
