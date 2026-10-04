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

## Project layout

- `src/main/java/`: data structures, operation counters, and benchmark.
- `src/test/java/`: JUnit 5 tests.
- `results/results.csv`: measured benchmark results, added after running the benchmark.
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

## Current status

The Maven project and the shared `Metrics` class are implemented. JUnit 5 tests
cover reset and reuse, independent counter instances, and counts above the
maximum `int` value. The data structures, benchmark, measurements, charts, and
report will be added in later stages.

All measured times will use seconds, including the CSV column `time_s`.
This is an intentional change from the assignment PDF, which specifies `time_ms`.
