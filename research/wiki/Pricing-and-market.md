# Pricing and market — the no-vig frame

**Sources:** `research/derivations/propslab-pricing.md` (SymPy-verified derivations),
`research/results.json` (Python bench against the betting CSV, `recorded`), the frozen
run's `per_delta.csv`, and `StrategyNotes` in the app. Arithmetic marked "worked" is
hand-checkable on the page.

## 1. American odds → probability (the two conversions)

- **Decimal:** `1 + 100/|a|` for negatives, `1 + a/100` for positives.
  −110 → 1.909, −105 → 1.952, −100 → 2.00, +100 → 2.00, +120 → 2.20, +150 → 2.50, +300 → 4.00.
- **Implied probability:** `1/decimal`.
  −110 → 0.5238, −105 → 0.5122, −100 → 0.5000, +100 → 0.5000, +120 → 0.4545,
  +150 → 0.4000, +300 → 0.2500.

**Break-even win rate = implied probability.** −110 needs 52.4%; +120 needs 45.5%.

## 2. No-vig frame and hold (worked + `recorded`)

Two-way: `no-vig_p(over) = p_over / (p_over + p_under)`, and
`hold = 1 − (1/dec_fav + 1/dec_dog)`.

- **Worked −110/−110:** 0.5238 / 0.5238 → 0.5000 each side; hold 4.76%.
- **Worked −115/−105** (the pair the app actually imports, from `FeedTest`):
  0.53488 / 0.51220 → sum 1.04708 → no-vig over **0.51083**, hold **4.50%**.

`recorded` across 19,810 of 24,440 NBA games with both moneylines
(`results.json` f1):

| | |
|---|---|
| Mean hold | **3.745%** (median 3.759%, p95 4.708%, max 6.322%) |
| Rows with negative hold | 0 |
| Mean no-vig p(home) | 0.5881 |
| Identity checks | `−110/−110` → 0.500000 / 4.7619% ✓; probabilities sum to 1 every row ✓ |

## 3. The market itself is the benchmark (`recorded`)

De-vigged moneyline probabilities against actual outcomes, 19,810 rows
(`results.json` f2):

| | |
|---|---|
| Brier | **0.202329** (coin flip 0.25) |
| Log loss | 0.589246 |
| ECE, 10 bins | **0.005610** |
| Home win rate | 0.58662 |
| Same figures with vig included | Brier 0.202962, log loss 0.591227 |

The no-vigged market is calibrated to ±0.6 points out of the box. Any model that wants
to beat it has to clear both its Brier and its ECE — and note this is the *betting-rows*
frame, not the player-games frame of [Model-metrics.md](Model-metrics.md); the two
Briers are not comparable across frames (wiki rule 4).

## 4. Kelly: no bet is positive at the vigged price (`recorded` + worked)

`results.json` f3, 19,810 games:

| | |
|---|---|
| Games with a positive Kelly stake at the vigged price | **0 of 19,810** |
| Max f\* across the sample | −0.00026 |
| Mean f\* | −0.1207 |
| Closed form vs numeric argmax | matched in every case, max error 2e-6 |

**Worked quarter-Kelly at −110/−110.** With b = 0.909, edge e defined the way the app
defines it (model over − no-vig over, so p = 0.5 + e), Kelly simplifies to
**f = 2.1·e − 0.05**:

| edge e (pts) | p | full f | ¼ f |
|---:|---:|---:|---:|
| 0.00 | 0.5000 | −0.050 → no bet | — |
| 0.02 | 0.5200 | −0.008 → no bet | — |
| 0.04 | 0.5400 | +0.034 | 0.0085 |
| 0.06 | 0.5600 | +0.076 | 0.0190 |
| 0.08 | 0.5800 | +0.118 | 0.0295 |
| 0.10 | 0.6000 | +0.160 | 0.0400 |

The hold taxes **2.38 points of edge** before full Kelly turns positive — below that,
the growth-optimal stake is zero, not small. The screen shows full Kelly and stakes
quarter Kelly because the app's probabilities are a baseline, not a proven edge
(`StrategyNotes`).

## 5. Calibration by line offset (frozen run, `recomputed` from `per_delta.csv`)

Level bias (mean p − observed) and ECE per offset, holdout 70,952 rows per offset:

| δ | champion ECE | C2_gbm ECE | champion bias | C2_gbm bias |
|---:|---:|---:|---:|---:|
| −3 | 0.01526 | 0.00810 | +0.0148 | +0.0081 |
| −2 | 0.01723 | 0.01211 | +0.0164 | +0.0087 |
| −1 | 0.01768 | 0.01266 | +0.0177 | +0.0087 |
| 0 | 0.01562 | 0.01277 | +0.0155 | +0.0077 |
| +1 | 0.01312 | 0.00895 | +0.0130 | +0.0047 |
| +2 | 0.01142 | 0.00686 | +0.0111 | +0.0004 |
| +3 | 0.00893 | 0.00727 | +0.0079 | +0.0012 |

**Both models are optimistic at every offset** — the champion more so — and the
champion's worst calibration sits at δ = −1, where the line sits just under the mean
and the bet is hardest. The GBM roughly halves the level bias everywhere; its cleanest
offset is +2 (bias +0.0004), i.e. where the line is above the player's mean.
