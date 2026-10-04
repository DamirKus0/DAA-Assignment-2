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

## Current status

This is the project setup stage. There are no Java classes or tests yet.
The benchmark, measurements, charts, and report will be added in later stages.

All measured times will use seconds, including the CSV column `time_s`.
This is an intentional change from the assignment PDF, which specifies `time_ms`.
