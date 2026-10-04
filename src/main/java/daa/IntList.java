package daa;

/** Common operations used to benchmark the array and linked list fairly. */
public interface IntList {
    void add(int value);
    void add(int index, int value);
    int remove(int index);
    int get(int index);
    boolean contains(int value);
    int size();
    Metrics getMetrics();
}
