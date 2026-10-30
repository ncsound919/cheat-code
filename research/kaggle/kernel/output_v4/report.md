# PropsLab challenger run — result

**Verdict: PROMOTE C2_gbm**

Dataset `tap919/propslab-nba-prop-training` (sha256 `962376828535ff49…`). Fit ≤2022-23 (594,251 player-games), holdout 2023-24, 2024-25, 2025-26 (70,952). Grid: 7 line offsets, 496,664 holdout evaluations.

## Headline

| model | Brier | log loss | ECE | slope | ΔBrier vs table |
|---|---:|---:|---:|---:|---:|
| champion_table | 0.226308 | 0.640863 | 0.01376 | 0.966 |  |
| C1_logistic | 0.228109 | 0.648117 | 0.01033 | 0.938 | +0.001801 |
| C2_gbm | 0.224787 | 0.637540 | 0.00570 | 0.986 | -0.001520 |
| C2_core_ablation | 0.225573 | 0.639265 | 0.00488 | 0.985 | -0.000735 |
| B0_base_rate | 0.229415 | 0.650969 | 0.00678 | 0.946 | +0.003107 |

## Promotion rule (all six must hold)

### C1_logistic — REJECTED

- [ ] delta_brier_negative
- [ ] ci_game_excludes_zero
- [ ] ci_player_excludes_zero
- [ ] ece_le_0_010
- [x] slope_in_range
- [ ] two_of_three_seasons
- per-season ΔBrier: 2023-24 +0.002130, 2024-25 +0.002116, 2025-26 +0.001163

### C2_gbm — PROMOTED

- [x] delta_brier_negative
- [x] ci_game_excludes_zero
- [x] ci_player_excludes_zero
- [x] ece_le_0_010
- [x] slope_in_range
- [x] two_of_three_seasons
- per-season ΔBrier: 2023-24 -0.001593, 2024-25 -0.001404, 2025-26 -0.001564

## Bootstrap (ΔBrier vs champion, 95% CI)

| model | cluster | mean | lo | hi | clusters |
|---|---|---:|---:|---:|---:|
| C1_logistic | game | +0.001803 | +0.001578 | +0.002022 | 3,478 |
| C1_logistic | player | +0.001799 | +0.001391 | +0.002215 | 746 |
| C2_gbm | game | -0.001520 | -0.001769 | -0.001279 | 3,478 |
| C2_gbm | player | -0.001519 | -0.001789 | -0.001249 | 746 |
| C2_core_ablation | game | -0.000736 | -0.000945 | -0.000532 | 3,478 |
| C2_core_ablation | player | -0.000735 | -0.000950 | -0.000519 | 746 |

## Brier by line offset

| δ | n | champion | C1 logistic | C2 GBM |
|---:|---:|---:|---:|---:|
| -3 | 70,952 | 0.19697 | 0.20550 | 0.19553 |
| -2 | 70,952 | 0.22620 | 0.23174 | 0.22442 |
| -1 | 70,952 | 0.24576 | 0.24654 | 0.24411 |
| +0 | 70,952 | 0.24904 | 0.24761 | 0.24730 |
| +1 | 70,952 | 0.23996 | 0.23909 | 0.23844 |
| +2 | 70,952 | 0.22345 | 0.22325 | 0.22209 |
| +3 | 70,952 | 0.20276 | 0.20304 | 0.20161 |

## Monotonicity violations (fraction of player-games)

- champion_table: 0.000%
- C1_logistic: 0.000%
- C2_gbm: 0.000%
- C2_core_ablation: 0.000%
- B0_base_rate: 0.000%

## Scope limits (from CRITERIA.md §12)

- No real prop lines exist in this corpus: this prices a synthetic line, so **no edge claim is possible**.
- All probabilities are `P(over | plays)`; availability is not modelled.
- Playoffs excluded. No closing-line movement test.
