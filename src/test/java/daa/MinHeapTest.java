package daa;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinHeapTest {
    @Test
    void newHeapIsEmptyAndRejectsMinimumOperationsWithoutCountingWork() {
        MinHeap heap = new MinHeap();

        assertEquals(0, heap.size());
        assertEquals(10, heap.capacity());
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);
        assertHeapProperty(heap);
        assertMetrics(heap, 0L, 0L, 0L);
    }

    @Test
    void singletonCanBePeekedRemovedAndReplaced() {
        MinHeap heap = new MinHeap();
        heap.insert(42);
        assertEquals(1, heap.size());
        assertMetrics(heap, 0L, 0L, 0L);
        assertHeapProperty(heap);

        heap.getMetrics().reset();
        assertEquals(42, heap.peekMin());
        assertEquals(1, heap.size());
        assertMetrics(heap, 1L, 0L, 0L);

        heap.getMetrics().reset();
        assertEquals(42, heap.extractMin());
        assertMetrics(heap, 1L, 0L, 0L);
        assertHeapProperty(heap);
        assertEquals(0, heap.size());
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);
        assertMetrics(heap, 1L, 0L, 0L);

        heap.insert(-7);
        assertHeapProperty(heap);
        assertEquals(-7, heap.extractMin());
        assertHeapProperty(heap);
        assertEquals(0, heap.size());
        assertEquals(10, heap.capacity());
    }

    @Test
    void duplicatesAndIntExtremesAreExtractedWithTheirMultiplicities() {
        int[] input = {Integer.MAX_VALUE, 0, Integer.MIN_VALUE, -1,
                Integer.MAX_VALUE, 1, Integer.MIN_VALUE, 0, -1};
        MinHeap heap = populated(input);
        int[] expected = input.clone();
        Arrays.sort(expected);

        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], heap.peekMin());
            assertEquals(expected[index], heap.extractMin());
            assertHeapProperty(heap);
            assertEquals(expected.length - index - 1, heap.size());
        }
        assertThrows(IllegalStateException.class, heap::extractMin);
    }

    @Test
    void ascendingAndDescendingInputBothProduceSortedOutput() {
        for (boolean descending : new boolean[] {false, true}) {
            MinHeap heap = new MinHeap();
            for (int index = 0; index < 512; index++) {
                heap.insert(descending ? 511 - index : index);
                assertHeapProperty(heap);
                assertEquals(index + 1, heap.size());
            }
            for (int expected = 0; expected < 512; expected++) {
                assertEquals(expected, heap.extractMin());
                assertHeapProperty(heap);
            }
            assertEquals(0, heap.size());
        }
    }

    @Test
    void capacityDoublesOnlyWhenFullAndIsReusedWithoutShrinking() {
        MinHeap heap = new MinHeap();
        int expectedCapacity = 10;

        for (int value = 0; value < 257; value++) {
            if (value == expectedCapacity) {
                expectedCapacity *= 2;
            }
            heap.insert(value);
            assertHeapProperty(heap);
            assertEquals(expectedCapacity, heap.capacity());
            assertEquals(value + 1, heap.size());
        }
        for (int value = 0; value < 257; value++) {
            assertEquals(value, heap.extractMin());
            assertHeapProperty(heap);
            assertEquals(expectedCapacity, heap.capacity());
        }

        for (int value = 20; value >= 0; value--) {
            heap.insert(value);
            assertHeapProperty(heap);
            assertEquals(expectedCapacity, heap.capacity());
        }
        for (int value = 0; value <= 20; value++) {
            assertEquals(value, heap.extractMin());
            assertHeapProperty(heap);
        }
        assertEquals(0, heap.size());
    }

    @Test
    void insertionCountsOnlyReadsAndValueComparisonsWhenNoSwapIsNeeded() {
        MinHeap heap = new MinHeap();
        heap.insert(10);
        assertMetrics(heap, 0L, 0L, 0L);
        assertHeapProperty(heap);

        heap.getMetrics().reset();
        heap.insert(10);
        assertMetrics(heap, 2L, 0L, 1L);
        assertHeapProperty(heap);

        heap.getMetrics().reset();
        heap.insert(20);
        assertMetrics(heap, 2L, 0L, 1L);
        assertHeapProperty(heap);
        assertContents(heap, 10, 10, 20);
    }

    @Test
    void siftUpCountsEachSwapAndTheFinalUnsuccessfulComparison() {
        MinHeap reachesRoot = populated(1, 10, 5, 20, 15, 8, 7);
        reachesRoot.insert(0);
        assertMetrics(reachesRoot, 6L, 6L, 3L);
        assertHeapProperty(reachesRoot);
        assertContents(reachesRoot, 0, 1, 5, 10, 15, 8, 7, 20);

        MinHeap stopsBelowRoot = populated(1, 10, 5, 20, 15, 8, 7);
        stopsBelowRoot.insert(9);
        assertMetrics(stopsBelowRoot, 6L, 4L, 3L);
        assertHeapProperty(stopsBelowRoot);
        assertContents(stopsBelowRoot, 1, 9, 5, 10, 15, 8, 7, 20);
    }

    @Test
    void growthCountsCopiedElementsAsWellAsSiftUpWork() {
        MinHeap noSwap = populated(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        noSwap.insert(10);
        assertEquals(20, noSwap.capacity());
        assertMetrics(noSwap, 12L, 10L, 1L);
        assertHeapProperty(noSwap);

        MinHeap withSwaps = populated(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        withSwaps.insert(-1);
        assertEquals(20, withSwaps.capacity());
        assertMetrics(withSwaps, 16L, 16L, 3L);
        assertHeapProperty(withSwaps);
        assertEquals(-1, withSwaps.peekMin());
    }

    @Test
    void peekReadsOnlyTheRootAndDoesNotRemoveIt() {
        MinHeap heap = populated(9, 3, 7, 1, 5);

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertEquals(1, heap.peekMin());
            assertEquals(5, heap.size());
            assertMetrics(heap, attempt, 0L, 0L);
        }
        assertHeapProperty(heap);
        assertEquals(1, heap.extractMin());
        assertHeapProperty(heap);
        assertEquals(3, heap.peekMin());
    }

    @Test
    void extractionFromTwoElementsMovesTheLastElementWithoutComparisons() {
        MinHeap heap = populated(3, 7);

        assertEquals(3, heap.extractMin());
        assertMetrics(heap, 2L, 1L, 0L);
        assertHeapProperty(heap);
        assertContents(heap, 7);

        heap.getMetrics().reset();
        assertEquals(7, heap.extractMin());
        assertMetrics(heap, 1L, 0L, 0L);
        assertHeapProperty(heap);
        assertEquals(0, heap.size());
    }

    @Test
    void siftDownHandlesAParentWithOnlyALeftChild() {
        MinHeap heap = populated(1, 2, 3);

        assertEquals(1, heap.extractMin());
        assertMetrics(heap, 4L, 3L, 1L);
        assertHeapProperty(heap);
        assertContents(heap, 2, 3);
    }

    @Test
    void siftDownSelectsTheRightChildWhenItIsSmaller() {
        MinHeap heap = populated(1, 4, 2, 7, 5, 3);

        assertEquals(1, heap.extractMin());
        assertMetrics(heap, 5L, 3L, 2L);
        assertHeapProperty(heap);
        assertContents(heap, 2, 4, 3, 7, 5);
    }

    @Test
    void siftDownCountsComparisonsWhenChildrenAreEqual() {
        MinHeap heap = populated(1, 2, 2, 3, 4, 5, 6);

        assertEquals(1, heap.extractMin());
        assertMetrics(heap, 8L, 5L, 4L);
        assertHeapProperty(heap);
        assertContents(heap, 2, 3, 2, 6, 4, 5);
    }

    @Test
    void siftDownStopsWithoutSwappingWhenReplacementEqualsTheSmallerChild() {
        MinHeap heap = populated(1, 2, 2, 3, 4, 2);

        assertEquals(1, heap.extractMin());
        assertMetrics(heap, 5L, 1L, 2L);
        assertHeapProperty(heap);
        assertContents(heap, 2, 2, 2, 3, 4);
    }

    @Test
    void siftDownCanCrossSeveralLevels() {
        MinHeap heap = populated(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertEquals(1, heap.extractMin());
        assertMetrics(heap, 11L, 7L, 6L);
        assertHeapProperty(heap);
        assertContents(heap, 2, 4, 3, 8, 5, 6, 7, 10, 9);
    }

    @Test
    void resetPreservesContentsCapacityAndMetricsIdentity() {
        MinHeap heap = populated(4, 2, 6);
        Metrics metrics = heap.getMetrics();
        heap.insert(1);
        assertMetrics(heap, 4L, 4L, 2L);
        assertHeapProperty(heap);

        metrics.reset();

        assertSame(metrics, heap.getMetrics());
        assertMetrics(heap, 0L, 0L, 0L);
        assertEquals(4, heap.size());
        assertEquals(10, heap.capacity());
        assertContents(heap, 1, 2, 6, 4);
        metrics.reset();
        assertEquals(1, heap.peekMin());
        assertMetrics(heap, 1L, 0L, 0L);
    }

    @Test
    void heapsHaveIndependentStorageAndCounters() {
        MinHeap first = populated(10, 20);
        MinHeap second = populated(30, 40);
        assertNotSame(first.getMetrics(), second.getMetrics());

        first.insert(5);
        assertMetrics(first, 2L, 2L, 1L);
        assertMetrics(second, 0L, 0L, 0L);
        assertHeapProperty(first);

        assertEquals(30, second.peekMin());
        first.getMetrics().reset();
        assertMetrics(first, 0L, 0L, 0L);
        assertMetrics(second, 1L, 0L, 0L);
        assertContents(first, 5, 20, 10);
        assertContents(second, 30, 40);
    }

    @Test
    void diagnosticReadsOnlyActiveIndicesAndCountsSuccessfulReads() {
        MinHeap heap = populated(2, 4, 6);
        assertEquals(2, heap.valueAt(0));
        assertEquals(6, heap.valueAt(2));
        assertMetrics(heap, 2L, 0L, 0L);

        for (int index : new int[] {Integer.MIN_VALUE, -1, 3, 9, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> heap.valueAt(index));
            assertMetrics(heap, 2L, 0L, 0L);
        }
        assertEquals(2, heap.extractMin());
        assertHeapProperty(heap);
        heap.getMetrics().reset();
        assertThrows(IndexOutOfBoundsException.class, () -> heap.valueAt(2));
        assertMetrics(heap, 0L, 0L, 0L);
        assertContents(heap, 4, 6);
    }

    @Test
    void randomMixedOperationsMatchPriorityQueue() {
        MinHeap actual = new MinHeap();
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        Random random = new Random(42);

        for (int operation = 0; operation < 5_000; operation++) {
            int kind = random.nextInt(4);
            if (expected.size() >= 128 && kind < 2) {
                kind = 2;
            }
            switch (kind) {
                case 0, 1 -> {
                    int value = operation % 17 == 0 ? random.nextInt() : random.nextInt(101) - 50;
                    actual.insert(value);
                    expected.add(value);
                    assertHeapProperty(actual);
                }
                case 2 -> {
                    if (expected.isEmpty()) {
                        assertThrows(IllegalStateException.class, actual::extractMin);
                    } else {
                        assertEquals(expected.remove().intValue(), actual.extractMin());
                    }
                    assertHeapProperty(actual);
                }
                case 3 -> {
                    if (expected.isEmpty()) {
                        assertThrows(IllegalStateException.class, actual::peekMin);
                    } else {
                        assertEquals(expected.peek().intValue(), actual.peekMin());
                    }
                }
                default -> throw new AssertionError("Unreachable operation");
            }
            assertEquals(expected.size(), actual.size(), "Size after operation " + operation);
            if (!expected.isEmpty()) {
                assertEquals(expected.peek().intValue(), actual.peekMin(),
                        "Minimum after operation " + operation);
            }
        }

        while (!expected.isEmpty()) {
            assertEquals(expected.remove().intValue(), actual.extractMin());
            assertHeapProperty(actual);
            assertEquals(expected.size(), actual.size());
        }
        assertEquals(0, actual.size());
    }

    @Test
    void completeRandomExtractionIsNondecreasingAndPreservesDuplicateCounts() {
        MinHeap heap = new MinHeap();
        int[] occurrences = new int[51];
        Random random = new Random(42);
        for (int index = 0; index < 1_000; index++) {
            int value = random.nextInt(51) - 25;
            occurrences[value + 25]++;
            heap.insert(value);
            assertHeapProperty(heap);
        }

        int previous = Integer.MIN_VALUE;
        for (int remaining = 1_000; remaining > 0; remaining--) {
            int actual = heap.extractMin();
            assertHeapProperty(heap);
            assertTrue(previous <= actual, "Extraction order must be nondecreasing");
            assertTrue(actual >= -25 && actual <= 25);
            assertTrue(occurrences[actual + 25] > 0, "Extracted an unexpected occurrence");
            occurrences[actual + 25]--;
            assertEquals(remaining - 1, heap.size());
            previous = actual;
        }
        for (int count : occurrences) {
            assertEquals(0, count);
        }
        assertThrows(IllegalStateException.class, heap::extractMin);
    }

    private static MinHeap populated(int... values) {
        MinHeap heap = new MinHeap();
        for (int value : values) {
            heap.insert(value);
            assertHeapProperty(heap);
        }
        heap.getMetrics().reset();
        return heap;
    }

    private static void assertHeapProperty(MinHeap heap) {
        for (int child = 1; child < heap.size(); child++) {
            int parent = (child - 1) / 2;
            int parentValue = heap.valueAt(parent);
            int childValue = heap.valueAt(child);
            assertTrue(parentValue <= childValue,
                    "Heap order at parent " + parent + " and child " + child);
        }
    }

    private static void assertContents(MinHeap heap, int... expected) {
        assertEquals(expected.length, heap.size());
        int[] actual = new int[heap.size()];
        for (int index = 0; index < actual.length; index++) {
            actual[index] = heap.valueAt(index);
        }
        Arrays.sort(actual);
        Arrays.sort(expected);
        assertArrayEquals(expected, actual);
    }

    private static void assertMetrics(MinHeap heap, long steps, long moves, long comparisons) {
        Metrics metrics = heap.getMetrics();
        assertEquals(steps, metrics.steps, "steps");
        assertEquals(moves, metrics.moves, "moves");
        assertEquals(comparisons, metrics.comparisons, "comparisons");
    }
}
