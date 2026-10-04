package daa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsTest {
    @Test
    void resetClearsAllCountersAndAllowsReuse() {
        Metrics metrics = new Metrics();
        metrics.steps = 7L;
        metrics.moves = 3L;
        metrics.comparisons = 5L;

        metrics.reset();

        assertEquals(0L, metrics.steps);
        assertEquals(0L, metrics.moves);
        assertEquals(0L, metrics.comparisons);

        metrics.steps++;
        metrics.moves += 2L;
        metrics.comparisons++;

        assertEquals(1L, metrics.steps);
        assertEquals(2L, metrics.moves);
        assertEquals(1L, metrics.comparisons);
    }

    @Test
    void countersCanExceedTheIntRange() {
        Metrics metrics = new Metrics();
        metrics.steps = 2_147_483_647L;
        metrics.moves = 2_147_483_647L;
        metrics.comparisons = 2_147_483_647L;

        metrics.steps++;
        metrics.moves += 2L;
        metrics.comparisons += 3L;

        assertEquals(2_147_483_648L, metrics.steps);
        assertEquals(2_147_483_649L, metrics.moves);
        assertEquals(2_147_483_650L, metrics.comparisons);

        metrics.reset();

        assertEquals(0L, metrics.steps);
        assertEquals(0L, metrics.moves);
        assertEquals(0L, metrics.comparisons);
    }

    @Test
    void instancesHaveIndependentCounters() {
        Metrics first = new Metrics();
        Metrics second = new Metrics();

        first.steps++;
        first.moves += 2L;
        first.comparisons += 3L;

        assertEquals(0L, second.steps);
        assertEquals(0L, second.moves);
        assertEquals(0L, second.comparisons);

        second.steps = 4L;
        second.moves = 5L;
        second.comparisons = 6L;
        first.reset();

        assertEquals(4L, second.steps);
        assertEquals(5L, second.moves);
        assertEquals(6L, second.comparisons);
    }
}
