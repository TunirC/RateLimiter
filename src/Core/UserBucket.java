package Core;

import java.time.Instant;

public class UserBucket {
    private Instant windowStartTime;
    private int requestCount;

    public UserBucket(Instant windowStartTime, int requestCount) {
        this.windowStartTime = windowStartTime;
        this.requestCount = requestCount;
    }

    public Instant getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(Instant windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }
}
