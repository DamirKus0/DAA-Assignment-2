package daa;

public final class MinHeap {
    private int[] elements = new int[10];
    private int size;
    private final Metrics metrics = new Metrics();

    public void insert(int value) {
        growIfFull();
        elements[size] = value;
        size++;
        siftUp(size - 1);
    }

    public int peekMin() {
        checkNotEmpty();
        int minimum = elements[0];
        metrics.steps++;
        return minimum;
    }

    public int extractMin() {
        checkNotEmpty();
        int minimum = elements[0];
        metrics.steps++;
        size--;

        if (size > 0) {
            int last = elements[size];
            metrics.steps++;
            elements[0] = last;
            metrics.moves++;
            siftDown(0);
        }
        return minimum;
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

    int valueAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
        int value = elements[index];
        metrics.steps++;
        return value;
    }

    private void checkNotEmpty() {
        if (size == 0) {
            throw new IllegalStateException("Heap is empty.");
        }
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            int currentValue = elements[index];
            metrics.steps++;
            int parentValue = elements[parent];
            metrics.steps++;
            metrics.comparisons++;
            if (currentValue >= parentValue) {
                return;
            }

            elements[index] = parentValue;
            metrics.moves++;
            elements[parent] = currentValue;
            metrics.moves++;
            index = parent;
        }
    }

    private void siftDown(int index) {
        while (index < size / 2) {
            int child = index * 2 + 1;
            int childValue = elements[child];
            metrics.steps++;
            int right = child + 1;
            if (right < size) {
                int rightValue = elements[right];
                metrics.steps++;
                metrics.comparisons++;
                if (rightValue < childValue) {
                    child = right;
                    childValue = rightValue;
                }
            }

            int currentValue = elements[index];
            metrics.steps++;
            metrics.comparisons++;
            if (currentValue <= childValue) {
                return;
            }

            elements[index] = childValue;
            metrics.moves++;
            elements[child] = currentValue;
            metrics.moves++;
            index = child;
        }
    }

    private void growIfFull() {
        if (size < elements.length) {
            return;
        }
        int newCapacity = elements.length * 2;
        if (newCapacity < elements.length) {
            throw new OutOfMemoryError("Heap capacity is too large to double.");
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
