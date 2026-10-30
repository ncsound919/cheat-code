# PropsLab challenger run — pre-registered criteria

**Status: FROZEN before execution.** Written 2026-10-29, before the kernel was
pushed. Nothing below may be changed after seeing holdout results. If the challenger
fails, it is not re-tuned against the holdout; the protocol restarts on a future
season that does not exist yet.

| | |
|---|---|
| Dataset | `tap919/propslab-nba-prop-training` |
| Parquet SHA-256 | `962376828535ff49e58b92b4b98cd7c687eabc21326ad68a4ffc66deefba8dbc` |
| Rows | 665,203 player-games, 30 seasons (1996-97 → 2025-26) |
| Kernel | `tap919/propslab-challenger-table-vs-constrained-gbm` |
| Method doc | `research/BEST_PRACTICES.md` |

---

## 1. The question

> Does a model that sees **where the line sits relative to the player's mean**, plus
> context, price `P(points > line)` better than the champion lookup table — out of time?

The champion's structural blind spot is that it answers from `mu` alone. Two players
with the same mean get the same distribution, and the line's position relative to the
mean is not a first-class input. A smooth model in `(mu, line)` could exploit that.
This run decides whether it actually does.

### Hypotheses

| ID | Hypothesis | Predicted |
|---|---|---|
| H1 | A constrained GBM beats the table on Brier, out of time | ~30% likely |
| H2 | The GBM holds ECE ≤ 0.010 while doing so | ~20% likely |
| H3 | The improvement concentrates at \|δ\| ≥ 2 (far from the mean), not at δ = 0 | likely |
| H4 | Adding opponent defense and rest moves Brier by < 0.001 | likely |

H3 and H4 are diagnostics, not promotion criteria. They are recorded so that a null
result still teaches something.

**Prior expectation, stated honestly:** the champion probably wins. It already
achieves ECE 0.0027 out of time, and every attempt to add information so far
(player sd: 0.0059, minutes: 0.0080) made calibration worse. This run exists to make
that a measured statement rather than an opinion.

---

## 2. Champion (must be beaten)

The μ-keyed empirical distribution table.

```
fit rows:      season_year <= "2022-23"
bucket width:  0.5 points of mu, clipped to [0, 50]
support:       k = 0..60
smoothing:     Laplace, alpha = 0.5 over 61 cells
query:         P(points > L) = 1 - CDF(floor(L)), CDF interpolated
               linearly between the two nearest bucket CDFs
```

Interpolation is included deliberately: it makes the champion stronger than the
nearest-bucket version used in the earlier EDA (ECE 0.0027). The bar should be the
best version of the incumbent, not a convenient one.

Push mass at integer lines is handled by the table itself and is **not** part of this
comparison — the grid uses half-integer lines only, so every row is a clean binary.

---

## 3. Challengers

| ID | Model | Calibration |
|---|---|---|
| C1 | `LogisticRegression` (L2, standardised) | isotonic, cross-fitted |
| C2 | `HistGradientBoostingClassifier`, `monotonic_cst = [+1 mu, −1 line]` | isotonic, cross-fitted |
| B0 | Base rate (pooled `P(over)` per δ) | none — sanity floor |

Cross-fitting uses `GroupKFold` keyed on `season_year` **within the fit period**, so a
calibration fold never shares a season with its training fold. Both champion and
challengers see exactly the same 594,251 fit rows.

C2's monotonic constraints are not optional. A model that prices a 27.5 line as easier
than 26.5 for the same player is wrong regardless of its Brier score.

---

## 4. Features (frozen — no additions after results)

| Feature | Source | Why it is allowed |
|---|---|---|
| `mu` | 0.5·season-to-date + 0.5·trailing-10 | champion's own mean |
| `line` | grid | the information the champion lacks |
| `delta` | `line − mu` | explicit position |
| `t_sd` | trailing 10 sd of points | player volatility |
| `t_min` | trailing 10 minutes | role / playing time |
| `t_usg` | trailing 10 usage rate | offensive role |
| `rest_days`, `b2b` | schedule | known pre-game |
| `is_home` | schedule | known pre-game |
| `n_prior_season` | games played this season | role stability |
| `own_pace_trail` | trailing team possessions | game environment |
| `opp_def100_trail` | opponent trailing pts allowed/100 | matchup |

No player identity, no team identity, no season index, no target encoding. Those
would let the model memorise rather than generalise, and the holdout contains players
who did not exist in the fit window.

---

## 5. Line grid (frozen)

```
base  = floor(mu) + 0.5          # half point just below the mean, no push mass
line  = base + delta
delta ∈ {−3, −2, −1, 0, +1, +2, +3}
```

7 offsets × 665,203 rows. Every line is a half-integer, so no pushes and no ties.
Training rows are the grid-expanded set; **evaluation uncertainty is computed on
clusters, never on the expanded rows** (see §8).

---

## 6. Splits (frozen)

| Window | Seasons | Role |
|---|---|---|
| Fit | ≤ 2022-23 | train champion + challengers (594,251 rows) |
| Calibration | cross-fitted inside fit window, grouped by season | isotonic calibration |
| **Holdout** | 2023-24, 2024-25, 2025-26 | **evaluated once** (70,952 rows) |

No holdout row influences fitting, calibration, feature choice, or hyperparameters.

---

## 7. Metrics

**Primary (selection):**

- Brier score, pooled over all δ and reported per δ.
- ΔBrier = challenger − champion. Negative means the challenger is better.

**Secondary:**

- Log loss.

**Diagnostic (never used to select):**

- ECE, equal-mass adaptive bins, 15 bins (Nixon et al. 2019 recommendation).
- Reliability table per δ.
- Calibration slope `b` and intercept `a` from `logit(y) ~ a + b·logit(p)`.
- Monotonicity violations: for each (player-game), `P(over)` must not increase as
  `delta` increases.

**Sanity floor:** B0 must land near 0.25 Brier. If it does not, the harness is broken
and the run is void regardless of other numbers.

---

## 8. Uncertainty

Cluster bootstrap, 10,000 replicates, seed 20261029.

Two independent clusterings, both required:

1. by `game_id` — captures within-game correlation across ~20 players;
2. by `player_id` — captures within-player correlation across seasons.

Percentile 95% CI on ΔBrier. **Both** must exclude zero. Row-level bootstrap is
forbidden: the grid duplicates each player-game 7×, which would understate the
standard error by roughly √7 and manufacture significance.

---

## 9. Promotion rule

The challenger replaces the champion **only if all five hold**:

| # | Condition | Threshold |
|---|---|---|
| 1 | ΔBrier < 0 | point estimate |
| 2 | 95% CI upper bound < 0 | game-clustered **and** player-clustered |
| 3 | ECE (holdout, pooled) | ≤ 0.010 |
| 4 | Calibration slope `b` | within [0.90, 1.10] |
| 5 | Improvement present in ≥ 2 of 3 holdout seasons | per-season ΔBrier < 0 |

Any failure → **the table stays the champion**. A partial win (better Brier, worse
ECE) is a rejection, not a judgement call. That is the negative-binomial lesson
applied in advance.

Condition 2 is one rule with two required tests, so `results.json` reports **six**
booleans (`ci_game_excludes_zero`, `ci_player_excludes_zero`, …) for these five
conditions. "All five hold" means all six booleans are true; there is no reading in
which passing one clustering is enough.

---

## 10. Quality checklist

### 10a. Dataset gates — must pass before upload (recorded)

- [x] Fit rows > 500k (594,251)
- [x] Holdout rows > 60k (70,952)
- [x] No season overlap between fit and holdout
- [x] All three holdout seasons present
- [x] Zero nulls across all 22 columns
- [x] 2025-26 `opp_def100_trail` populated and sane (mean 112.7) — the zeroed-columns trap
- [x] 2025-26 pace sane (mean 102.5)
- [x] Leakage: 1,135 trailing means recomputed from raw SQL, 0 mismatches
- [x] `corr(mu, pts) = 0.719` — plausible, not suspicious
- [x] Unique `(player_id, game_id)`
- [x] Two teams per game
- [x] SHA-256 recorded in manifest

### 10b. Run gates — must pass inside the kernel

All checked values are from the accepted run (kernel v3, 2026-10-29).

- [x] Dataset SHA-256 matches the manifest (fail fast if not) — `9623768…efba8dbc`
- [x] Row count matches 665,203
- [x] Fit/holdout row counts match 594,251 / 70,952
- [x] All model probabilities strictly inside (0, 1) — no clipped 0 or 1
- [x] B0 base-rate Brier within [0.20, 0.30] — 0.2294
- [x] Champion Brier **at δ = 0** within ±0.02 of the previously measured 0.2486 — 0.249044 (see amendment below)
- [x] No NaN in any prediction
- [x] Monotonicity: GBM violation rate < 0.5% — 0.000%, max violation +0.00e+00

### 10b-i. Amendment 2026-10-29 — champion sanity gate was mis-specified

Recorded after the first complete run, before the corrected run was pushed.

The gate as originally written read "champion Brier within ±0.02 of the previously
measured 0.2486". The 0.2486 reference came from the earlier EDA and was measured at
**δ = 0** (line at `floor(mu) + 0.5`, just below the player's mean). The gate,
however, was applied to the champion's **pooled-across-δ** Brier. Those are different
quantities: Brier falls toward the tails because `P(over)` approaches 0 or 1 there.

Kernel v2 therefore failed at 0.2263 against a 0.2486 bar despite correct modelling.
Verified against the frozen dataset before changing anything:

| Quantity | Local recomputation | Kernel v2 |
|---|---:|---:|
| Champion Brier, δ = 0 | 0.249044 | — |
| Champion Brier, pooled over 7 offsets | 0.226308 | 0.2263 |

The δ = 0 figure reproduces the 0.2486 reference within 0.0005, and the pooled figure
reproduces the kernel exactly. So the harness was right and the gate was wrong.

**Change:** the gate now compares the δ = 0 slice against 0.2486, tolerance unchanged
at 0.02. No other gate, metric, threshold, feature, grid point, split, or promotion
condition is touched. The run's verdict is still decided solely by §9.

This amendment is a harness correction, not a result-driven adjustment: it was made
on a gate that produced no model comparison at all, and it was verified independently
of the challenger's scores.

### 10c. Output gates — required artifacts

Produced by kernel v4 (v3 wrote two of them incompletely; see amendment below).

- [x] `results.json` — all metrics, per δ, per season
- [x] `bootstrap.csv` — mean / 95% CI / cluster counts, both clusterings
- [x] `bootstrap_draws.csv` — the 10,000 ΔBrier draws per model × clustering
- [x] `reliability.csv` — bin, predicted, observed, count, pooled and per δ
- [x] `champion_table.json` — the exportable table for Kotlin
- [x] `gbm_bundle.json` — the promoted GBM (4 folds x 200 trees + 4 isotonic maps, 3.69 MB)
- [x] `report.md` — human-readable verdict against §9
- [x] `environment.json` — versions and seeds
- [x] `per_delta.csv` — Brier / log loss / ECE at each offset

If any output artifact is missing, the run does not count.

### 10c-i. Amendment 2026-10-29 — two artifact descriptions overstated the output

Found while auditing the v3 artifacts after the verdict was recorded. Neither affects
any decision: the promotion rule was evaluated on in-memory arrays, not on these
files. Recorded because a checklist that describes files that do not exist is worse
than no checklist.

| Claim | v3 reality |
|---|---|
| `bootstrap.csv` — "10,000 ΔBrier draws" | 6 summary rows (mean, lo, hi, n_clusters). Raw draws were discarded. |
| `reliability.csv` — "per δ" | 75 rows, pooled across all offsets only |

**Change:** the kernel now also writes `bootstrap_draws.csv` (60,000 rows) and
per-offset reliability rows. Kernel v4 re-ran the identical protocol to produce
them, and reproduced every v3 metric to the printed precision — Brier, ECE, slope,
bootstrap mean and both CI bounds all match to 0.00e+00. That is the determinism
check that `BEST_PRACTICES.md §10` asks for, and it passed. No metric, threshold,
feature, grid point, split or promotion condition was touched.

### 10d. Outcome — 2026-10-29

| | |
|---|---|
| Verdict | **PROMOTE C2_gbm** |
| ΔBrier (point) | −0.001520 |
| 95% CI, game-clustered | [−0.001769, −0.001279] |
| 95% CI, player-clustered | [−0.001789, −0.001249] |
| ECE, holdout pooled | 0.00570 (bar ≤ 0.010) |
| Calibration slope | 0.986 (bar 0.90–1.10) |
| Seasons improved | 3 of 3 |
| C1 logistic | REJECTED — worse than the table on every condition except slope |

Recorded at the time, because it contradicts the stated prior:

- The prior in §1 was that the table would win. It did not.
- **H3 rejected.** The gain is not concentrated at \|δ\| ≥ 2 (tail mean −0.001432 vs
  middle mean −0.001638). The mechanism I predicted — exploiting the line's position
  far from μ — is not what happened.
- **H4 confirmed.** `rest_days`, `b2b`, `opp_def100_trail` and `t_usg` together are
  worth 0.000785 Brier, under the 0.001 threshold.
- The champion's pooled ECE is 0.01376, above the 0.010 bar this rule imposes on
  challengers. The rule is asymmetric; it was not changed after seeing results, and
  the verdict does not depend on it, because the GBM beats the table on Brier, log
  loss, ECE, slope, all three seasons and both clusterings simultaneously.
- This is still not an edge claim. No prop quotes exist in the corpus (§12.1).

---

## 11. What each outcome means

| Outcome | Reading |
|---|---|
| Challenger wins all 5 | Table is demoted. Ship the GBM behind a flag, keep the table as fallback. |
| Better Brier, ECE > 0.010 | Reject. This is the NB failure mode: sharper, not truer. |
| Wins only at \|δ\| ≥ 2 | Reject for v1. Real prop lines cluster near the mean; a tail-only win is not where the money is. |
| Tie within CI | Table stays. "Not established" is a result, not a failure. |
| B0 ≈ 0.25 but champion ≫ 0.26 | Harness bug. Investigate before believing anything. |

---

## 12. What this run cannot establish

Stated now, so it cannot be quietly dropped later:

1. **No edge.** There are still no player-prop lines in any dataset we hold. This
   prices a synthetic line at a chosen offset. Edge requires a book's line and price.
2. **No availability model.** The source contains only appearances. The probability a
   player suits up is not modelled, and it matters for EV.
3. **No closing-line movement.** No prop line history exists to test whether the model
   beats the market's own drift.
4. **Conditional on playing.** All probabilities are `P(over | plays)`.
5. **Playoffs excluded** from both fit and holdout.

---

## 13. Sign-off

| | |
|---|---|
| Criteria version | 1.0 |
| Frozen at | 2026-10-29, before kernel push |
| Dataset hash | `9623768…efba8dbc` |
| Grid | 7 offsets |
| Bootstrap | 10,000, seed 20261029 |
| Decision rule | all 5 conditions in §9 |
| Amendments | §10b-i (2026-10-29): champion sanity gate now compares the δ = 0 slice. Harness only; no modelling or promotion change. |
