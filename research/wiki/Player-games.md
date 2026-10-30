# Player-games: what one game looks like

**Frame:** the 665,203-row leakage-free extract, unless marked `recorded` (EDA frame,
721,198 rows). All figures `recomputed` from the parquet on 2026-10-29.

## Points distribution (target `pts`)

| | |
|---|---|
| mean | 10.11 |
| sd | 8.24 |
| variance | 67.96 |
| **variance / mean** | **6.72** (Poisson would be 1.0) |
| min / max | 0 / 83 |
| share of 0-point games | 10.8% |
| skew | 1.01 |
| quartiles (p25 / p50 / p75) | 4 / 8 / 15 |
| p01 / p99 | 0 / 34 |

`recorded` (EDA frame, same shape): mean 9.99, var/mean 6.78, zero-share 11.3%.

**Reading:** the target is right-shifted and wildly overdispersed. A Poisson is wrong as
a raw model of a player-game (it forces var = mean) and a normal is worse — it puts mass
below zero while 11% of games are literally 0 points.

## Where the spread comes from

| Component | Variance | Share |
|---|---:|---:|
| between players (who they are) | 22.31 | 32.8% |
| within player (night to night) | 45.65 | 67.2% |

**Reading:** two thirds of the variance is the same player on a different night. That is
the irreducible noise floor for any prop model, and it is why a 1–2 point edge is
meaningful while a 6-point "edge" is almost certainly a bug.

## Fit vs holdout era

| | mean pts |
|---|---:|
| training (≤2022-23) | 10.01 |
| holdout (2023-24 → 2025-26) | 10.95 |

The holdout era scores ~0.94 points more per game than the training era. Any model
trained only on old data starts with a level bias — the frozen run measures exactly that
(see [Model-metrics.md](Model-metrics.md)).

## Era trend (`recorded` season means)

| | |
|---|---|
| first 5 seasons (1996-97…) | 9.61 |
| last 5 seasons (…2025-26) | 10.97 |
| change | +1.36 |
| highest season | 2022-23 at 11.19 |
| lowest season | 1998-99 at 9.18 (shortened season) |

## Schedule splits — raw averages, nothing controlled (`recomputed`)

| Split | Share of rows | Mean pts |
|---|---:|---:|
| home | 50.0% | 10.25 |
| away | 50.0% | 9.97 |
| back-to-back | 20.4% | 10.38 |
| not back-to-back | 79.6% | 10.04 |
| mean rest days | 2.52 | — |
| mean minutes | 23.3 | 13.8% of rows under 10 min |

`recorded` (EDA frame, finer rest buckets):

| Rest | n | Mean pts |
|---|---:|---:|
| back-to-back | 144,882 | 10.30 |
| 1 day | 367,774 | 10.54 |
| 2 days | 105,047 | 10.14 |
| 3–7 days | 66,045 | 7.89 |
| 8+ days | 36,951 | 6.66 |

**Caveat:** the 3–7 and 8+ day buckets are tiny and self-selecting (injuries, returns,
dead minutes). Do not quote these as rest effects — quote them as raw means.

## Predicting one game from history (`recorded`)

| Predictor | MAE | RMSE | Bias |
|---|---:|---:|---:|
| trailing-10 points | 4.45 | 5.82 | +0.01 |
| trailing-10 (holdout bake-off) | 4.70 | 6.17 | +0.13 |
| season-to-date (holdout) | 4.69 | 6.17 | +0.42 |
| 50/50 blend (= `mu`) | 4.63 | **6.09** | +0.27 |

`recomputed` equivalent: `mu` correlates with `pts` at **r = 0.719**, misses by **4.38**
points on average, lands within ±3 on 44.3% of rows and within ±5 on 66.3%.

**Reading:** a single player-game can only be predicted to about ±4–6 points. Minutes
explain a third of it (`recorded` R²: median 0.375 across 11,529 player-seasons) — a
model that leans mostly on minutes is leaning on the third that is knowable.

## Integer lines push; half lines don't (`recorded`)

Push rates at integer lines (share of rows landing exactly on the line):

| line | push share |
|---:|---:|
| 2 | 19.0% |
| 4 | 10.5% |
| 6 | 9.1% |
| 3 | 7.5% |
| 1 | 3.0% |

Across the board: **7.6%** unweighted, **7.3%** weighted. At half-integer lines the push
rate is 0 by construction (points are integers). The app models push mass explicitly on
whole-number lines and the benchmark sidesteps it by using half lines — both choices are
deliberate; see [Formula-benchmarks.md](Formula-benchmarks.md).
