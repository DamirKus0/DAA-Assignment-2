# DAA Assignment 2

Java implementations of `DynamicArray`, `MyLinkedList`, and `MinHeap`, with
operation counters, JUnit 5 tests, and reproducible benchmarks.

## Requirements

- JDK 17 or later. Maven Wrapper is included.
- Python 3.10+ for plots only.
- Internet access during the first setup to download dependencies.

## Build, test, and benchmark

Run from the project directory:

```sh
./mvnw clean verify          
./mvnw test              
./mvnw -Pbenchmark verify   
```

On Windows, replace `./mvnw` with `mvnw.cmd`.

## Generate plots

Set up the Python environment once, then run the plotting script:

```sh
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements-plot.txt
python scripts/plot_results.py
```

On Windows, use `py -m venv .venv` and `.venv\Scripts\Activate.ps1`
in PowerShell. The script reads the CSV and creates four PNG figures.

## Measurements and files

Benchmarks use seed 42 and sizes 100, 1,000, 10,000, and 100,000. Every case has
two warm-ups and five measured runs; the CSV stores median time and one run's
counts, which must match across repeats. (`time_s`),
an intentional change from the PDF's `time_ms`.

- `src/main/java/`: structures, counters, and benchmark.
- `src/test/java/`: JUnit 5 tests.
- [results/results.csv](results/results.csv): measured results.
- [results/benchmark-info.txt](results/benchmark-info.txt): runtime settings.
- [results/plots/](results/plots/): time and operation-count charts.
- [REPORT.md](REPORT.md): counting rules, complexity, proofs, and discussion.

To reproduce everything, run the benchmark and then the plotting script.
New timings may require updating the numerical discussion in the report.
