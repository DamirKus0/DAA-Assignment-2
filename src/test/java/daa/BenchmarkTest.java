package daa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void inputsAreReproducibleAndUseIndependentArrays() {
        Benchmark.Inputs first = Benchmark.createInputs(32);
        Benchmark.Inputs second = Benchmark.createInputs(32);

        assertArrayEquals(first.values, second.values);
        assertArrayEquals(first.getIndices, second.getIndices);
        assertArrayEquals(first.searchValues, second.searchValues);
        assertArrayEquals(first.insertValues, second.insertValues);
        assertNotSame(first.values, second.values);
        assertNotSame(first.getIndices, second.getIndices);
        assertNotSame(first.searchValues, second.searchValues);
        assertNotSame(first.insertValues, second.insertValues);
        assertEquals(32, first.values.length);
        assertEquals(10_000, first.getIndices.length);
        assertEquals(1_000, first.searchValues.length);
        assertEquals(1_000, first.insertValues.length);
    }

    @Test
    void queriesAreValidWithExactlyHalfSuccessfulSearchesAndCorrectChecksums() {
        Benchmark.Inputs inputs = Benchmark.createInputs(32);
        long expectedValueSum = 0L;
        for (int value : inputs.values) {
            assertTrue(value >= 0 && value < 1_000_000);
            expectedValueSum += value;
        }
        long expectedGetSum = 0L;
        for (int index : inputs.getIndices) {
            assertTrue(index >= 0 && index < inputs.values.length);
            expectedGetSum += inputs.values[index];
        }

        int hits = 0;
        int misses = 0;
        for (int query : inputs.searchValues) {
            if (firstIndexOf(inputs.values, query) >= 0) {
                hits++;
            } else {
                assertTrue(query < 0, "Missing queries must be outside the input domain");
                misses++;
            }
        }
        assertEquals(500, hits);
        assertEquals(500, misses);
        assertEquals(expectedGetSum, inputs.expectedGetSum);
        assertEquals(expectedValueSum, inputs.expectedValueSum);
    }

    @Test
    void sampleSnapshotsLongCountersAndConvertsNanosecondsToSeconds() {
        Metrics metrics = new Metrics();
        metrics.steps = 3_000_000_001L;
        metrics.moves = 4_000_000_002L;
        metrics.comparisons = 5_000_000_003L;

        Benchmark.Sample sample = new Benchmark.Sample(1_250_000_000L, metrics);
        metrics.reset();
        metrics.steps = 17L;

        assertEquals(1_250_000_000L, sample.elapsedNanos);
        assertEquals(1.25, sample.timeSeconds());
        assertCounters(sample, 3_000_000_001L, 4_000_000_002L, 5_000_000_003L);
    }

    @Test
    void medianSelectsActualMiddleSampleWithoutReorderingInput() {
        Benchmark.Sample median = sample(5L);
        Benchmark.Sample[] samples = {
                sample(9L), sample(1L), sample(7L), sample(3L), median
        };
        Benchmark.Sample[] original = samples.clone();

        assertSame(median, Benchmark.medianSample(samples));
        assertArrayEquals(original, samples);
        assertEquals(5L, Benchmark.medianSample(new Benchmark.Sample[] {
                sample(9L), sample(5L), sample(5L), sample(1L), sample(5L)
        }).elapsedNanos);
    }

    @Test
    void medianRejectsWrongRepeatCountOrInconsistentCounters() {
        assertThrows(IllegalArgumentException.class, () -> Benchmark.medianSample(
                new Benchmark.Sample[] {sample(1L), sample(2L), sample(3L), sample(4L)}));
        assertThrows(IllegalArgumentException.class, () -> Benchmark.medianSample(
                new Benchmark.Sample[] {
                        sample(1L), sample(2L), sample(3L), sample(4L), sample(5L), sample(6L)
                }));

        for (int changedCounter = 0; changedCounter < 3; changedCounter++) {
            Metrics inconsistent = counters(10L, 20L, 30L);
            switch (changedCounter) {
                case 0 -> inconsistent.steps++;
                case 1 -> inconsistent.moves++;
                case 2 -> inconsistent.comparisons++;
                default -> throw new AssertionError("Unreachable counter");
            }
            Benchmark.Sample[] samples = {
                    sample(1L), sample(2L), sample(3L), sample(4L),
                    new Benchmark.Sample(5L, inconsistent)
            };
            assertThrows(IllegalStateException.class, () -> Benchmark.medianSample(samples));
        }
    }

    @Test
    void randomAccessCountsOnlyMeasuredQueriesForEachStructure() {
        Benchmark.Inputs inputs = Benchmark.createInputs(32);
        long listSteps = 0L;
        for (int index : inputs.getIndices) {
            listSteps += index;
        }

        assertCounters(Benchmark.runOnce("W1", "-", "DynamicArray", inputs),
                10_000L, 0L, 0L);
        assertCounters(Benchmark.runOnce("W1", "-", "MyLinkedList", inputs),
                listSteps, 0L, 0L);
    }

    @Test
    void searchCountersMatchTheFirstOccurrenceOfEveryQuery() {
        Benchmark.Inputs inputs = Benchmark.createInputs(32);
        long comparisons = 0L;
        long listSteps = 0L;
        for (int query : inputs.searchValues) {
            int index = firstIndexOf(inputs.values, query);
            comparisons += index < 0 ? inputs.values.length : index + 1L;
            listSteps += index < 0 ? inputs.values.length : index;
        }

        assertCounters(Benchmark.runOnce("W2", "-", "DynamicArray", inputs),
                comparisons, 0L, comparisons);
        assertCounters(Benchmark.runOnce("W2", "-", "MyLinkedList", inputs),
                listSteps, 0L, comparisons);
    }

    @Test
    void arrayEditsIncludeShiftsAndGrowthButExcludeSetupAndValidation() {
        for (int n : new int[] {8, 10}) {
            Benchmark.Inputs inputs = Benchmark.createInputs(n);
            for (String variant : new String[] {"head", "middle"}) {
                int index = variant.equals("head") ? 0 : n / 2;
                // Capacities 10 through 640 are copied before reaching capacity 1,280.
                long growthCopies = 10L + 20L + 40L + 80L + 160L + 320L + 640L;
                // 1,000 insertions and removals at a fixed index have 999,000 extra shifts.
                long moves = 2_000L * (n - index) + 999_000L + growthCopies;

                assertCounters(Benchmark.runOnce("W3", variant, "DynamicArray", inputs),
                        moves + 1_000L, moves, 0L);
            }
        }
    }

    @Test
    void listEditsKeepTheInitialMiddleIndexAndExcludeValidationTraversals() {
        for (int n : new int[] {8, 10}) {
            Benchmark.Inputs inputs = Benchmark.createInputs(n);
            assertCounters(Benchmark.runOnce("W3", "head", "MyLinkedList", inputs),
                    1_000L, 3_000L, 0L);
            assertCounters(Benchmark.runOnce("W3", "middle", "MyLinkedList", inputs),
                    1_000L * (2L * (n / 2) + 1L), 3_000L, 0L);
        }
    }

    @Test
    void heapWorkloadRepeatsFromFreshStateWithoutMutatingItsInput() {
        Benchmark.Inputs inputs = Benchmark.createInputs(32);
        int[] originalValues = inputs.values.clone();

        Benchmark.Sample first = Benchmark.runOnce("W4", "-", "MinHeap", inputs);
        Benchmark.Sample second = Benchmark.runOnce("W4", "-", "MinHeap", inputs);

        assertTrue(first.steps > inputs.values.length);
        assertTrue(first.moves > 0L);
        assertTrue(first.comparisons > 0L);
        assertTrue(first.elapsedNanos >= 0L);
        assertCounters(second, first.steps, first.moves, first.comparisons);
        assertArrayEquals(originalValues, inputs.values);
    }

    @Test
    void completeSmallRunWritesEveryCaseWithLocaleIndependentSeconds() throws IOException {
        Path output = temporaryDirectory.resolve("results.csv");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            Benchmark.run(output, new int[] {8});
        } finally {
            Locale.setDefault(previous);
        }

        List<String> rows = Files.readAllLines(output);
        assertEquals(10, rows.size());
        assertEquals("workload,variant,structure,n,time_s,steps,moves,comparisons", rows.get(0));
        Set<String> expectedCases = Set.of(
                "W1,-,DynamicArray", "W1,-,MyLinkedList",
                "W2,-,DynamicArray", "W2,-,MyLinkedList",
                "W3,head,DynamicArray", "W3,head,MyLinkedList",
                "W3,middle,DynamicArray", "W3,middle,MyLinkedList",
                "W4,-,MinHeap");
        Set<String> actualCases = new HashSet<>();
        for (String row : rows.subList(1, rows.size())) {
            String[] fields = row.split(",", -1);
            assertEquals(8, fields.length);
            assertTrue(actualCases.add(String.join(",", Arrays.copyOf(fields, 3))),
                    "Every workload/variant/structure combination must occur once");
            assertEquals("8", fields[3]);
            assertTrue(fields[4].matches("[0-9]+\\.[0-9]{9}"), "Seconds: " + fields[4]);
            assertTrue(Double.parseDouble(fields[4]) > 0.0);
            for (int field = 5; field < 8; field++) {
                assertTrue(fields[field].matches("[0-9]+"));
                assertTrue(Long.parseLong(fields[field]) >= 0L);
            }
        }
        assertEquals(expectedCases, actualCases);
    }

    private static int firstIndexOf(int[] values, int query) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == query) {
                return index;
            }
        }
        return -1;
    }

    private static Benchmark.Sample sample(long elapsedNanos) {
        return new Benchmark.Sample(elapsedNanos, counters(10L, 20L, 30L));
    }

    private static Metrics counters(long steps, long moves, long comparisons) {
        Metrics metrics = new Metrics();
        metrics.steps = steps;
        metrics.moves = moves;
        metrics.comparisons = comparisons;
        return metrics;
    }

    private static void assertCounters(Benchmark.Sample sample,
                                       long steps, long moves, long comparisons) {
        assertEquals(steps, sample.steps, "steps");
        assertEquals(moves, sample.moves, "moves");
        assertEquals(comparisons, sample.comparisons, "comparisons");
    }
}
