# Derived insights — what the numbers say together

Each insight names its source page so the local model can cite rather than improvise.
Nothing on this page rests on an artifact that is not in the workspace.

## 1. The variance floor: 6.7 points per game, two thirds of it irreducible noise
[Player-games.md](Player-games.md): var/mean 6.72, within-player share 67.2%. A single
player-game can be predicted to about ±4.4 points (`mu` MAE 4.38). Any claimed edge
that "predicts" better than that should be treated as extraordinary — and one game's
noise (sd 8.24) is larger than any realistic edge, which is why the promotion gate
demands 400 bets at t ≥ 2 instead of trusting a hot week.

## 2. The frozen challenger wins mostly by removing bias
[Model-metrics.md](Model-metrics.md): the champion table is +1.37 pp optimistic; the
GBM's ΔBrier −0.001520 (95% CI [−0.001769, −0.001279] game-clustered) roughly equals
the bias it corrected, and its level bias drops to +0.56 pp. The ablation with a
*better* ECE (0.00488) still scores worse on Brier — calibration and sharpness are
separate axes and the gate tests both. Per [Pricing-and-market.md](Pricing-and-market.md) §5,
the improvement holds at all seven line offsets.

## 3. Every straight bet at the vigged price has negative Kelly growth
[Pricing-and-market.md](Pricing-and-market.md) §4: **0 of 19,810 games** have a
positive Kelly fraction at the price the book actually offers; mean f\* = −0.12.
Combined with the measured mean hold of 3.745% (§2), this is the arithmetic behind the
app's refusal to bet without an independent probability: at the vig, the growth-optimal
stake is not small, it is **zero**.

## 4. The hold taxes 2.38 points of edge before betting pays at all
[Pricing-and-market.md](Pricing-and-market.md) §4: at −110, full Kelly is
f = 2.1·e − 0.05, so an edge under 2.38 points over the no-vig price yields a negative
stake. The quarter-Kelly column is what the app stakes precisely because its edge
estimate is a baseline — sizing is where a marginal model survives, and this formula
shows the cliff it has to stay right of.

## 5. Push mass is the most predictable part of a prop
[Formula-benchmarks.md](Formula-benchmarks.md) §4: observed push 0.0721, both models
predict within 0.2 points (push Brier ~0.066), and at integer lines the rate climbs to
19% at line 2. Half-integer lines eliminate pushes by construction — that is why the
benchmark grid uses them — while the app at real whole-number lines models push
explicitly rather than rounding it away.

## 6. Parametric distributions lose to a lookup table on this target
[Formula-benchmarks.md](Formula-benchmarks.md) §2: at half lines the empirical lookup
scores Brier 0.24906 with ECE 0.0063, the poisson 0.24994 (barely better than the 0.25
coin flip), the negative binomial 0.25367 — *worse* than a coin flip — and own-history
0.26076. [Player-games.md](Player-games.md) explains why: var/mean = 6.72 shatters the
Poisson's equal-variance assumption before the game starts.

## 7. No arithmetic candidate beats the champion — only the GBM did
[Formula-benchmarks.md](Formula-benchmarks.md) §1: normal_tsd, logistic, poisson and
empirical_shrink all have strictly positive ΔBrier CIs against the lookup table. The
one model in the project that beat it is the constrained GBM
([Model-metrics.md](Model-metrics.md)). "Try a different formula" has been measured and
failed; "add the line position as a first-class feature" is what worked.

## 8. Calibration degrades exactly where the bet gets interesting
[Pricing-and-market.md](Pricing-and-market.md) §5: both models are optimistic at every
offset, worst at δ = −1 (line just under the mean) and best at δ = +3. The offsets that
are easiest to calibrate are not the ones a bettor wants — so calibration numbers must
always be quoted with their δ, never pooled (the pooled ECE, 0.0057–0.0138, hides a
per-offset spread of 0.007–0.018).

## 9. The era blind spot is documented, not hidden
[Player-games.md](Player-games.md): the holdout era scores +0.94 points over the
training era, and the frozen run's +1.37 pp level bias is the visible consequence of
training ≤ 2022-23. The gate's "improves in ≥ 2 of 3 holdout seasons" test is the guard
against era-fitting; retraining cadence is a known, quantified cost.

## 10. What this wiki will not do
Per `StrategyNotes`: the model may rephrase these notes but never add a figure to them.
Every number above names a page whose figures trace to an artifact on disk
(`baselines.json`, `per_delta.csv`, `results.json`, `eda_stage*.json`, `formula_comparison.json`).
A previous draft of the wiki carried tables with no artifact anywhere in the workspace —
a six-formula bench, a live-book pipeline bench, NBA/MLB line-vs-BAE, an ice-hockey
h2h study, a 47-season moneyline prior and a sandwich sweep — and they were deleted
rather than citable. If a number is not here, the answer is "the wiki does not cover
it", not an estimate.
