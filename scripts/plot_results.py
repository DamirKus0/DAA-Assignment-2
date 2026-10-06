#!/usr/bin/env python3
"""Validate the benchmark CSV and render one four-panel PNG per workload."""

import argparse
import csv
import math
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import FixedLocator, FuncFormatter, LogLocator, NullLocator


PROJECT_ROOT = Path(__file__).resolve().parents[1]
HEADER = ["workload", "variant", "structure", "n", "time_s", "steps", "moves", "comparisons"]
SIZES = (100, 1_000, 10_000, 100_000)
SERIES = {
    "W1": (("DynamicArray", "-"), ("MyLinkedList", "-")),
    "W2": (("DynamicArray", "-"), ("MyLinkedList", "-")),
    "W3": (
        ("DynamicArray", "head"),
        ("DynamicArray", "middle"),
        ("MyLinkedList", "head"),
        ("MyLinkedList", "middle"),
    ),
    "W4": (("MinHeap", "-"),),
}
DESCRIPTIONS = {
    "W1": ("Random access", "10,000 get(index) operations"),
    "W2": ("Membership search", "1,000 contains(x) queries: 500 present and 500 absent"),
    "W3": ("Insertion and deletion", "1,000 insertions followed by 1,000 removals; head and middle variants"),
    "W4": ("Heap insertion and extraction", "n insertions followed by n extractMin() operations"),
}
COLORS = {"DynamicArray": "#1864ab", "MyLinkedList": "#c45b16", "MinHeap": "#7552a3"}
METRICS = (
    ("time_s", "Elapsed time", "Median time (seconds)"),
    ("steps", "Steps", "Steps (count)"),
    ("moves", "Moves", "Moves (count)"),
    ("comparisons", "Comparisons", "Comparisons (count)"),
)


def read_results(path):
    """Reject incomplete, duplicate, or malformed data before drawing anything."""
    expected = {
        (workload, variant, structure, size)
        for workload, series in SERIES.items()
        for structure, variant in series
        for size in SIZES
    }
    rows = {}
    with path.open(newline="", encoding="utf-8") as source:
        reader = csv.DictReader(source)
        if reader.fieldnames != HEADER:
            raise ValueError("CSV header must be exactly: " + ",".join(HEADER))
        for line_number, row in enumerate(reader, start=2):
            if None in row or any(value is None for value in row.values()):
                raise ValueError(f"Line {line_number}: incorrect number of fields")
            try:
                size = int(row["n"])
                counts = {name: int(row[name]) for name in HEADER[5:]}
                seconds = float(row["time_s"])
            except ValueError as error:
                raise ValueError(f"Line {line_number}: invalid numeric field") from error
            key = (row["workload"], row["variant"], row["structure"], size)
            if key not in expected:
                raise ValueError(f"Line {line_number}: unexpected case {key}")
            if key in rows:
                raise ValueError(f"Line {line_number}: duplicate case {key}")
            if not math.isfinite(seconds) or seconds <= 0:
                raise ValueError(f"Line {line_number}: time_s must be positive and finite")
            if any(value < 0 for value in counts.values()):
                raise ValueError(f"Line {line_number}: counts must be nonnegative integers")
            rows[key] = {"time_s": seconds, **counts}
    missing = expected - rows.keys()
    if missing:
        raise ValueError(f"CSV is missing {len(missing)} of the 36 expected cases")
    return rows


def style_for(structure, variant):
    """Keep structure colors and variant styles consistent across all panels."""
    if variant == "middle":
        marker, linestyle = "s", "--"
    elif variant == "head":
        marker, linestyle = "o", "-"
    else:
        marker = {"DynamicArray": "o", "MyLinkedList": "s", "MinHeap": "D"}[structure]
        linestyle = "-" if structure != "MyLinkedList" else "--"
    return {
        "color": COLORS[structure],
        "linestyle": linestyle,
        "marker": marker,
        "linewidth": 2.0,
        "markersize": 6.5,
        "markerfacecolor": "white",
        "markeredgewidth": 1.6,
    }


def configure_y_axis(axis, values):
    if all(value == 0 for value in values):
        axis.set_ylim(-0.12, 1.0)
        axis.set_yticks([0])
        axis.text(
            0.5, 0.50, "All series are zero (overlapping)",
            transform=axis.transAxes, ha="center", va="center",
            fontsize=12, color="#52606d",
        )
        return "linear"
    if any(value == 0 for value in values):
        axis.set_yscale("symlog", linthresh=1)
        axis.set_ylim(bottom=0)
        return "symlog"
    axis.set_yscale("log")
    axis.yaxis.set_major_locator(LogLocator(base=10, numticks=6))
    axis.yaxis.set_minor_locator(NullLocator())
    if min(values) == max(values):
        axis.set_ylim(min(values) / 3, max(values) * 3)
    else:
        axis.margins(y=0.17)
    return "log"


def draw_workload(workload, rows, output_dir):
    figure, axes = plt.subplots(2, 2, figsize=(12, 8), dpi=150)
    figure.subplots_adjust(left=0.10, right=0.975, bottom=0.11, top=0.83, wspace=0.30, hspace=0.48)
    title, description = DESCRIPTIONS[workload]
    figure.text(0.10, 0.955, f"{workload}  |  {title}", fontsize=21, weight="bold", color="#172b4d")
    figure.text(0.10, 0.910, description, fontsize=12, color="#52606d")

    for axis, (metric, title, ylabel) in zip(axes.flat, METRICS):
        values_by_series = []
        for structure, variant in SERIES[workload]:
            values = [rows[(workload, variant, structure, size)][metric] for size in SIZES]
            values_by_series.append(values)
            label = structure if variant == "-" else f"{structure}: {variant}"
            axis.plot(SIZES, values, label=label, **style_for(structure, variant))

        values = [value for series in values_by_series for value in series]
        scale = configure_y_axis(axis, values)
        axis.set_title(f"{title}  ({scale} scale)", loc="left", pad=10, fontsize=13, weight="bold")
        axis.set_xscale("log")
        axis.set_xlim(SIZES[0] / 1.25, SIZES[-1] * 1.25)
        axis.xaxis.set_major_locator(FixedLocator(SIZES))
        axis.xaxis.set_major_formatter(FuncFormatter(lambda value, position: f"{int(value):,}"))
        axis.xaxis.set_minor_locator(NullLocator())
        axis.set_xlabel("Initial size n (log scale)" if workload != "W4" else "Elements n (log scale)")
        axis.set_ylabel(ylabel)
        axis.grid(which="major", color="#d9e2ec", linewidth=0.8)
        axis.set_axisbelow(True)
        axis.spines[["top", "right"]].set_visible(False)
        axis.spines[["bottom", "left"]].set_color("#aab7c4")
        axis.legend(loc="best", fontsize=8.5 if workload == "W3" else 10,
                    frameon=True, facecolor="white", edgecolor="#d9e2ec", framealpha=0.94)

        if any(values) and len(values_by_series) > 1:
            if all(series == values_by_series[0] for series in values_by_series[1:]):
                axis.text(0.97, 0.05, "Series coincide", transform=axis.transAxes,
                          ha="right", va="bottom", fontsize=9, color="#52606d")
            elif workload == "W3" and values_by_series[2] == values_by_series[3]:
                axis.text(0.97, 0.05, "List variants coincide", transform=axis.transAxes,
                          ha="right", va="bottom", fontsize=9, color="#52606d")

    figure.text(
        0.10, 0.035,
        "Time: median of 5 measured runs after 2 warm-ups. Counts: identical across measured runs.",
        fontsize=10, color="#52606d",
    )
    destination = output_dir / f"{workload.lower()}.png"
    figure.savefig(destination, facecolor="white", metadata={"Title": f"{workload}: {DESCRIPTIONS[workload][0]}"})
    plt.close(figure)
    return destination


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=PROJECT_ROOT / "results" / "results.csv")
    parser.add_argument("--output-dir", type=Path, default=PROJECT_ROOT / "results" / "plots")
    args = parser.parse_args()
    try:
        rows = read_results(args.input)
    except (OSError, ValueError) as error:
        parser.error(str(error))
    args.output_dir.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({
        "font.family": "DejaVu Sans", "font.size": 11,
        "axes.labelsize": 11, "xtick.labelsize": 10, "ytick.labelsize": 10,
    })
    print(f"Validated {len(rows)} benchmark cases from {args.input}")
    for workload in SERIES:
        print(f"Created {draw_workload(workload, rows, args.output_dir)}")


if __name__ == "__main__":
    main()
