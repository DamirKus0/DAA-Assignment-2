package daa;

public final class DynamicArray implements IntList {
    private int[] elements = new int[10];
    private int size;
    private final Metrics metrics = new Metrics();

    public void add(int value) {
        add(size, value);
    }

    public void add(int index, int value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
        growIfFull();

        for (int i = size; i > index; i--) {
            int shifted = elements[i - 1];
            metrics.steps++;
            elements[i] = shifted;
            metrics.moves++;
        }
        elements[index] = value;
        size++;
    }

    public int remove(int index) {
        checkIndex(index);
        int removed = elements[index];
        metrics.steps++;

        for (int i = index; i < size - 1; i++) {
            int shifted = elements[i + 1];
            metrics.steps++;
            elements[i] = shifted;
            metrics.moves++;
        }
        size--;
        return removed;
    }

    public int get(int index) {
        checkIndex(index);
        int value = elements[index];
        metrics.steps++;
        return value;
    }

    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            int current = elements[i];
            metrics.steps++;
            metrics.comparisons++;
            if (current == value) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
    }

    private void growIfFull() {
        if (size < elements.length) {
            return;
        }
        int newCapacity = elements.length * 2;
        if (newCapacity < elements.length) {
            throw new OutOfMemoryError("Array capacity is too large to double.");
        }
        int[] expanded = new int[newCapacity];
        for (int i = 0; i < size; i++) {
            int copied = elements[i];
            metrics.steps++;
            expanded[i] = copied;
            metrics.moves++;
        }
        elements = expanded;
    }
}
