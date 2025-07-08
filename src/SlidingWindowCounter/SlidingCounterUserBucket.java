package SlidingWindowCounter;

public class SlidingCounterUserBucket {
    private long currentWindowSize;
    private int currentWindowCounter;
    private int prevWindowCounter;

    public SlidingCounterUserBucket(long currentWindowSize) {
        this.currentWindowSize = currentWindowSize;
        this.currentWindowCounter = 0;
        this.prevWindowCounter = 0;
    }

    public long getCurrentWindowSize() {
        return currentWindowSize;
    }

    public void setCurrentWindowSize(long currentWindowSize) {
        this.currentWindowSize = currentWindowSize;
    }

    public int getCurrentWindowCounter() {
        return currentWindowCounter;
    }

    public void setCurrentWindowCounter(int currentWindowCounter) {
        this.currentWindowCounter = currentWindowCounter;
    }

    public int getPrevWindowCounter() {
        return prevWindowCounter;
    }

    public void setPrevWindowCounter(int prevWindowCounter) {
        this.prevWindowCounter = prevWindowCounter;
    }
}
