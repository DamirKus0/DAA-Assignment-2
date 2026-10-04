# DAA Assignment 2

A Java project for implementing and comparing a dynamic array, a linked list,
and a minimum heap.

## Requirements

- JDK 17 or later. The project targets Java 17.
- Internet access on the first build to download Maven and dependencies.

The Maven Wrapper is included, so a separate Maven installation is not required.

## Build and test

From the project directory on macOS or Linux:

```sh
./mvnw clean verify
```

To run tests without packaging:

```sh
./mvnw test
```

On Windows, use `mvnw.cmd clean verify` and `mvnw.cmd test`.

To use an installed JDK 17 on macOS:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## Run the benchmark

Build, run the tests, and reproduce all 36 rows in `results/results.csv` with:

```sh
./mvnw -Pbenchmark verify
```

On Windows, use `mvnw.cmd -Pbenchmark verify`. The `benchmark` Maven profile uses
the [Exec Maven Plugin](https://www.mojohaus.org/exec-maven-plugin/examples/example-exec-for-java-programs.html)
to start a separate Java process with `-Xms256m -Xmx256m`. The first run may need
to download the plugin. Runtime details are saved in `results/benchmark-info.txt`.

The program generates data with `new Random(42)` for each of the four sizes:
100, 1,000, 10,000, and 100,000. Stored values are in `0..999999`. One input object
per size contains the values and query sequences shared by both list structures
and all repeats. `IntList` declares their existing common methods, so both run
through the same workload code.

| Workload | Measured operations |
| --- | --- |
| W1 | 10,000 random `get(index)` calls. |
| W2 | 1,000 `contains(value)` calls: 500 values selected from the actual input and 500 negative values that cannot be present. This query array is shuffled once before the repeats. |
| W3, head | 1,000 insertions at index 0, followed by 1,000 removals at index 0. |
| W3, middle | 1,000 insertions at the original `n / 2`, followed by 1,000 removals at that same fixed index. The index does not change as the size changes. |
| W4 | Insert all `n` values into an empty heap, then extract all `n` values. |

First, the entire suite runs two warm-ups per case. Only after every case has
warmed up does the measured phase start, with five runs per case. This exercises
both structures and every workload before retaining any time measurements. Every
run creates a fresh structure. Input generation, output-buffer allocation, and
the initial filling for W1-W3 happen before timing. Counters are reset after
setup. W4 includes both insertion and extraction, including heap expansion.

The measured region includes the workload loops, calls to the structures,
operation-counter updates, and small bookkeeping operations: accumulating W1/W2
results and storing W3/W4 output values. Correctness checks happen after timing
and after copying the counters into a sample. W1 checks the returned sum; W2
checks that exactly 500 queries succeed. W3 checks reverse insertion order and
all original values. To keep that last check linear, it reads the array by index
and drains the linked list from its head. This validation does not enter the CSV
counts. W4 checks nondecreasing output, the sum, and the empty final heap; the
JUnit heap tests separately check exact values and duplicate counts.

Time is measured with `System.nanoTime()` and converted to seconds by dividing
the elapsed nanoseconds by `1_000_000_000.0`. The third of five sorted samples
provides the median time. The program checks that all five samples have identical
operation counts; each CSV row contains those counts for one measured run, not
their sum across the five runs. Warm-up results are discarded.

The CSV header is:

```text
workload,variant,structure,n,time_s,steps,moves,comparisons
```

Variants are `head` and `middle` for W3 and `-` otherwise. Times use a decimal
point and nine decimal places, preserving any positive nanosecond measurement.
The CSV is written only after every case completes successfully. Repeated runs
should reproduce operation counts; elapsed times can change with JVM compilation,
garbage collection, and other activity on the computer. Two warm-ups reduce
startup effects but do not guarantee that the JVM has reached a steady state.

## Project layout

- `src/main/java/`: data structures, operation counters, and benchmark.
- `src/test/java/`: JUnit 5 tests.
- `results/results.csv`: measured benchmark results.
- `results/benchmark-info.txt`: Java, operating system, and benchmark settings.
- `results/plots/`: PNG charts, added after collecting results.
- `REPORT.md`: analysis and measurements, added in the report stage.

## Operation counters

`daa.Metrics` holds three `long` counters. Each structure will own a separate
instance and update it inside its methods, next to the operation being counted.
Call `reset()` after setup and before each measured workload. Resetting counters
does not reset the contents of a structure.

| Counter | What counts as one operation |
| --- | --- |
| `steps` | Reading an array cell or a node's `next` link, including a link to `null`. |
| `moves` | Writing an existing element into a new array position, or explicitly assigning a stored list link (`head`, `tail`, or `next`). |
| `comparisons` | Comparing two element values, including a stored value against a search value. |

Counting rules for all structures:

- Read each array cell into a local variable when its value is needed. Count the
  actual read once; reusing that local value does not add another step.
- Copying one element during array growth counts as one step and one move.
- Shifting one element counts as one step and one move.
- Swapping two heap elements counts as two moves. Count the two cell reads where
  they occur; if the comparison already read both values into local variables,
  writing those cached values back does not add more steps.
- Moving the last heap element to the root counts as one step and one move.
- Initially storing a newly supplied value in an array is not a move. Later
  relocation of that stored element counts normally. Updating a local variable
  is not a move either.
- Explicit assignments to stored list links count as moves, including assignments
  of `null`. Each read of a `next` link counts as a step, including successor reads
  used to rewire links during insertion or removal. One action can therefore
  contribute to both counters. Reading `head` or `tail` alone is not a step.
- Comparing indices, sizes, capacities, or references does not count as an
  element comparison. Allocations, automatic zero initialization, and reading a
  node's `int` value do not add to these three counters.
- Failed input checks do not add counts. Only operations actually performed are
  counted; totals are not estimated from complexity formulas.

## DynamicArray

`daa.DynamicArray` stores primitive `int` values in an `int[]`. It starts with
capacity 10. The size is the number of stored values; the capacity is the number
of available array cells. When full, it allocates an array with twice the capacity
and copies the existing values. Removing values does not shrink the array.

| Method | Behavior |
| --- | --- |
| `add(value)` | Adds a value at the end. Amortized constant time; a growth operation copies all existing values. |
| `add(index, value)` | Allows indices from 0 through `size`, inclusive. Shifts the suffix right, starting at the end, then stores the new value. |
| `remove(index)` | Returns the removed value and shifts the following values left. |
| `get(index)` | Reads an existing value in constant time and counts exactly one step. |
| `contains(value)` | Searches from the beginning and stops at the first match. |

`get` and `remove` require `0 <= index < size`. Invalid indices throw
`IndexOutOfBoundsException` before changing data, capacity, or counters.
`size()`, `capacity()`, and `getMetrics()` expose basic information without
changing counters. `getMetrics().reset()` clears counts while preserving values.

The tests check empty and single-element arrays, duplicates, integer limits,
invalid indices, repeated growth, and exact counter values. A test compares
5,000 mixed operations generated with `new Random(42)` against `ArrayList`.
Standard collections are used only in tests.

## MyLinkedList

`daa.MyLinkedList` is a singly linked list. Each node holds an `int` value and
one `next` reference. The list keeps `head`, `tail`, and `size`. A singly linked
design needs only one link per node and keeps link updates easy to explain.
The `tail` reference makes appending constant time without walking the list.

| Method | Behavior |
| --- | --- |
| `add(value)` | Links a new node after the tail in constant time. For an empty list, sets both head and tail. |
| `add(index, value)` | Allows indices from 0 through `size`, inclusive. Head insertion and appending are constant time; interior insertion first finds the preceding node. |
| `remove(index)` | Returns the removed value and reconnects the surrounding links. Head removal is constant time; other removals first find the preceding node. |
| `get(index)` | Walks forward from the head and follows exactly `index` links. |
| `contains(value)` | Walks forward and stops at the first matching value. |

Index rules and exceptions match `DynamicArray`. After removing the only node,
both `head` and `tail` are `null`. Removing the last node of a longer list updates
`tail` to its predecessor. That search is linear in a singly linked list.

Link changes always count as moves. For example, adding at the head of a
nonempty list takes zero steps but two moves. Removing its head takes one step
and one move; removing its only node takes an extra move to clear `tail`.
Reading a node's value is not a step under the assignment's counting rule.
Thus, finding a value at the head takes zero steps and one comparison.
An unsuccessful search follows the final link to `null` and counts that step.

The tests cover head and tail changes, empty-list reuse, duplicates, invalid
indices, independent counters, and exact counts for each operation. A random
test compares 5,000 mixed operations with `ArrayList` and checks every stored
value after each operation.

## MinHeap

`daa.MinHeap` stores a binary minimum heap in an `int[]`. For a child at index
`i > 0`, its parent is at `(i - 1) / 2`. Every parent's value is at most its
children's values, so the minimum is always at index 0.

| Method | Behavior |
| --- | --- |
| `insert(value)` | Stores the value at the end, then swaps it with its parent while it is smaller. |
| `peekMin()` | Reads the root in constant time without removing it. |
| `extractMin()` | Returns the root, moves the last value to the root, then swaps it with its smaller child until the heap property is restored. |

`peekMin()` and `extractMin()` throw `IllegalStateException` on an empty heap,
without changing counters. The initial capacity is 10 and doubles when full.
Capacity does not shrink after extraction. Sifting uses at most a logarithmic
number of levels, but an insertion that grows the array also copies all current
values. A singleton extraction reads the root once and performs no moves.

Comparisons use the values directly, without subtraction, so the full `int`
range is supported. Sifting reuses values already read for comparisons; each
swap counts two moves and does not repeat those reads.

Tests inspect every parent and child after each insertion and extraction using
the package-private `valueAt(index)` method. This method counts its array read
as one step and does not expose the backing array. Exact operation counts are
asserted before these diagnostic reads. The benchmark will not call this method.

The tests cover empty and singleton heaps, duplicates, integer limits, repeated
growth, both child choices during sifting, and exact operation counts. A test
compares 5,000 mixed operations with `PriorityQueue` using `new Random(42)`.
Another extracts 1,000 random values and checks nondecreasing order and duplicate
counts. Standard collections remain confined to test code.

## Current status

The Maven project, all three data structures, operation counters, and the
benchmark are implemented. Charts and the report will be added in later stages.

All measured times will use seconds, including the CSV column `time_s`.
This is an intentional change from the assignment PDF, which specifies `time_ms`.
