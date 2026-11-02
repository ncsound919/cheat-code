# PropsLab training dataset

`propslab_training.parquet` is the frozen player-game dataset behind the promoted
GBM. It is **not** a scoring table on its own: two of the twelve model features are
quote-time values and must be constructed before a row can be scored.

| File | What it is |
|---|---|
| `propslab_training.parquet` | 665,203 rows, 22 columns, 30 seasons (1996-97 … 2025-26), 2,615 players, 33,460 games |
| `propslab_training_sample.csv` | 1.7 MB CSV sample of the same columns |
| `schema.json` | Column types and the dataset's own notes |
| `manifest.json` | Source, licence, byte counts and SHA-256 for each file |

Source: `chevronronson__nba-stats-dataset/sql/nba_stats.sqlite`, CC0-1.0.
Regular season only (`game_id` prefix `002`), games actually played only. Every
player feature is `shift(1)`-lagged, so a game never contributes to its own row.
Label: `pts`.

## The twelve model features

Ten come straight from the parquet:

| Feature | Meaning |
|---|---|
| `mu` | Blend of season-to-date and trailing-10 points, floored at 0 |
| `t_sd` | Trailing points standard deviation |
| `t_min` | Trailing minutes |
| `t_usg` | Trailing usage |
| `rest_days` | Days since the player's previous game |
| `b2b` | Second game of a back-to-back |
| `is_home` | Home game |
| `n_prior_season` | Prior-season sample size |
| `own_pace_trail` | Team pace, derived from raw counting stats |
| `opp_def100_trail` | Opponent defensive rating per 100 possessions, derived from raw counting stats |

Two are **quote-time only** — they depend on the line being priced, so they cannot be
frozen into the parquet:

| Feature | Construction at scoring time |
|---|---|
| `line` | The half-point line being quoted |
| `delta` | Integer offset from `floor(mu) + 0.5`: `line - (floor(mu) + 0.5)`, in `[-3, 3]` |

Training expands every player-game across a grid of offsets
(`research/kaggle/kernel/train.py`, `DELTAS = [-3, -2, -1, 0, 1, 2, 3]`):

```python
base  = floor(mu) + 0.5
line  = base + delta          # delta in DELTAS = [-3, -2, -1, 0, 1, 2, 3]
y     = pts > line
```

That is why the dataset has 665,203 rows rather than one row per player-game, and
why `GbmBundleScorer` rejects integer lines: the model was trained on half-point
lines with no push mass. To reproduce a score, derive `base = floor(mu) + 0.5`
from the row and set `delta = line - base` (an integer in `[-3, 3]`) — **not**
`line - mu`, which is fractional and outside the trained feature range.

## Verification

- Dataset validation: `python research/kaggle/validate_dataset.py`.
- Scorer parity: `python ecosystem-sweep/harness/gbm_artifact_parity.py`.
  The Kotlin `GbmBundleScorer` matches `export_gbm.py::predict_bundle` to
  **1.1e-16** over 120 sampled rows on the promoted bundle.
- File hashes are in `manifest.json`; the parquet is 18,118,646 bytes.
