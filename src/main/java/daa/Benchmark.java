package daa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public final class Benchmark {
    static final int WARMUP_RUNS = 2;
    static final int MEASURED_RUNS = 5;
    static final int GET_QUERIES = 10_000;
    static final int SEARCH_QUERIES = 1_000;
    static final int EDIT_QUERIES = 1_000;
    static final String CSV_HEADER =
            "workload,variant,structure,n,time_s,steps,moves,comparisons";

    private static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    private static volatile long consumedResult;

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: daa.Benchmark [output.csv]");
        }
        Path output = Path.of(args.length == 0 ? "results/results.csv" : args[0]);
        run(output, SIZES);
        writeEnvironment(output.resolveSibling("benchmark-info.txt"));
        System.out.println("Saved " + output + " (time_s is in seconds).");
    }

    static void run(Path output, int[] sizes) throws IOException {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        String[] workloads = {"W1", "W2", "W3", "W3", "W4"};
        String[] variants = {"-", "-", "head", "middle", "-"};
        System.out.printf(Locale.ROOT, "Warm-up runs: %d; measured runs: %d; seed: 42%n",
                WARMUP_RUNS, MEASURED_RUNS);
        Inputs[] allInputs = new Inputs[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            allInputs[i] = createInputs(sizes[i]);
        }

        for (int phase = 0; phase < 2; phase++) {
            System.out.println(phase == 0 ? "Warming up all cases..." : "Measuring all cases...");
            for (int sizeIndex = 0; sizeIndex < sizes.length; sizeIndex++) {
                int n = sizes[sizeIndex];
                Inputs inputs = allInputs[sizeIndex];
                for (int scenario = 0; scenario < workloads.length; scenario++) {
                    String workload = workloads[scenario];
                    String variant = variants[scenario];
                    String[] structures = workload.equals("W4")
                            ? new String[] {"MinHeap"}
                            : new String[] {"DynamicArray", "MyLinkedList"};
                    for (String structure : structures) {
                        if (phase == 0) {
                            for (int repeat = 0; repeat < WARMUP_RUNS; repeat++) {
                                runOnce(workload, variant, structure, inputs);
                            }
                            continue;
                        }
                        Sample[] samples = new Sample[MEASURED_RUNS];
                        for (int repeat = 0; repeat < MEASURED_RUNS; repeat++) {
                            samples[repeat] = runOnce(workload, variant, structure, inputs);
                        }
                        Sample median = medianSample(samples);
                        csv.append(String.format(Locale.ROOT,
                                "%s,%s,%s,%d,%.9f,%d,%d,%d%n",
                                workload, variant, structure, n, median.timeSeconds(),
                                median.steps, median.moves, median.comparisons));
                        System.out.printf(Locale.ROOT, "%s %-6s %-12s n=%6d time_s=%.9f%n",
                                workload, variant, structure, n, median.timeSeconds());
                    }
                }
            }
        }

        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, csv.toString(), StandardCharsets.UTF_8);
    }

    static Inputs createInputs(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Size must be positive.");
        }
        Random random = new Random(42);
        int[] values = new int[n];
        long expectedValueSum = 0L;
        for (int i = 0; i < n; i++) {
            values[i] = random.nextInt(1_000_000);
            expectedValueSum += values[i];
        }

        int[] getIndices = new int[GET_QUERIES];
        long expectedGetSum = 0L;
        for (int i = 0; i < getIndices.length; i++) {
            getIndices[i] = random.nextInt(n);
            expectedGetSum += values[getIndices[i]];
        }

        int[] searchValues = new int[SEARCH_QUERIES];
        for (int i = 0; i < SEARCH_QUERIES / 2; i++) {
            searchValues[i] = values[random.nextInt(n)];
            searchValues[i + SEARCH_QUERIES / 2] = -1 - random.nextInt(1_000_000);
        }
        for (int i = searchValues.length - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            int saved = searchValues[i];
            searchValues[i] = searchValues[other];
            searchValues[other] = saved;
        }

        int[] insertValues = new int[EDIT_QUERIES];
        for (int i = 0; i < insertValues.length; i++) {
            insertValues[i] = random.nextInt(1_000_000);
        }
        return new Inputs(values, getIndices, searchValues, insertValues,
                expectedGetSum, expectedValueSum);
    }

    static Sample runOnce(String workload, String variant, String structure, Inputs inputs) {
        if (workload.equals("W4")) {
            if (!variant.equals("-") || !structure.equals("MinHeap")) {
                throw new IllegalArgumentException("W4 requires MinHeap and variant -.");
            }
            return measureHeap(inputs);
        }
        if (workload.equals("W3")) {
            if (!variant.equals("head") && !variant.equals("middle")) {
                throw new IllegalArgumentException("W3 requires head or middle.");
            }
        } else if ((!workload.equals("W1") && !workload.equals("W2"))
                || !variant.equals("-")) {
            throw new IllegalArgumentException("Unknown workload or variant.");
        }
        IntList list = switch (structure) {
            case "DynamicArray" -> new DynamicArray();
            case "MyLinkedList" -> new MyLinkedList();
            default -> throw new IllegalArgumentException("Unknown list structure.");
        };
        return measureList(workload, variant, list, inputs);
    }

    private static Sample measureList(String workload, String variant, IntList list, Inputs inputs) {
        for (int value : inputs.values) {
            list.add(value);
        }
        int index = variant.equals("middle") ? inputs.values.length / 2 : 0;
        int[] removed = workload.equals("W3") ? new int[EDIT_QUERIES] : null;
        long checksum = 0L;
        Metrics metrics = list.getMetrics();
        metrics.reset();

        long start = System.nanoTime();
        switch (workload) {
            case "W1" -> {
                for (int query : inputs.getIndices) {
                    checksum += list.get(query);
                }
            }
            case "W2" -> {
                for (int query : inputs.searchValues) {
                    if (list.contains(query)) {
                        checksum++;
                    }
                }
            }
            case "W3" -> {
                for (int value : inputs.insertValues) {
                    list.add(index, value);
                }
                for (int i = 0; i < EDIT_QUERIES; i++) {
                    removed[i] = list.remove(index);
                }
            }
            default -> throw new IllegalArgumentException("Unknown list workload.");
        }
        long elapsed = System.nanoTime() - start;
        Sample sample = new Sample(elapsed, metrics);

        if (workload.equals("W1") && checksum != inputs.expectedGetSum) {
            throw new IllegalStateException("W1 returned an incorrect checksum.");
        }
        if (workload.equals("W2") && checksum != SEARCH_QUERIES / 2) {
            throw new IllegalStateException("W2 did not find exactly 500 present queries.");
        }
        if (workload.equals("W3")) {
            for (int i = 0; i < EDIT_QUERIES; i++) {
                if (removed[i] != inputs.insertValues[EDIT_QUERIES - 1 - i]) {
                    throw new IllegalStateException("W3 removed an unexpected value.");
                }
                checksum += removed[i];
            }
            verifyRestoredContents(list, inputs.values);
        }
        consumedResult = checksum;
        return sample;
    }

    private static void verifyRestoredContents(IntList list, int[] expected) {
        if (list.size() != expected.length) {
            throw new IllegalStateException("W3 did not restore the original size.");
        }
        for (int i = 0; i < expected.length; i++) {
            int actual = list instanceof MyLinkedList ? list.remove(0) : list.get(i);
            if (actual != expected[i]) {
                throw new IllegalStateException("W3 did not restore the original values.");
            }
        }
    }

    private static Sample measureHeap(Inputs inputs) {
        MinHeap heap = new MinHeap();
        int[] extracted = new int[inputs.values.length];
        Metrics metrics = heap.getMetrics();
        metrics.reset();

        long start = System.nanoTime();
        for (int value : inputs.values) {
            heap.insert(value);
        }
        for (int i = 0; i < extracted.length; i++) {
            extracted[i] = heap.extractMin();
        }
        long elapsed = System.nanoTime() - start;
        Sample sample = new Sample(elapsed, metrics);

        long checksum = 0L;
        for (int i = 0; i < extracted.length; i++) {
            if (i > 0 && extracted[i - 1] > extracted[i]) {
                throw new IllegalStateException("W4 output is not in nondecreasing order.");
            }
            checksum += extracted[i];
        }
        if (heap.size() != 0 || checksum != inputs.expectedValueSum) {
            throw new IllegalStateException("W4 checksum or final size is incorrect.");
        }
        consumedResult = checksum;
        return sample;
    }

    static Sample medianSample(Sample[] samples) {
        if (samples.length != MEASURED_RUNS) {
            throw new IllegalArgumentException("Exactly five measured samples are required.");
        }
        Sample[] ordered = samples.clone();
        Sample first = samples[0];
        for (Sample sample : samples) {
            if (sample.steps != first.steps || sample.moves != first.moves
                    || sample.comparisons != first.comparisons) {
                throw new IllegalStateException("Operation counts differ between repeats.");
            }
        }
        for (int i = 1; i < ordered.length; i++) {
            Sample value = ordered[i];
            int j = i - 1;
            while (j >= 0 && ordered[j].elapsedNanos > value.elapsedNanos) {
                ordered[j + 1] = ordered[j];
                j--;
            }
            ordered[j + 1] = value;
        }
        return ordered[ordered.length / 2];
    }

    private static void writeEnvironment(Path output) throws IOException {
        String info = "java_version=" + System.getProperty("java.version") + "\n"
                + "java_vm=" + System.getProperty("java.vm.name") + "\n"
                + "java_vendor=" + System.getProperty("java.vendor") + "\n"
                + "os_name=" + System.getProperty("os.name") + "\n"
                + "os_version=" + System.getProperty("os.version") + "\n"
                + "os_arch=" + System.getProperty("os.arch") + "\n"
                + "available_processors=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "max_heap_bytes=" + Runtime.getRuntime().maxMemory() + "\n"
                + "seed=42\nwarmup_runs=2\nmeasured_runs=5\ntime_unit=s\n"
                + "warmup_order=all cases before any retained measurement\n"
                + "data_range=0..999999\n"
                + "w3_order=1000 insertions, then 1000 removals at the same index\n"
                + "w3_middle_index=initial n / 2\n"
                + "validation=after the timer and counter snapshot\n";
        Files.writeString(output, info, StandardCharsets.UTF_8);
    }

    static final class Inputs {
        final int[] values;
        final int[] getIndices;
        final int[] searchValues;
        final int[] insertValues;
        final long expectedGetSum;
        final long expectedValueSum;

        Inputs(int[] values, int[] getIndices, int[] searchValues, int[] insertValues,
                long expectedGetSum, long expectedValueSum) {
            this.values = values;
            this.getIndices = getIndices;
            this.searchValues = searchValues;
            this.insertValues = insertValues;
            this.expectedGetSum = expectedGetSum;
            this.expectedValueSum = expectedValueSum;
        }
    }

    static final class Sample {
        final long elapsedNanos;
        final long steps;
        final long moves;
        final long comparisons;

        Sample(long elapsedNanos, Metrics metrics) {
            if (elapsedNanos <= 0) {
                throw new IllegalArgumentException("Measured time must be positive.");
            }
            this.elapsedNanos = elapsedNanos;
            this.steps = metrics.steps;
            this.moves = metrics.moves;
            this.comparisons = metrics.comparisons;
        }

        double timeSeconds() {
            return elapsedNanos / 1_000_000_000.0;
        }
    }
}
