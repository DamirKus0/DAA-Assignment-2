package daa;

import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicArrayTest {
    @Test
    void newArrayIsEmptyAndSearchDoesNoWork() {
        DynamicArray array = new DynamicArray();

        assertEquals(0, array.size());
        assertEquals(10, array.capacity());
        assertFalse(array.contains(0));
        assertFalse(array.contains(Integer.MIN_VALUE));
        assertMetrics(array, 0L, 0L, 0L);
    }

    @Test
    void singletonCanBeReadRemovedAndReplaced() {
        DynamicArray array = new DynamicArray();
        array.add(0, 42);

        assertEquals(1, array.size());
        assertEquals(42, array.get(0));
        assertTrue(array.contains(42));
        assertEquals(42, array.remove(0));
        assertEquals(0, array.size());
        assertFalse(array.contains(42));

        array.add(-7);
        assertContents(array, -7);
        assertEquals(10, array.capacity());
    }

    @Test
    void duplicatesAndEveryIntValueAreStoredWithoutSentinels() {
        DynamicArray array = populated(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0);

        assertContents(array, Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0);
        assertTrue(array.contains(Integer.MIN_VALUE));
        assertTrue(array.contains(Integer.MAX_VALUE));
        assertEquals(0, array.remove(1));
        assertTrue(array.contains(0));
        assertEquals(0, array.remove(2));
        assertFalse(array.contains(0));
        assertContents(array, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void insertsAtHeadMiddleAndSizePreserveOrder() {
        DynamicArray array = populated(20, 40);

        array.add(0, 10);
        array.add(2, 30);
        array.add(array.size(), 50);

        assertContents(array, 10, 20, 30, 40, 50);
    }

    @Test
    void removesHeadMiddleAndLastAndReturnsTheirValues() {
        DynamicArray array = populated(10, 20, 30, 40, 50);

        assertEquals(10, array.remove(0));
        assertContents(array, 20, 30, 40, 50);
        assertEquals(30, array.remove(1));
        assertContents(array, 20, 40, 50);
        assertEquals(50, array.remove(array.size() - 1));
        assertContents(array, 20, 40);
        assertFalse(array.contains(50));
    }

    @Test
    void emptyArrayRejectsInvalidIndicesWithoutChangingCounters() {
        DynamicArray array = new DynamicArray();

        for (int index : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> array.get(index));
            assertThrows(IndexOutOfBoundsException.class, () -> array.remove(index));
        }
        for (int index : new int[] {Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> array.add(index, 99));
        }

        assertEquals(0, array.size());
        assertEquals(10, array.capacity());
        assertMetrics(array, 0L, 0L, 0L);
    }

    @Test
    void invalidIndicesOnFullArrayDoNotResizeMutateOrCountWork() {
        DynamicArray array = populated(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        array.get(0);
        array.contains(-1);

        for (int index : new int[] {Integer.MIN_VALUE, -1, 10, 11, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> array.get(index));
            assertThrows(IndexOutOfBoundsException.class, () -> array.remove(index));
            assertMetrics(array, 11L, 0L, 10L);
        }
        for (int index : new int[] {Integer.MIN_VALUE, -1, 11, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> array.add(index, 99));
            assertMetrics(array, 11L, 0L, 10L);
        }

        assertEquals(10, array.capacity());
        assertContents(array, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void capacityDoublesOnlyWhenFullAndDoesNotShrink() {
        DynamicArray array = new DynamicArray();
        int expectedCapacity = 10;

        for (int value = 0; value < 1_000; value++) {
            if (value == expectedCapacity) {
                expectedCapacity *= 2;
            }
            array.add(value);
            assertEquals(expectedCapacity, array.capacity());
            assertEquals(value + 1, array.size());
            for (int index = 0; index <= value; index++) {
                assertEquals(index, array.get(index));
            }
        }

        for (int value = 999; value >= 0; value--) {
            assertEquals(value, array.remove(array.size() - 1));
            assertEquals(expectedCapacity, array.capacity());
        }
        assertEquals(0, array.size());
    }

    @Test
    void appendCountsOnlyElementsCopiedDuringGrowth() {
        DynamicArray array = new DynamicArray();
        for (int value = 0; value < 10; value++) {
            array.add(value);
        }
        assertMetrics(array, 0L, 0L, 0L);

        array.add(10);
        assertEquals(20, array.capacity());
        assertMetrics(array, 10L, 10L, 0L);

        array.getMetrics().reset();
        array.add(11);
        assertMetrics(array, 0L, 0L, 0L);
        assertContents(array, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
    }

    @Test
    void indexedInsertCountsOnlyShiftedExistingElements() {
        DynamicArray array = populated(10, 20, 30, 40);

        array.add(1, 99);
        assertMetrics(array, 3L, 3L, 0L);
        assertContents(array, 10, 99, 20, 30, 40);

        array.getMetrics().reset();
        array.add(array.size(), 50);
        assertMetrics(array, 0L, 0L, 0L);
        assertContents(array, 10, 99, 20, 30, 40, 50);
    }

    @Test
    void indexedInsertDuringGrowthCountsBothCopyingAndShifting() {
        DynamicArray array = populated(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        array.add(3, 99);

        assertEquals(20, array.capacity());
        assertMetrics(array, 17L, 17L, 0L);
        assertContents(array, 0, 1, 2, 99, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void getCountsOneReadAndRemoveCountsReturnedValueAndShiftedSuffix() {
        DynamicArray array = populated(10, 20, 30, 40);

        assertEquals(30, array.get(2));
        assertMetrics(array, 1L, 0L, 0L);

        array.getMetrics().reset();
        assertEquals(20, array.remove(1));
        assertMetrics(array, 3L, 2L, 0L);
        assertContents(array, 10, 30, 40);

        array.getMetrics().reset();
        assertEquals(40, array.remove(2));
        assertMetrics(array, 1L, 0L, 0L);
        assertContents(array, 10, 30);
    }

    @Test
    void searchStopsAtFirstMatchAndResetPreservesContentsAndMetricsIdentity() {
        DynamicArray array = populated(7, 8, 7, 9);
        Metrics metrics = array.getMetrics();

        assertTrue(array.contains(7));
        assertMetrics(array, 1L, 0L, 1L);

        metrics.reset();
        assertSame(metrics, array.getMetrics());
        assertMetrics(array, 0L, 0L, 0L);
        assertEquals(4, array.size());
        assertEquals(10, array.capacity());
        assertTrue(array.contains(9));
        assertMetrics(array, 4L, 0L, 4L);

        metrics.reset();
        assertFalse(array.contains(6));
        assertMetrics(array, 4L, 0L, 4L);
        assertContents(array, 7, 8, 7, 9);
    }

    @Test
    void arraysHaveIndependentStorageAndCounters() {
        DynamicArray first = populated(10, 20);
        DynamicArray second = populated(30, 40);
        assertNotSame(first.getMetrics(), second.getMetrics());

        first.add(0, 5);
        assertMetrics(first, 2L, 2L, 0L);
        assertMetrics(second, 0L, 0L, 0L);

        assertTrue(second.contains(40));
        first.getMetrics().reset();
        assertMetrics(first, 0L, 0L, 0L);
        assertMetrics(second, 2L, 0L, 2L);
        assertContents(first, 5, 10, 20);
        assertContents(second, 30, 40);
    }

    @Test
    void randomMixedOperationsMatchArrayList() {
        DynamicArray actual = new DynamicArray();
        ArrayList<Integer> expected = new ArrayList<>();
        Random random = new Random(42);

        for (int operation = 0; operation < 5_000; operation++) {
            int value = random.nextInt(101) - 50;
            switch (random.nextInt(5)) {
                case 0 -> {
                    actual.add(value);
                    expected.add(value);
                }
                case 1 -> {
                    int index = random.nextInt(expected.size() + 1);
                    actual.add(index, value);
                    expected.add(index, value);
                }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        assertEquals(expected.remove(index).intValue(), actual.remove(index));
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        assertEquals(expected.get(index).intValue(), actual.get(index));
                    }
                }
                case 4 -> assertEquals(expected.contains(value), actual.contains(value));
                default -> throw new AssertionError("Unreachable operation");
            }

            assertEquals(expected.size(), actual.size(), "Size after operation " + operation);
            for (int index = 0; index < expected.size(); index++) {
                assertEquals(expected.get(index).intValue(), actual.get(index),
                        "Value after operation " + operation + " at index " + index);
            }
        }

        while (!expected.isEmpty()) {
            int index = random.nextInt(expected.size());
            assertEquals(expected.remove(index).intValue(), actual.remove(index));
        }
        assertEquals(0, actual.size());
        assertFalse(actual.contains(0));
    }

    private static DynamicArray populated(int... values) {
        DynamicArray array = new DynamicArray();
        for (int value : values) {
            array.add(value);
        }
        return array;
    }

    private static void assertContents(DynamicArray array, int... expected) {
        assertEquals(expected.length, array.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], array.get(index), "Value at index " + index);
        }
    }

    private static void assertMetrics(DynamicArray array, long steps, long moves, long comparisons) {
        Metrics metrics = array.getMetrics();
        assertEquals(steps, metrics.steps, "steps");
        assertEquals(moves, metrics.moves, "moves");
        assertEquals(comparisons, metrics.comparisons, "comparisons");
    }
}
