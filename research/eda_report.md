# Working backwards from the data

Source: `chevronronson__nba-stats-dataset/sql/nba_stats.sqlite`, `player_boxscores`
(CC0-1.0). Regular season only (`game_id` prefix `002`): 736,410 rows, 1996-97 →
2025-26. DNPs removed (`available_flag`), zero-minute rows removed. Every feature
is `shift(1)`-lagged, so a game is never in its own predictors.

Scripts: `eda_stage2.py` … `eda_stage5.py`. Raw numbers: `eda_stage*.json`.

Fit: everything before 2023-24 (592,912 rows). Holdout: 2023-24 → 2025-26
(70,952 rows).

---

## 1. What the distribution looks like

| | |
|---|---|
| mean | 9.99 |
| variance | 67.7 |
| var / mean | **6.78** |
| share of 0-point games | 11.3% |
| quartiles | 4 / 8 / 15 |

Poisson assumes var = mean. This is **6.8× overdispersed**, so a Poisson is wrong
as a raw model of an arbitrary player-game — and a normal is worse (it puts mass
below zero: 11% of games are 0 points).

## 2. Overdispersion is a function of the mean

| trailing mean | n | var/mean | implied NB r |
|---|---|---|---|
| 0-5 | 161,515 | 4.50 | 1.09 |
| 5-10 | 217,627 | 3.92 | 2.59 |
| 10-15 | 146,446 | 3.42 | 4.99 |
| 15-20 | 80,978 | 3.09 | 8.01 |
| 20-25 | 38,851 | 2.90 | 11.20 |
| 25-30 | 14,512 | 2.79 | 14.30 |
| 30-40 | 3,869 | 2.79 | 16.61 |

Fitted `r = 0.1743 · μ^1.352` — but see §5: applying it turns out to be a mistake.

## 3. Minutes explain about a third, not most

Within a player-season (11,529 player-seasons, ≥20 games):

- median R² of points on minutes: **0.375** (p25 0.255, p75 0.504)

So playing time is the single biggest observable driver, but two thirds of a
player's game-to-game swing is rate (usage, matchup, game script), not minutes.
That is why a minutes model alone will not fix the distribution.

## 4. The mean: recency and season-to-date are equally weighted

Holdout RMSE / MAE, one game ahead:

| predictor | RMSE | MAE |
|---|---|---|
| trailing 10 games | 6.170 | 4.697 |
| season-to-date | 6.173 | 4.687 |
| **50/50 blend** | **6.092** | **4.632** |
| 30/70 blend | 6.105 | 4.640 |

Regressing points on both windows: coefficients **0.469** (season-to-date) and
**0.471** (trailing 10), intercept 0.90, R² 0.526 vs 0.513 for season-to-date
alone.

Two independent estimates landing on 50/50 is the useful part: the last 10 games
carry real information, but no more than half of it.

## 5. Pricing the line: the parametric models lose

Line convention `L = floor(μ) + 0.5` (the half point just below the predicted
mean — realistic prop shape, no push mass). 25,000 holdout rows.

| model | Brier | log loss | **ECE** |
|---|---|---|---|
| **empirical table keyed by μ** | **0.24864** | **0.69043** | **0.0027** |
| Poisson(μ) | 0.24994 | 0.69311 | 0.0192 |
| NB, r = 0.1743·μ^1.352 | 0.25367 | 0.70109 | 0.0554 |
| player's own last 10 games | 0.26076 | 0.71725 | 0.0720 |

The negative binomial is **the worst parametric option**. Its `r` was fitted on
pooled buckets, so it carries between-player heterogeneity into an individual
player's distribution and over-disperses the near-mean region. Poisson is
closer because, conditional on the player's own mean, var ≈ mean is a better
local approximation than the pooled NB.

The player's own 10-game histogram is the worst model overall — 10 samples are
noise, even though the *mean* of those 10 games is not.

## 6. A second key makes it worse

| table | cells | Brier | ECE |
|---|---|---|---|
| μ | 83 | **0.24864** | **0.0027** |
| μ × trailing sd | 370 | 0.24900 | 0.0059 |
| μ × trailing minutes | 223 | 0.24877 | 0.0080 |

Fragmentation costs more than the extra key buys. The player's own volatility and
role add nothing measurable once the mean is known — which is the same message as
§3 from a different angle.

## 7. Push mass is large and predictable on average

At the integer nearest the predicted mean, the share of games landing exactly on
the line is **7.21%** (25,000 holdout rows). The table predicts 7.39%, the NB
7.26% — both fine on average, both poor per row (push Brier 0.066, because a 7%
event is mostly irreducible).

Weighted push rate across integer lines 1-35 in the full sample: **7.33%**,
ranging from 2.9% (line 1) to 19.1% (line 2). It is not a constant, and it is
much larger than the 0.0 the current scorer assumes by refusing integer lines.

---

## The basis this supports

1. **Mean:** `μ = 0.5 · season-to-date + 0.5 · trailing-10`, floor at 0.
   Measured coefficients 0.47/0.47, RMSE 6.09. Cold start (<5 prior games) needs
   its own rule — not modelled yet.
2. **Distribution:** `P(points = k | μ)` from an empirical table keyed on a 0.5
   point μ bucket, Laplace-smoothed, fit on pre-2023 data. 83 buckets × 61 values
   ≈ 5k floats. ECE 0.0027 out of time.
3. **Push:** the same table returns `P(points = line)` for an integer line. No
   separate push model needed.
4. **Not:** NB with pooled dispersion, Poisson as the shipped model, the player's
   own histogram, or any second key.

This is deliberately unglamorous: a 5k-number lookup table plus a two-line mean.
It is also the only thing measured here that is calibrated out of time, it is
deterministic, and it ports to Kotlin without a maths library.

## What it still cannot do

- **No edge test.** There are no prop lines in the corpus, so this prices a
  synthetic line at the mean. Edge requires a book's line and price.
- **No opponent/pace adjustment.** Not tested. The second-key result suggests the
  gain would be small, but that is a hypothesis, not a measurement.
- **No cold start.** Rows need 5 prior games; a season debut or a trade has no μ.
- **Playoffs excluded** from the fit (50,355 rows held out of scope entirely).
