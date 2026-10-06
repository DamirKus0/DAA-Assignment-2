package daa;

public final class Metrics {
    public long steps;
    public long moves;
    public long comparisons;

    public void reset() {
        steps = 0L;
        moves = 0L;
        comparisons = 0L;
    }
}
