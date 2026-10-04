package daa;

/** Counts the physical operations performed by one data structure. */
public final class Metrics {
    public long steps;
    public long moves;
    public long comparisons;

    /** Starts a new measurement without replacing this counter object. */
    public void reset() {
        steps = 0L;
        moves = 0L;
        comparisons = 0L;
    }
}
