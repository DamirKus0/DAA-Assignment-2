package daa;

/** A singly linked list with a tail reference for constant-time append. */
public final class MyLinkedList {
    private static final class Node {
        private final int value;
        private Node next;

        private Node(int value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;
    private final Metrics metrics = new Metrics();

    public void add(int value) {
        Node added = new Node(value);
        if (size == 0) {
            head = added;
            metrics.moves++;
        } else {
            tail.next = added;
            metrics.moves++;
        }
        tail = added;
        metrics.moves++;
        size++;
    }

    public void add(int index, int value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
        if (index == size) {
            add(value);
            return;
        }

        Node added = new Node(value);
        if (index == 0) {
            added.next = head;
            metrics.moves++;
            head = added;
            metrics.moves++;
        } else {
            Node previous = nodeAt(index - 1);
            Node next = previous.next;
            metrics.steps++;
            added.next = next;
            metrics.moves++;
            previous.next = added;
            metrics.moves++;
        }
        size++;
    }

    public int remove(int index) {
        checkIndex(index);
        Node removed;
        if (index == 0) {
            removed = head;
            Node next = removed.next;
            metrics.steps++;
            head = next;
            metrics.moves++;
            if (size == 1) {
                tail = null;
                metrics.moves++;
            }
        } else {
            Node previous = nodeAt(index - 1);
            removed = previous.next;
            metrics.steps++;
            Node next = removed.next;
            metrics.steps++;
            previous.next = next;
            metrics.moves++;
            if (index == size - 1) {
                tail = previous;
                metrics.moves++;
            }
        }
        size--;
        return removed.value;
    }

    public int get(int index) {
        checkIndex(index);
        return nodeAt(index).value;
    }

    public boolean contains(int value) {
        Node current = head;
        while (current != null) {
            metrics.comparisons++;
            if (current.value == value) {
                return true;
            }
            current = current.next;
            metrics.steps++;
        }
        return false;
    }

    public int size() {
        return size;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
    }

    private Node nodeAt(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
            metrics.steps++;
        }
        return current;
    }
}
