"""
Three-formula bench for PropsLab.

Runs the only three pieces of maths the app cannot get wrong, against real
closing moneylines (24,440 NBA games, 2007-10-28 -> 2026):

  F1  American -> decimal -> implied -> two-way no-vig, plus the hold
  F2  Brier / log loss / ECE of the de-vigged market probability vs the result
  F3  Kelly fraction: closed form, growth-rate maximisation, and the
      "no edge after vig" check

Input : D:\\datasets\\sports-datasets\\kaggle-downloads\\2026-10-28\\
        cviaxmiwnptr-nba-betting-2008-2026\\nba_2008-2026.csv
Output: formulas/results.json, formulas/report.md

No model is fit. This measures the market and checks the arithmetic; it is the
benchmark any PropsLab model has to beat, not a strategy.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

import numpy as np
import pandas as pd

SRC = Path(
    r"D:\datasets\sports-datasets\kaggle-downloads\2026-10-28"
    r"\cviaxmiwnptr-nba-betting-2008-2026\nba_2008-2026.csv"
)
OUT = Path(__file__).resolve().parent
CLIP = 1e-6
BINS = 10


# ---------------------------------------------------------------- F1: pricing
def american_to_decimal(ml: float) -> float:
    """Mirror of Odds.americanToDecimal in app/src/main/java/com/propslab/domain/Odds.kt."""
    if ml > 0:
        return 1.0 + ml / 100.0
    return 1.0 + 100.0 / -ml


def implied(ml: float) -> float:
    return 1.0 / american_to_decimal(ml)


def no_vig_two_way(ml_a: float, ml_b: float) -> tuple[float, float]:
    """(p_a, hold). p_a is the de-vigged probability of side a."""
    ia, ib = implied(ml_a), implied(ml_b)
    total = ia + ib
    return ia / total, total - 1.0


# ----------------------------------------------------------------- F2: scores
def brier(p: np.ndarray, y: np.ndarray) -> float:
    return float(np.mean((p - y) ** 2))


def log_loss(p: np.ndarray, y: np.ndarray) -> float:
    p = np.clip(p, CLIP, 1 - CLIP)
    return float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))


def reliability(p: np.ndarray, y: np.ndarray, bins: int = BINS):
    edges = np.linspace(0.0, 1.0, bins + 1)
    idx = np.clip(np.digitize(p, edges[1:-1], right=False), 0, bins - 1)
    rows = []
    ece = 0.0
    n = len(p)
    for b in range(bins):
        m = idx == b
        cnt = int(m.sum())
        if cnt == 0:
            rows.append({"bin": b, "lo": float(edges[b]), "hi": float(edges[b + 1]),
                         "n": 0, "mean_p": None, "observed": None, "gap": None})
            continue
        mp = float(p[m].mean())
        obs = float(y[m].mean())
        gap = abs(obs - mp)
        ece += cnt / n * gap
        rows.append({"bin": b, "lo": float(edges[b]), "hi": float(edges[b + 1]),
                     "n": cnt, "mean_p": mp, "observed": obs, "gap": gap})
    return float(ece), rows


# ------------------------------------------------------------------ F3: Kelly
def kelly_fraction(p: float, decimal_odds: float) -> float:
    """f* = (b p - q) / b with b = decimal - 1. Negative means 'do not bet'."""
    b = decimal_odds - 1.0
    if b <= 0:
        raise ValueError("decimal odds must exceed 1.0")
    return (b * p - (1.0 - p)) / b


def log_growth(f: float, p: float, decimal_odds: float) -> float:
    b = decimal_odds - 1.0
    if 1.0 + b * f <= 0 or 1.0 - f <= 0:
        return float("-inf")
    return p * math.log(1.0 + b * f) + (1.0 - p) * math.log(1.0 - f)


def main() -> None:
    df = pd.read_csv(SRC)
    df = df.dropna(subset=["moneyline_away", "moneyline_home", "score_away", "score_home"]).copy()
    # A valid American price is >= 100 or <= -100 (never between).
    ok = ((df.moneyline_away.abs() >= 100) & (df.moneyline_home.abs() >= 100))
    df = df[ok]

    p_home = np.empty(len(df))
    p_away = np.empty(len(df))
    holds = np.empty(len(df))
    for i, (a, h) in enumerate(zip(df.moneyline_away.to_numpy(), df.moneyline_home.to_numpy())):
        p_home[i], holds[i] = no_vig_two_way(float(h), float(a))
        p_away[i] = 1.0 - p_home[i]

    y = (df.score_home.to_numpy() > df.score_away.to_numpy()).astype(float)

    # ---- F1 identities
    p110, hold110 = no_vig_two_way(-110, -110)
    f1 = {
        "rows_used": int(len(df)),
        "games_total": int(pd.read_csv(SRC).shape[0]),
        "identity_minus110": {
            "no_vig": p110,
            "hold": hold110,
            "expected_hold": 1.0 - 2.0 * (100.0 / 210.0),
        },
        "hold_mean": float(holds.mean()),
        "hold_median": float(np.median(holds)),
        "hold_p95": float(np.percentile(holds, 95)),
        "hold_max": float(holds.max()),
        "hold_negative_rows": int((holds < 0).sum()),
        "p_home_mean": float(p_home.mean()),
    }
    f1["checks"] = {
        "no_vig_of_balanced_book_is_half": abs(p110 - 0.5) < 1e-12,
        "hold_of_minus110_both_sides_is_4_76pct": abs(hold110 - 0.04761904761904767) < 1e-9,
        "no_vig_probabilities_sum_to_one_per_row": bool(np.allclose(p_home + p_away, 1.0)),
        "p_home_within_unit_interval": bool(((p_home > 0) & (p_home < 1)).all()),
    }

    # ---- F2 market calibration
    ece, rel = reliability(p_home, y)
    # with-vig comparison: the raw implied probability (no de-vig)
    imp_home = np.array([implied(float(h)) for h in df.moneyline_home.to_numpy()])
    f2 = {
        "n": int(len(y)),
        "home_win_rate": float(y.mean()),
        "brier": brier(p_home, y),
        "log_loss": log_loss(p_home, y),
        "ece": ece,
        "brier_with_vig": brier(imp_home, y),
        "log_loss_with_vig": log_loss(imp_home, y),
        "baseline_brier_0_5": brier(np.full_like(y, 0.5), y),
        "reliability": rel,
    }

    # ---- F3 Kelly
    dec_home = np.array([american_to_decimal(float(h)) for h in df.moneyline_home.to_numpy()])
    f_star = np.array([kelly_fraction(float(p), float(d)) for p, d in zip(p_home, dec_home)])
    synthetic = []
    grid = np.linspace(0.0, 0.999, 200_001)
    for p in (0.40, 0.50, 0.55, 0.60, 0.70):
        for dec in (1.80, 1.91, 2.10, 2.50):
            fs = kelly_fraction(p, dec)
            b = dec - 1.0
            g = np.full_like(grid, -np.inf)
            ok = (1.0 + b * grid > 0) & (1.0 - grid > 0)
            g[ok] = p * np.log(1.0 + b * grid[ok]) + (1.0 - p) * np.log(1.0 - grid[ok])
            best = float(grid[int(np.argmax(g))])
            synthetic.append({
                "p": p, "decimal": dec, "f_star": fs,
                "numeric_argmax": best,
                "argmax_abs_error": abs(best - fs) if fs > 0 else None,
                "growth_at_f_star": log_growth(fs, p, dec) if fs > 0 else None,
                "growth_at_zero": log_growth(0.0, p, dec),
            })
    max_err = max([s["argmax_abs_error"] for s in synthetic if s["argmax_abs_error"] is not None])
    f3 = {
        "n": int(len(f_star)),
        "fraction_positive_at_vigged_price": float((f_star > 0).mean()),
        "max_f_star": float(f_star.max()),
        "mean_f_star": float(f_star.mean()),
        "closed_form_matches_numeric_argmax": bool(max_err <= 1e-4),
        "max_argmax_error": float(max_err),
        "synthetic": synthetic,
    }

    result = {"source": str(SRC), "f1_pricing": f1, "f2_market_calibration": f2, "f3_kelly": f3}
    (OUT / "results.json").write_text(json.dumps(result, indent=2), encoding="utf-8")

    lines = [
        "# Formula bench - measured",
        "",
        f"Source: `{SRC.name}`  |  games used: **{f1['rows_used']:,}** of {f1['games_total']:,}",
        "",
        "## F1 - pricing",
        f"- no-vig of -110/-110 = **{p110:.6f}**, hold = **{hold110 * 100:.4f}%**",
        f"- mean hold across the book = **{f1['hold_mean'] * 100:.3f}%** (median {f1['hold_median'] * 100:.3f}%, p95 {f1['hold_p95'] * 100:.3f}%)",
        f"- rows with negative hold (arbitrage): {f1['hold_negative_rows']}",
        "",
        "## F2 - market calibration (the benchmark)",
        f"- home win rate: {f2['home_win_rate']:.4f}",
        f"- Brier (de-vig): **{f2['brier']:.6f}**  vs 0.25 coin flip  |  with vig: {f2['brier_with_vig']:.6f}",
        f"- log loss (de-vig): **{f2['log_loss']:.6f}**  |  with vig: {f2['log_loss_with_vig']:.6f}",
        f"- ECE (10 bins): **{f2['ece']:.6f}**",
        "",
        "| bin | n | mean p | observed | gap |",
        "|---|---|---|---|---|",
    ]
    for r in rel:
        if r["n"] == 0:
            continue
        lines.append(f"| {r['lo']:.1f}-{r['hi']:.1f} | {r['n']} | {r['mean_p']:.4f} | {r['observed']:.4f} | {r['gap']:.4f} |")
    lines += [
        "",
        "## F3 - Kelly",
        f"- closed form matched the numeric growth-rate maximum in every synthetic case: **{f3['closed_form_matches_numeric_argmax']}** (max error {f3['max_argmax_error']:.1e})",
        f"- at the vigged price, fraction of rows with a positive Kelly stake: **{f3['fraction_positive_at_vigged_price']:.4f}** (max f* {f3['max_f_star']:.4f})",
        "- reading: the de-vigged probability is not an edge against the same book's price; that is the expected result, and it is why PropsLab needs a *model* probability, not a de-vigged market one.",
        "",
    ]
    (OUT / "report.md").write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
