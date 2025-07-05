package core;

import java.time.LocalDateTime;

public class UserBucket {
    private LocalDateTime windowStartTime;
    private int requestCount;

    public UserBucket(LocalDateTime windowStartTime, int requestCount) {
        this.windowStartTime = windowStartTime;
        this.requestCount = requestCount;
    }

    public LocalDateTime getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(LocalDateTime windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }
}
