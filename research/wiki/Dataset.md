# Dataset

**File:** `research/kaggle/dataset/propslab_training.parquet`
**SHA-256:** `962376828535ff49e58b92b4b98cd7c687eabc21326ad68a4ffc66deefba8dbc`
**Rebuilt by:** `research/kaggle/build_dataset.py` from
`chevronronson__nba-stats-dataset/sql/nba_stats.sqlite` (CC0-1.0)
**Labels below:** all figures `recomputed` unless marked `recorded`.

## Shape

| | |
|---|---|
| Rows | 665,203 |
| Columns | 20 |
| Seasons | 30 — 1996-97 → 2025-26 (regular season only) |
| Players | 2,615 |
| Games | 33,460 |
| Nulls | 0 |
| Training rows | 594,251 (through 2022-23) |
| Holdout rows | 70,952 (2023-24, 2024-25, 2025-26 — 746 players, 3,478 games) |

Three different row counts appear across this project's analyses, and they are not
interchangeable:

| Frame | Rows | Used by |
|---|---|---|
| Leakage-free extract | 665,203 | parquet, all model runs, this wiki's recomputations |
| EDA frame | 721,198 | `eda_stage*.json` (`recorded`) |
| Betting rows | 19,810 of 24,440 games | `results.json` pricing/calibration (`recorded`) |

## Gates (re-run 2026-10-29, `python kaggle/validate_dataset.py`)

```
ALL GATES PASSED
[PASS] fit rows > 500k (594,251)          [PASS] no season overlap
[PASS] holdout rows > 60k (70,952)        [PASS] holdout seasons complete
[PASS] no nulls anywhere (0 nulls)        [PASS] 2025-26 defense/pace populated and sane
[PASS] leakage spot-check: trailing mean excludes the current game
       (1,135 rows checked, 0 mismatches)
[PASS] corr(mu, pts) = 0.719, plausible   [PASS] unique player-game, two teams per game
```

## How the rows were built

- Regular season only (`game_id` prefix `002`); games actually played only — the source
  has no DNP rows.
- **Every player feature is `shift(1)`-lagged**: a game never contributes to its own
  predictors. The spot-check recomputes the trailing mean straight from SQL and found 0
  mismatches in 1,135 rows.
- `mu = 0.5 × season-to-date + 0.5 × trailing-10`, floored at 0 — the app's own baseline
  estimate of expected points.
- `own_pace_trail` and `opp_def100_trail` are derived from raw counting stats, not from
  precomputed rating columns.
- **The label is points.** The binary target is built in the kernel as `pts > line`.

## Columns

| Column | Meaning |
|---|---|
| `season_year`, `game_id`, `game_date` | identity; season is a string like `2024-25` |
| `player_id`, `player_name`, `team_id`, `opp_team_id` | who and against whom |
| `is_home` | 1 home, 0 away (share is 0.500) |
| `rest_days`, `b2b` | days since last game; back-to-back flag (share 0.204) |
| `n_prior_season` | experience bucket |
| `t_pts`, `t_min`, `t_sd`, `t_usg` | trailing-10 means: points, minutes, sd, usage |
| `s_pts`, `s_min` | season-to-date means |
| `mu` | blended expected points (the model's core feature) |
| `own_pace_trail`, `opp_def100_trail` | team tempo and opponent defence, lagged |
| `pts`, `min` | **target** and minutes played in this game |

## Caveats to repeat when asked

- No real prop lines exist in this corpus. Runs price a synthetic line
  `floor(mu) + 0.5 + delta` — half-integer, so **pushes cannot occur** in the benchmark.
- Playoffs are excluded. There is no closing-line-movement test anywhere in this data.
- The dataset is points-only. Rebounds, assists, threes and PRA exist in the app's live
  pipeline (`GameLogModel`) but have never been through this benchmark.
