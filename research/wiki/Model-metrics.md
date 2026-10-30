# Model metrics — the frozen challenger run

**Run:** `research/kaggle/runs/2026-10-29/` (kernel
`tap919/propslab-challenger-table-vs-constrained-gbm`)
**Verdict: PROMOTE C2_gbm** — all six promotion gates passed.
Figures are `recorded` from the run artifacts; the per-delta over-rates underneath were
`recomputed` from the parquet and **match the artifact exactly**.

## Design

- Fit ≤ 2022-23 (594,251 player-games); holdout 2023-24, 2024-25, 2025-26 (70,952).
- Line grid: `floor(mu) + 0.5 + delta`, delta ∈ [−3 … +3] → **7 offsets,
  496,664 holdout evaluations**. Half-integer lines: no pushes.
- Target: `pts > line`. Pooled observed over-rate: **0.48355**.

## Headline table (holdout, pooled across all 7 offsets)

| Model | Brier | log loss | ECE | slope | ΔBrier vs champion |
|---|---:|---:|---:|---:|---:|
| champion_table | 0.226308 | 0.640863 | 0.01376 | 0.966 | — |
| C1_logistic | 0.228109 | 0.648117 | 0.01033 | 0.938 | +0.001801 |
| **C2_gbm (constrained)** | **0.224787** | **0.637540** | **0.00570** | **0.986** | **−0.001520** |
| C2_core_ablation | 0.225573 | 0.639265 | 0.00488 | 0.985 | −0.000735 |
| B0_base_rate | 0.229415 | 0.650969 | 0.00678 | 0.946 | +0.003107 |

Level check: champion mean predicted 0.49732 vs observed 0.48355 (**+1.37 pp optimistic**);
the GBM's residual bias is +0.56 pp (0.48920 vs 0.48355).

## Bootstrap — ΔBrier vs champion, 95% CI

| Model | Cluster | Mean | Lo | Hi | Clusters |
|---|---|---:|---:|---:|---:|
| C2_gbm | game | −0.001520 | −0.001769 | −0.001279 | 3,478 |
| C2_gbm | player | −0.001519 | −0.001789 | −0.001249 | 746 |
| C2_core_ablation | game | −0.000736 | −0.000945 | −0.000532 | 3,478 |
| C1_logistic | game | +0.001803 | +0.001578 | +0.002022 | 3,478 |
| C1_logistic | player | +0.001799 | +0.001391 | +0.002215 | 746 |

Both C2_gbm intervals sit entirely below zero — the win is not a clustering artefact.

## The six promotion gates

| Gate | C2_gbm | C1_logistic |
|---|---|---|
| ΔBrier < 0 | pass | fail |
| game-clustered CI excludes 0 | pass | fail |
| player-clustered CI excludes 0 | pass | fail |
| ECE ≤ 0.010 | pass (0.00570) | fail (0.01033) |
| slope in [0.9, 1.1] | pass (0.986) | pass (0.938) |
| improves ≥ 2 of 3 holdout seasons | pass (3/3) | fail (0/3) |

Per-season ΔBrier — **C2_gbm**: 2023-24 −0.001593, 2024-25 −0.001404, 2025-26 −0.001564.
**C1_logistic**: +0.002130, +0.002116, +0.001163 (worse every season).

## Brier by line offset (the difficulty curve)

| δ | champion | C1 | C2_gbm | observed over-rate |
|---:|---:|---:|---:|---:|
| −3 | 0.19697 | 0.20550 | 0.19553 | 0.70862 |
| −2 | 0.22620 | 0.23174 | 0.22442 | 0.63043 |
| −1 | 0.24576 | 0.24654 | 0.24411 | 0.54631 |
| 0 | 0.24904 | 0.24761 | 0.24730 | 0.47123 |
| +1 | 0.23996 | 0.23909 | 0.23844 | 0.40254 |
| +2 | 0.22345 | 0.22325 | 0.22209 | 0.34020 |
| +3 | 0.20276 | 0.20304 | 0.20161 | 0.28553 |

The over-rate column was recomputed from the parquet on 2026-10-29 and matches the run
artifact to better than 5×10⁻⁶ — the wiki and the frozen run agree.

**Reading:** δ = 0 (line exactly at the baseline) is the hardest row — Brier ≈ 0.247–0.249
for every model — and offsets of ±3 are easy. Pooled Brier (0.2248) is therefore *lower*
than the δ=0 value; never compare a pooled number against a single-offset number.

## Calibration reliability (max |predicted − observed| per bin)

| Model | pooled | at δ = 0 |
|---|---:|---:|
| champion_table | 0.02201 | 0.03564 |
| C2_gbm | 0.01653 | 0.03114 |
| C1_logistic | 0.02046 | — |
| B0_base_rate | 0.01326 | 0.03434 |

Full per-bin rows: `runs/2026-10-29/reliability.csv` and `wiki/baselines.json`.

## What the run settled that was not obvious before

1. The champion's +1.37 pp level bias is roughly the size of the GBM's entire improvement
   — **the win is mostly bias correction, not new structure.**
2. Constraining `line` alone is insufficient: `delta` must be constrained too, or the
   model undoes the `line` constraint along the direction the data moves. The smoke test
   caught a 0.640% violation rate; with `delta` constrained it is 0.000%.
3. The ablation (`C2_core_ablation`, ECE 0.00488) calibrates *better* than the promoted
   model but scores worse on Brier — calibration alone is not the promotion rule.

## Promotion rule, verbatim

`delta_brier < 0` · `ci_hi < 0` (both clusterings) · `ece ≤ 0.010` ·
`slope ∈ [0.9, 1.1]` · improvement in ≥ 2 of 3 holdout seasons.
