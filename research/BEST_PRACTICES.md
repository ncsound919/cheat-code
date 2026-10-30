# Best practices before training — PropsLab challenger model

Written before any training run. Sources are listed at the end; every claim that
drives a design decision is cited, and every practice is tied to a concrete choice
in this project rather than left as general advice.

---

## 0. What is being trained, and what is not

This run trains a **tabular probability model**: given a player's recent form and a
prop line, output `P(points > line)`. It is not an LLM, and no language model is in
the scoring path.

That is a deliberate scoping decision with three grounds:

1. The champion already has ECE 0.0027 on 25k out-of-time rows. The input is ~10
   numbers. A 4B-parameter model has no representational advantage on 10 numbers.
2. The app's own contract (`README.md:33`) says the assistant never supplies a
   probability. Putting a generative model in the scoring path breaks the guarantee
   that makes settlement auditable.
3. Tabular GBM on 600k rows is a CPU job measured in minutes. Spending GPU quota on
   it would be theatre.

So "training" here means: fit a challenger that is *allowed* to use information the
champion structurally cannot, and let a pre-registered test decide.

---

## 1. Platform facts (verified against Kaggle docs, not memory)

| Constraint | Value | Consequence for this run |
|---|---|---|
| Dataset size limit | 200 GB per dataset | Our extract is ~50 MB; irrelevant |
| Notebook session | 12 h CPU/GPU, 9 h TPU | One script kernel, well inside |
| Persisted disk | 20 GB (`/kaggle/working`) | Results + table JSON only |
| RAM | 29 GB on T4×2, 30 GB CPU-only | Grid of 7 lines × 663k rows fits easily |
| Interactive idle | 20 min | Never use interactive; push a script kernel |
| Default visibility | private | Correct: this is derived personal research |
| Archives | uncompressed server-side | Parquet, not zip-of-parquet |

Two operational rules follow:

- **Pin the dataset version in the kernel** (`dataset_sources`), so a re-run six
  months from now cannot silently pick up different bytes.
- **Write results to `/kaggle/working` as files**, not just stdout. Notebook logs are
  ephemeral; files are the artifact of record.

Source: Kaggle docs, *Datasets* (200 GB limit) and *Notebooks* (technical
specifications, 12 h / 20 GB / RAM table).

---

## 2. Score with proper scoring rules; treat ECE as a diagnostic

This is the single most important methodological point, and it is easy to get wrong.

**Brier and log loss are strictly proper.** They decompose into reliability,
resolution and uncertainty (Murphy 1973). Minimising them is the correct objective
for a probability model that will be used to make decisions.

**ECE is not a proper scoring rule.** It is a binned diagnostic. It depends on the
number of bins, the binning scheme, and the sample size, and it can be reduced by
degenerate predictions. Nixon et al. (2019) show that conclusions about which
recalibration method is best "are drastically impacted by the choice of calibration
measure", and recommend adaptive binning and the L2 norm for stability.

**A lower Brier score does not prove better calibration.** scikit-learn's own
calibration guide states it plainly: a lower Brier loss "does not necessarily mean a
better calibrated model, it could also mean a worse calibrated model with much more
discriminatory power."

So the rule for this project:

| Role | Metric |
|---|---|
| **Selection / promotion** | Brier (primary), log loss (secondary) |
| **Diagnostic** | ECE with equal-mass adaptive bins, reliability curve, calibration slope |
| **Never** | Selecting a model *because* its ECE is lower |

A model that wins Brier but drifts ECE is exactly the negative-binomial failure from
the earlier EDA (ECE 0.055 vs 0.003 while claiming to model the same thing). The
promotion rule therefore requires **both**: win on Brier *and* hold ECE.

### Calibration slope and intercept

Fit `logit(y) ~ a + b·logit(p)` on the holdout. Ideal is `a=0, b=1`.

- `b < 1` → overconfident (predictions too extreme)
- `b > 1` → underconfident
- `a ≠ 0` → systematic bias

This is reported because ECE can look fine while the slope is badly off in the tails,
and prop betting lives in the tails.

Sources: Murphy (1973); Nixon et al. (2019); scikit-learn *Probability calibration*.

---

## 3. Split by time and by group — never at random

The data is a panel: repeated observations of players across 30 seasons. A random
row split leaks in at least three ways:

1. **Same-game leakage.** 20-30 players share one game. If a game's rows land in both
   train and test, the model has seen the game's pace, defensive context and outcome
   neighbourhood.
2. **Same-player leakage.** Consecutive games of one player are highly correlated
   (year-over-year scoring-rate correlation measured at 0.86). A player in both
   splits lets the model memorise the player, not the process.
3. **Temporal leakage.** Any season-level normalisation (pace, era, league scoring
   level) computed over the whole dataset imports the future into the past.

The design:

- **Fit:** seasons through 2022-23.
- **Holdout:** 2023-24, 2024-25, 2025-26 — strictly later, never touched during
  fitting or calibration.
- **Within-fit resampling** for calibration uses `GroupKFold` keyed on **season**, so
  a calibration fold never shares a season with its training fold.
- **Every feature is `shift(1)`-lagged.** A game is never in its own predictors. This
  was already the rule in the EDA scripts and it carries over.

The analogue in finance is purged/embargoed cross-validation (López de Prado, 2018):
when observations overlap in time, adjacent samples must be removed from the training
fold. Our lag structure plus season-grouped folds provides the same protection.

---

## 4. Calibrate on data the model has not seen

scikit-learn is explicit: "Ideally, the calibrator is fit on a dataset independent of
the training data used to fit the classifier." Fitting a calibrator on in-sample
predictions produces a calibrator that is too sharp.

Two valid ways to get that independence:

| Method | Cost | Bias |
|---|---|---|
| Hold out a calibration slice | 1 fit | wastes data |
| Cross-fitted (out-of-fold) predictions | k fits | none, uses all data |

This run uses **cross-fitted calibration** (`CalibratedClassifierCV(ensemble=False)`
with season-grouped folds): out-of-fold predictions produce the calibrator, then the
base model is refit on all fit-period data. Both champion and challenger therefore
see the same 592,912 rows — no side is handicapped by a smaller window.

**Platt vs isotonic.** scikit-learn: isotonic "will perform as well as or better than
sigmoid when there is enough data (greater than ~1000 samples)". We have ~4M
grid-expanded rows and a single monotone input-output relationship, so isotonic is
appropriate. It is also monotone, which preserves the ordering that the physics
requires.

---

## 5. Encode the physics as constraints

`P(over)` must be **non-increasing in the line** and **non-decreasing in the player's
mean**, holding everything else fixed. This is not a modelling preference; it is a
logical requirement. A model that says a 27.5 line is easier to beat than a 26.5 line
for the same player is wrong regardless of its Brier score.

Both scikit-learn's `HistGradientBoostingClassifier` and LightGBM/XGBoost support
monotonic constraints. We set `+1` on `mu`, `-1` on `line`, and then **verify** on the
holdout by counting violations across the δ grid. A model that needs the constraint to
behave is telling you something; a model that violates it without the constraint is
broken.

This is also the mechanism by which a learned model can plausibly beat a lookup table:
the table's answer is a step function of a bucketed mean, while a constrained GBM is
smooth in `(mu, line)` and can use the line's *position* relative to the mean, which
the table ignores.

---

## 6. Always carry a trivial baseline and a strong champion

Two reference points, both cheap:

- **Base rate:** predict the pooled `P(over)` for every row. Brier ≈ 0.25. If a model
  cannot beat this, something is wrong with the pipeline, not the model.
- **Champion:** the μ-keyed empirical distribution table, evaluated as a full CDF so it
  can answer *any* line, not just the one it was tuned on. This is the model that must
  be beaten. It already achieves ECE 0.0027 out of time.

Reporting only "challenger vs nothing" is how weak models get shipped.

---

## 7. Pre-register, then run once

The dominant failure mode in this kind of work is not a bad model — it is running
twenty variants and reporting the one that looked good. Every extra degree of freedom
(which seasons, which features, which binning, which metric) is a chance to fit noise.

Mitigations, all fixed before the run:

- Feature list frozen (no adding a feature after seeing holdout results).
- Line grid frozen at 7 offsets.
- Metrics and promotion thresholds frozen.
- One holdout evaluation. If the challenger fails, it is not re-tuned against the
  holdout — it goes back to the fit period and the whole protocol restarts with a
  fresh holdout, which we do not currently have.

This is the garden-of-forking-paths problem, and the only real defence is to decide
the test before seeing the answer.

---

## 8. Uncertainty must respect the dependence structure

A naive bootstrap over evaluation rows would be wrong here for two reasons:

1. Each player-game contributes **7 rows** (one per line offset). Those rows are
   near-duplicates of the same underlying event.
2. Players share games, and games share seasons.

So: **cluster bootstrap**. Resample clusters with replacement, recompute ΔBrier on the
resampled clusters, and report percentile CIs. This run reports **two** clusterings —
by `game_id` and by `player_id` — and requires both to exclude zero before claiming an
improvement. If the two disagree, the honest conclusion is "not established", which is
the same conclusion reached for Jev's sector routing.

10,000 replicates, fixed seed.

---

## 9. Do not resample for imbalance

The positive rate varies with the line offset, roughly 0.2–0.8 across the grid, so
imbalance is mild. SMOTE-style oversampling distorts the base rate and therefore
destroys calibration, which is the one property we are measuring. No resampling. No
class weights. The output must remain a probability, not a score.

---

## 10. Reproducibility is part of the result

- Fixed seeds for every stochastic step.
- Dataset pinned by version, plus SHA-256 in the dataset manifest.
- Environment recorded (Python, numpy, pandas, sklearn versions).
- Results written as machine-readable JSON, not prose only.
- The champion table exported as a deterministic JSON artifact so the Kotlin
  implementation can be diffed against the Python one number for number.

A result that cannot be reproduced on a fresh kernel is an anecdote.

---

## 11. Anti-patterns this run explicitly refuses

| Anti-pattern | Why it is refused |
|---|---|
| Random K-fold on panel data | leaks players, games and seasons |
| Selecting on ECE | not a proper scoring rule |
| Tuning on the holdout | destroys the only out-of-time estimate we have |
| Adding features after seeing results | multiplicity; the reported CI becomes meaningless |
| Reporting Brier without a CI | a 0.0005 difference on 70k rows may be noise |
| Row-level bootstrap with duplicated grid rows | understates variance by ~√7 |
| Resampling for "balance" | breaks calibration |
| Trusting precomputed rating columns | `def_rating`/`pace` are 0 for 93% of 2025-26 |
| Treating `available_flag` NaN as "did not play" | the table contains only appearances |
| Shipping a model because perplexity/accuracy improved | the coding-adapter lesson: task metric or nothing |

---

## 12. Two dataset traps found during this audit

These are recorded because both would silently corrupt a training run.

**Trap 1 — zeroed advanced stats.** In `team_boxscores`, `def_rating`, `pace` and
`poss` are populated for every season through 2024-25, then **0 for 93.1% of 2025-26**
— the most recent holdout season. Any opponent-strength feature built on those columns
would be missing exactly where it is evaluated. Fix: derive pace and defensive strength
from raw counting stats (`fga`, `fta`, `oreb`, `tov`, `pts`), which are 100% populated
in every season.

**Trap 2 — appearances only.** `player_boxscores` contains 787,531 rows and only 56
with zero minutes; `min` is never null. There are no DNP rows. So:

- "Did the player play?" is **not learnable from this table**. Any availability model
  needs a different source (injury reports, active rosters).
- All labels are conditional on playing. That matches how books settle props (a DNP
  voids the bet), but it means the model prices *given he plays*, and the EV of a bet
  additionally depends on the probability he plays.

`available_flag` is NaN for 51% of rows and 2.0 for 3,160 rows, so it is not a usable
label either.

---

## Sources

1. Kaggle, *Datasets* documentation — 200 GB per dataset; archive handling.
   https://www.kaggle.com/docs/datasets
2. Kaggle, *Notebooks* documentation — 12 h CPU/GPU sessions, 20 GB working disk,
   29-30 GB RAM, 20 min interactive idle.
   https://www.kaggle.com/docs/notebooks
3. Kaggle CLI, *Datasets metadata* — required fields, licenses, update frequency.
   https://github.com/Kaggle/kaggle-cli/blob/main/docs/datasets_metadata.md
4. Kaggle CLI, *Kernels metadata / Kernels commands* — `kernel-metadata.json`,
   accelerators, `kernels push/output/status`.
   https://github.com/Kaggle/kaggle-cli/blob/main/docs/kernels_metadata.md
5. scikit-learn, *1.16 Probability calibration* — proper scoring rules, Platt vs
   isotonic, calibrator must be fit on disjoint data, Brier caveat.
   https://scikit-learn.org/stable/modules/calibration.html
6. A. H. Murphy (1973), *A New Vector Partition of the Probability Score*,
   J. Appl. Meteorol. 12(4), 595-600.
7. J. Nixon et al. (2019), *Measuring Calibration in Deep Learning*,
   arXiv:1904.01685 — ECE flaws, adaptive binning, L2 norm.
8. C. Guo et al. (2017), *On Calibration of Modern Neural Networks*, ICML —
   temperature scaling, overconfidence.
9. A. Niculescu-Mizil & R. Caruana (2005), *Predicting Good Probabilities with
   Supervised Learning*, ICML — isotonic vs Platt, sample-size guidance.
10. C. Walsh & A. Joshi (2023), *Machine learning for sports betting: should model
    selection be based on accuracy or calibration?*, arXiv:2303.06021 — NBA data;
    calibration-based selection returned +34.7% ROI vs −35.2% for accuracy-based.
11. M. Uhrín et al. (2021), *Optimal sports betting strategies in practice*,
    IMA J. Management Mathematics — fractional Kelly as the robust practical choice.
12. M. López de Prado (2018), *Advances in Financial Machine Learning* — purged and
    embargoed cross-validation for overlapping observations.
