# Formula tests — run 2026-10-29

Two independent checks, because the app and the research bench are separate
implementations of the same arithmetic.

## 1. Kotlin JVM suite

```
gradle :app:testDebugUnitTest
```

**Result: 287 tests across 26 suites, 0 failures, 4 skipped** (the skipped cases are
the live ESPN smoke checks, which only run when their environment variable is set).
`DomainTest` (8) and `FormulaTest` (15) cover the domain layer; `MathXTest` (8)
covers the Math X formula lab, the ported prompt modes, the probability guard and
the local-model fallback. The rest of the suite covers the feed parsers, fantasy
desk, parlay arithmetic, settlement, the practice ledger and the agent tool policy.

The new layer under test is `domain/Formulas.kt`:

| Formula | What is asserted |
|---|---|
| `Pricing.hold` | equals the sum of both implied probabilities minus 1; `−110/−110` is exactly 4.7619%; a normal book is positive and an arbitrage is representable |
| `Pricing.edgePoints` | model minus no-vig market in probability points; antisymmetric; rejects non-probabilities |
| `Pricing.fairAmerican` | the fair price makes EV zero within 0.02 units across 57 price/distribution cases; push mass raises the break-even price (`0.50` with 10% push is `−125`); the result is always a valid American quote |
| `Staking.expectedValue` | matches the outcome sum; scales with stake; sign follows the price around fair |
| `Staking.kellyFraction` | zero at fair, negative below fair, matches a 1,000-point numeric maximisation of `p·ln(1+bf) + q·ln(1−f)` within 0.002; push case reduces correctly; boundaries validated |
| `OutcomeProbabilities` | rejects out-of-range, NaN, infinite and non-normalised triples |
| `PredictionGate` | exact 6 h / 24 h boundaries are usable; one millisecond older is rejected |

Table-driven loops cover the full grid of American prices `−1000 … +1000` for
odds conversion, no-vig symmetry, hold identity and EV.

The Math X lab is the in-app counterpart of this suite: four formulas, 15 checks,
executed against the same Kotlin functions. Its independent SymPy reference is
`research/mathx/verify_formulas.py`; see section 3.

Two real defects were found by compiling and running the tree:

1. `GbmBundleScorer.kt` was missing its class-closing brace, so the module did not
   compile at all.
2. `GbmBundleScorer` only checked features its trees happened to touch. A bundle
   with an unused feature silently scored a row that omitted that feature. It now
   validates the whole frozen feature schema, rejecting missing and infinite values
   before the walk. The existing `gbmScorerParityAndGuards` test covers it.

## 2. Math X formula derivation and SymPy reference

```
python research/mathx/verify_formulas.py
```

**Result: 16 of 16 symbolic/numeric checks pass.** The script derives the no-vig
identity, push-aware EV and fair price, the push-aware Kelly stationary point, and
the Platt calibration properties. It writes
`research/derivations/propslab-pricing.md`.

This is the research-side half of the Math X ingestion; the app-side half is
`com.propslab.mathx.FormulaLab`, which runs the same claims against Kotlin. The two
implementations are deliberately independent, so a disagreement is visible. No
model is called: Math X's Formula Lab would supply prose, not authority, and the
phone ships no weights yet. See `research/MATHX-INTEGRATION.md`.

## 3. Python bench against the real book

```
python research/formula_bench.py
```

Source: `nba_2008-2026.csv`, 19,810 games with both moneylines. Output:
`research/results.json`, `research/report.md`.

| Check | Result |
|---|---|
| No-vig of `−110/−110` | 0.500000, hold 4.7619% |
| Mean hold across the book | 3.745% (median 3.759%, p95 4.708%) |
| Rows with negative hold | 0 |
| Market Brier (de-vigged) | 0.202329 (coin flip 0.25) |
| Market log loss | 0.589246 |
| Market ECE, 10 bins | 0.005610 |
| Kelly closed form vs numeric argmax | matched in every case, max error 2e-6 |
| Positive Kelly stakes at the vigged price | 0 of 19,810 |

These reproduce the figures recorded in `PROPSLAB-ECOSYSTEM-PLAN.md §9` exactly.
That is the useful part: the Kotlin formulas and the Python reference agree on the
identities, and the bench's conclusion is unchanged — the de-vigged market
probability is not an edge against the same book.

## What is still not tested

No prop quotes exist, so `marketOver`, `edgePts` and EV are tested as arithmetic,
not against a real sportsbook. The promoted GBM bundle is parity-checked against
Python to 1.1e-16 over 120 sampled rows (`harness/gbm_artifact_parity.py`), but it
is not on the live board path: the odds feed does not carry five of its twelve
features, so `ChampionTable` remains the shipped model. The local model boundary is
tested with fakes and the uninstalled path; no real quantised model has produced a
reply inside the app.
