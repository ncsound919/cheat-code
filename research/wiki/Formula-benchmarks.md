# Formula benchmarks

Every benchmark on this page has an artifact on disk. A previous draft of this page
carried additional tables (a six-arithmetic-formula bench, a live-book pipeline bench,
NBA/MLB line-vs-BAE, an ice-hockey h2h study, a 47-season moneyline prior, a synthetic
sandwich sweep) whose numbers appear **nowhere** in this workspace and whose inputs
(hockey, MLB data) do not exist in the datasets at all. They were removed rather than
cited — see [Insights.md](Insights.md) #10 for the standing rule.

## 1. The five-candidate bench (holdout 2023-24 → 2025-26, `recomputed`)

Source: `research/formula_runs/formula_comparison.json` (= `.recorded.formula_candidates`
in `baselines.json`). Same frame as the frozen run: 70,952 holdout rows, observed over-rate
0.48355, line grid `floor(mu) + 0.5 + δ` for δ ∈ [−3 … +3].

| Candidate | Brier | log loss | ECE | ΔBrier vs champion | 95% CI | Promotable |
|---|---:|---:|---:|---:|---|---|
| champion_table (μ lookup) | 0.22631 | 0.64086 | 0.01376 | — (reference) | — | n/a |
| normal_tsd (Gaussian from trailing sd) | 0.22952 | 0.65534 | 0.02304 | +0.00321 | [+0.00293, +0.00350] | no |
| logistic_simple | 0.22986 | 0.65198 | 0.02602 | +0.00356 | [+0.00341, +0.00370] | no |
| poisson (from μ) | 0.23837 | 0.68414 | 0.09329 | +0.01207 | [+0.01176, +0.01238] | no |
| empirical_shrink | 0.24491 | 0.69486 | 0.07529 | +0.01858 | [+0.01766, +0.01954] | no |

**No arithmetic candidate beats the lookup table** — every CI is strictly positive.
The poisson is the worst of the four and its slope (0.447) says why: it compresses
probabilities toward the middle. The one model that did beat the champion is the
constrained GBM (ΔBrier −0.001520), documented in [Model-metrics.md](Model-metrics.md).

## 2. Half-line bake-off — parametric vs empirical (`recorded`)

Source: `eda_stage4.json` via `baselines.json` (`.recorded.eda_models.half_line`).
Half-integer lines only, so no pushes; observed over-rate 0.4724; a coin flip scores
Brier 0.250.

| Model | Brier | log loss | ECE | mean p |
|---|---:|---:|---:|---:|
| empirical (lookup) | 0.24906 | 0.69126 | **0.00631** | 0.46876 |
| poisson | 0.24994 | 0.69311 | 0.01921 | 0.47480 |
| negative binomial | 0.25367 | 0.70109 | 0.05536 | 0.41690 |
| own-history only | 0.26076 | 0.71725 | 0.07204 | 0.46756 |
| coin flip | 0.25000 | — | — | 0.50000 |

**The parametric models barely beat a coin flip, and the NB is worse than one.** The
empirical lookup is the only entry with both the best Brier and an ECE under 0.01 —
this is the measured basis for the app pricing from the lookup table rather than from a
distributional assumption.

## 3. Baseline calibration on the holdout (`recorded`)

Source: `eda_stage3.json` (`.holdout_calibration_half_line`).

| | |
|---|---|
| n | 20,000 |
| mean p (over) | 0.4133 |
| observed over-rate | 0.4591 |
| Brier (μ-blend baseline) | 0.25292 |
| Brier (poisson) | 0.24949 |
| ECE (baseline) | **0.04547** |

The μ-blend baseline under-predicts the over rate by 4.6 points on this frame — an
optimism/level error of the same family the frozen run measured (+1.37 pp; see
[Model-metrics.md](Model-metrics.md)). The gate's ECE ≤ 0.010 exists because a 0.045
ECE would otherwise look like a working model.

## 4. Push modelling — push mass is large and predictable (`recorded`)

Source: `eda_stage4.json` (`.push`).

| | |
|---|---|
| n | 25,000 |
| observed push rate | 0.07212 |
| NB predicted push | 0.07259 |
| lookup predicted push | 0.07391 |
| push Brier (NB) | 0.06652 |
| push Brier (lookup) | 0.06602 |

At whole-number lines ~7.2% of outcomes land exactly on the line (the integer-line
table in [Player-games.md](Player-games.md) shows rates up to 19% at line 2). Both
modellers land within 0.2 points of the truth, so push is the *most* predictable part
of a prop — which is why the benchmark grid uses half-lines (push impossible) and why
the app prices push explicitly at integer lines instead of rounding it away.

## 5. How benchmarks are run in this project

```
python research/kaggle/validate_dataset.py     # dataset gates (all pass)
python research/wiki_baselines.py              # regenerates wiki/baselines.json
python research/mathx/verify_formulas.py       # 16/16 symbolic checks (SymPy)
python research/formula_bench.py               # pricing identities vs the betting CSV
./gradlew :app:testDebugUnitTest               # Kotlin suite (109 tests)
```

Documented in `research/FORMULA-TESTS.md`. Promotion criteria (frozen before execution)
are in `research/CRITERIA.md`: ΔBrier < 0, both cluster-bootstrap CIs excluding zero,
ECE ≤ 0.010, slope ∈ [0.9, 1.1], improvement in ≥ 2 of 3 holdout seasons.
