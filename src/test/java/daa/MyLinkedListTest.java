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

class MyLinkedListTest {
    @Test
    void newListIsEmptyAndSearchDoesNoWork() {
        MyLinkedList list = new MyLinkedList();

        assertEquals(0, list.size());
        assertFalse(list.contains(0));
        assertFalse(list.contains(Integer.MIN_VALUE));
        assertMetrics(list, 0L, 0L, 0L);
    }

    @Test
    void singletonCanBeReadRemovedAndReplaced() {
        MyLinkedList list = new MyLinkedList();
        list.add(0, 42);

        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
        assertTrue(list.contains(42));
        assertEquals(42, list.remove(0));
        assertEquals(0, list.size());
        assertFalse(list.contains(42));

        list.add(-7);
        list.add(9);
        assertContents(list, -7, 9);
        assertEquals(-7, list.remove(0));
        assertEquals(9, list.remove(0));
        list.add(0, 11);
        assertContents(list, 11);
    }

    @Test
    void duplicatesAndEveryIntValueAreStoredWithoutSentinels() {
        MyLinkedList list = populated(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0);

        assertContents(list, Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0);
        assertTrue(list.contains(Integer.MIN_VALUE));
        assertTrue(list.contains(Integer.MAX_VALUE));
        assertEquals(0, list.remove(1));
        assertTrue(list.contains(0));
        assertEquals(0, list.remove(2));
        assertFalse(list.contains(0));
        assertContents(list, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void insertsAtHeadMiddleAndSizePreserveOrder() {
        MyLinkedList list = populated(20, 40);

        list.add(0, 10);
        list.add(2, 30);
        list.add(list.size(), 50);
        list.add(60);

        assertContents(list, 10, 20, 30, 40, 50, 60);
    }

    @Test
    void removesHeadMiddleAndLastAndKeepsTailUsableForAppend() {
        MyLinkedList list = populated(10, 20, 30, 40, 50);

        assertEquals(10, list.remove(0));
        assertContents(list, 20, 30, 40, 50);
        assertEquals(30, list.remove(1));
        assertContents(list, 20, 40, 50);
        assertEquals(50, list.remove(list.size() - 1));
        list.add(60);
        assertContents(list, 20, 40, 60);
        assertFalse(list.contains(50));

        assertEquals(60, list.remove(2));
        assertEquals(40, list.remove(1));
        list.add(list.size(), 70);
        assertContents(list, 20, 70);
        assertEquals(20, list.remove(0));
        list.add(80);
        assertContents(list, 70, 80);
    }

    @Test
    void emptyListRejectsInvalidIndicesWithoutChangingCounters() {
        MyLinkedList list = new MyLinkedList();

        for (int index : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(index));
            assertThrows(IndexOutOfBoundsException.class, () -> list.remove(index));
        }
        for (int index : new int[] {Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> list.add(index, 99));
        }

        assertEquals(0, list.size());
        assertMetrics(list, 0L, 0L, 0L);
        list.add(1);
        assertContents(list, 1);
    }

    @Test
    void invalidIndicesPreserveContentsAndExistingCounters() {
        MyLinkedList list = populated(10, 20, 30, 40);
        list.add(50);
        list.get(2);
        list.contains(-1);
        assertMetrics(list, 7L, 2L, 5L);

        for (int index : new int[] {Integer.MIN_VALUE, -1, 5, 6, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(index));
            assertThrows(IndexOutOfBoundsException.class, () -> list.remove(index));
            assertMetrics(list, 7L, 2L, 5L);
        }
        for (int index : new int[] {Integer.MIN_VALUE, -1, 6, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> list.add(index, 99));
            assertMetrics(list, 7L, 2L, 5L);
        }

        assertContents(list, 10, 20, 30, 40, 50);
        list.add(60);
        assertContents(list, 10, 20, 30, 40, 50, 60);
    }

    @Test
    void appendUsesTailAndCountsTwoPersistentLinkAssignments() {
        MyLinkedList list = new MyLinkedList();

        list.add(10);
        assertMetrics(list, 0L, 2L, 0L);
        list.getMetrics().reset();
        list.add(20);
        assertMetrics(list, 0L, 2L, 0L);
        list.getMetrics().reset();
        list.add(list.size(), 30);
        assertMetrics(list, 0L, 2L, 0L);
        assertContents(list, 10, 20, 30);

        MyLinkedList indexedEmpty = new MyLinkedList();
        indexedEmpty.add(0, 40);
        assertMetrics(indexedEmpty, 0L, 2L, 0L);
        assertContents(indexedEmpty, 40);
    }

    @Test
    void indexedInsertionCountsTraversalAndBothLinkAssignments() {
        MyLinkedList list = populated(10, 20, 30, 40);

        list.add(0, 5);
        assertMetrics(list, 0L, 2L, 0L);
        assertContents(list, 5, 10, 20, 30, 40);

        list.getMetrics().reset();
        list.add(3, 25);
        assertMetrics(list, 3L, 2L, 0L);
        assertContents(list, 5, 10, 20, 25, 30, 40);

        list.getMetrics().reset();
        list.add(1, 7);
        assertMetrics(list, 1L, 2L, 0L);
        assertContents(list, 5, 7, 10, 20, 25, 30, 40);
    }

    @Test
    void getCountsNextReadsFromHeadIncludingForTheLastIndex() {
        MyLinkedList list = populated(10, 20, 30, 40);

        for (int index = 0; index < list.size(); index++) {
            list.getMetrics().reset();
            assertEquals((index + 1) * 10, list.get(index));
            assertMetrics(list, index, 0L, 0L);
        }
    }

    @Test
    void headRemovalCountsNextReadAndClearsTailForSingleton() {
        MyLinkedList list = populated(10, 20);

        assertEquals(10, list.remove(0));
        assertMetrics(list, 1L, 1L, 0L);
        assertContents(list, 20);

        list.getMetrics().reset();
        assertEquals(20, list.remove(0));
        assertMetrics(list, 1L, 2L, 0L);
        assertEquals(0, list.size());
        assertFalse(list.contains(20));
        list.add(30);
        list.add(40);
        assertContents(list, 30, 40);
    }

    @Test
    void interiorAndTailRemovalCountSuccessorReadsAndTailUpdates() {
        MyLinkedList list = populated(10, 20, 30, 40, 50);

        assertEquals(30, list.remove(2));
        assertMetrics(list, 3L, 1L, 0L);
        assertContents(list, 10, 20, 40, 50);

        list.getMetrics().reset();
        assertEquals(50, list.remove(3));
        assertMetrics(list, 4L, 2L, 0L);
        assertContents(list, 10, 20, 40);

        list.getMetrics().reset();
        assertEquals(20, list.remove(1));
        assertMetrics(list, 2L, 1L, 0L);
        assertContents(list, 10, 40);

        list.getMetrics().reset();
        assertEquals(40, list.remove(1));
        assertMetrics(list, 2L, 2L, 0L);
        list.add(60);
        assertContents(list, 10, 60);
    }

    @Test
    void searchStopsAtFirstMatchAndCountsFinalNullReadOnMiss() {
        MyLinkedList list = populated(7, 8, 7, 9);

        assertTrue(list.contains(7));
        assertMetrics(list, 0L, 0L, 1L);
        list.getMetrics().reset();
        assertTrue(list.contains(8));
        assertMetrics(list, 1L, 0L, 2L);
        list.getMetrics().reset();
        assertTrue(list.contains(9));
        assertMetrics(list, 3L, 0L, 4L);
        list.getMetrics().reset();
        assertFalse(list.contains(6));
        assertMetrics(list, 4L, 0L, 4L);
        assertContents(list, 7, 8, 7, 9);
    }

    @Test
    void resetPreservesContentsAndMetricsIdentity() {
        MyLinkedList list = populated(10, 20);
        Metrics metrics = list.getMetrics();
        list.add(30);
        assertTrue(list.contains(30));
        assertMetrics(list, 2L, 2L, 3L);

        metrics.reset();

        assertSame(metrics, list.getMetrics());
        assertMetrics(list, 0L, 0L, 0L);
        assertContents(list, 10, 20, 30);
        metrics.reset();
        list.add(40);
        assertMetrics(list, 0L, 2L, 0L);
        assertContents(list, 10, 20, 30, 40);
    }

    @Test
    void listsHaveIndependentStorageAndCounters() {
        MyLinkedList first = populated(10, 20);
        MyLinkedList second = populated(30, 40);
        assertNotSame(first.getMetrics(), second.getMetrics());

        first.add(0, 5);
        assertMetrics(first, 0L, 2L, 0L);
        assertMetrics(second, 0L, 0L, 0L);

        assertTrue(second.contains(40));
        first.getMetrics().reset();
        assertMetrics(first, 0L, 0L, 0L);
        assertMetrics(second, 1L, 0L, 2L);
        assertContents(first, 5, 10, 20);
        assertContents(second, 30, 40);
    }

    @Test
    void randomMixedOperationsMatchArrayList() {
        MyLinkedList actual = new MyLinkedList();
        ArrayList<Integer> expected = new ArrayList<>();
        Random random = new Random(42);

        for (int operation = 0; operation < 5_000; operation++) {
            int value = random.nextInt(101) - 50;
            int kind = random.nextInt(5);
            if (expected.size() >= 128 && kind < 2) {
                kind = 2;
            }
            switch (kind) {
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
            assertEquals(expected.size(), actual.size());
        }
        assertFalse(actual.contains(0));
        actual.add(99);
        actual.add(100);
        assertContents(actual, 99, 100);
    }

    private static MyLinkedList populated(int... values) {
        MyLinkedList list = new MyLinkedList();
        for (int value : values) {
            list.add(value);
        }
        list.getMetrics().reset();
        return list;
    }

    private static void assertContents(MyLinkedList list, int... expected) {
        assertEquals(expected.length, list.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], list.get(index), "Value at index " + index);
        }
    }

    private static void assertMetrics(MyLinkedList list, long steps, long moves, long comparisons) {
        Metrics metrics = list.getMetrics();
        assertEquals(steps, metrics.steps, "steps");
        assertEquals(moves, metrics.moves, "moves");
        assertEquals(comparisons, metrics.comparisons, "comparisons");
    }
}
