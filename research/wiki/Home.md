# PropsLab wiki

Measured baselines for the on-device model. Every number in these pages came from this
project's own datasets and frozen run artifacts — nothing here is estimated, remembered
or rounded in your favour.

**Generated:** 2026-10-29 · **Source of truth:** `research/wiki/baselines.json`
(regenerate with `python research/wiki_baselines.py` from the repo root)

## Rules for the assistant

1. **Quote, don't invent.** A figure may only be repeated from a page in this wiki or
   from `StrategyNotes`. If the answer is not here, say the wiki does not cover it.
2. **Keep the source attached.** State which page a number came from when it matters.
3. **Respect the two labels.** `recomputed` = derived from the parquet in this
   workspace, so it can be re-checked at any time. `recorded` = measured earlier against
   raw sources that are no longer in this workspace, but preserved as artifacts here
   (`eda_stage2-5.json`, `results.json`) and copied into `baselines.json` verbatim —
   quotable, though not independently re-derivable.
4. **Never cross the frames.** The wiki draws on three different row counts
   (665,203 leakage-free extract · 721,198 EDA frame · 19,810 betting rows). Mixing them
   in one sentence produces a false comparison — each page says which frame it uses.
5. **Descriptive is not causal.** Schedule splits (home, rest, back-to-back) are raw
   averages with nothing controlled for. Present them as observations.

## Pages

| Page | What it holds |
|---|---|
| [Dataset.md](Dataset.md) | Provenance, shape, split, gates, leakage rules, column meanings |
| [Player-games.md](Player-games.md) | Point distributions, variance structure, schedule splits, era trend |
| [Model-metrics.md](Model-metrics.md) | The frozen challenger run: Brier, calibration, bootstrap, per-delta, gates |
| [Formula-benchmarks.md](Formula-benchmarks.md) | Candidate formulas, promotion criteria, model bake-offs, push modelling |
| [Pricing-and-market.md](Pricing-and-market.md) | Vig/hold identities, market calibration, Kelly checks |
| [Insights.md](Insights.md) | The cross-cutting findings, each with its number and its caveat |

## The five numbers to know first

| Fact | Value | Page |
|---|---|---|
| Leakage-free training rows | 665,203 player-games, 1996-97 → 2025-26 | Dataset |
| Best model (frozen run, 2026-10-29) | constrained GBM, Brier 0.22479 vs champion table 0.22631 | Model-metrics |
| Improvement | ΔBrier −0.001520, 95% CI [−0.001769, −0.001279] game-clustered | Model-metrics |
| Calibration of the winner | ECE 0.00570, slope 0.986, improved all three holdout seasons | Model-metrics |
| Spread of the target | points sd 8.24 on mean 10.11 (var/mean 6.72) — far too wide for a Poisson | Player-games |
