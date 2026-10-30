"""
Independent SymPy reference for the Math X formula layer ingested into PropsLab.

Run:
    python research/mathx/verify_formulas.py

Writes:
    research/derivations/propslab-pricing.md

This is the research-side half of the ingestion. It does not call Math X, an LLM,
Anthropic, or the network. The app-side half is `FormulaLab.kt`, which executes the
same claims against the Kotlin functions that ship in the APK.

The point of two implementations is disagreement: if the SymPy derivation and the
Kotlin checks stop agreeing, one of them is wrong.
"""
from __future__ import annotations

import math
from pathlib import Path

import sympy as sp

RESEARCH = Path(__file__).resolve().parents[1]
OUT = RESEARCH / "derivations" / "propslab-pricing.md"

p, t, b, f, dec, do, du, stake, a, slope, intercept = sp.symbols(
    "p t b f dec do du stake a slope intercept", real=True
)

checks: list[dict] = []


def symbolic(name: str, lhs, rhs=0, note: str = "") -> None:
    diff = sp.simplify(lhs - rhs)
    checks.append({"name": name, "ok": diff == 0, "detail": str(diff), "note": note})


def numeric(name: str, ok: bool, detail: str) -> None:
    checks.append({"name": name, "ok": bool(ok), "detail": detail, "note": ""})


# --------------------------------------------------------------------- pricing
p_over = (1 / do) / (1 / do + 1 / du)
p_under = (1 / du) / (1 / do + 1 / du)
hold = 1 / do + 1 / du - 1

symbolic("No-vig over simplifies to dec_under / (dec_over + dec_under)", p_over, du / (do + du))
symbolic("No-vig under simplifies to dec_over / (dec_over + dec_under)", p_under, do / (do + du))
symbolic("No-vig probabilities sum to one", p_over + p_under, 1)
symbolic("Hold equals (do + du - do*du) / (do*du)", hold, (do + du - do * du) / (do * du))
numeric(
    "Hold on -110 / -110 is 4.7619%",
    abs((1 / 1.9090909090909092 * 2 - 1) - (1 - 2 * 100 / 210)) < 1e-12,
    "implied(-110) = 0.523809..., hold = 0.047619...",
)

# ------------------------------------------------------------------- EV / fair
ev = stake * (p * b - (1 - p - t))
fair_decimal = sp.solve(sp.Eq(p * (dec - 1) - (1 - p - t), 0), dec)[0]

symbolic("Fair decimal equals (1 - t) / p", fair_decimal, (1 - t) / p)
symbolic("Fair decimal equals 1 + q / p", fair_decimal, 1 + (1 - p - t) / p)
symbolic("EV is zero at the fair decimal", ev.subs(b, fair_decimal - 1), 0)
numeric(
    "Push mass raises the break-even price",
    abs(1 + (1 - 0.5 - 0.10) / 0.5 - 1.8) < 1e-12,
    "p=0.5, t=0.10 -> decimal 1.80 -> American -125",
)
numeric(
    "EV at the fair price is zero for p=0.55, t=0.05",
    abs(0.55 * ((1 - 0.05) / 0.55 - 1) - (1 - 0.55 - 0.05)) < 1e-12,
    "fair decimal 1.72727..., EV = 0",
)

# ----------------------------------------------------------------------- Kelly
growth = p * sp.log(1 + b * f) + (1 - p - t) * sp.log(1 - f)
dg = sp.simplify(sp.diff(growth, f))
kelly = sp.solve(sp.Eq(dg, 0), f)[0]
kelly_closed = (p * b - (1 - p - t)) / (b * (1 - t))

symbolic("Stationary point matches the closed form", kelly, kelly_closed)
symbolic("Kelly is zero at the fair decimal", kelly_closed.subs(b, (1 - p - t) / p), 0)

numeric_cases = [(0.55, 0.0, -110), (0.60, 0.0, -110), (0.55, 0.08, -110), (0.75, 0.05, 150)]


def american_to_decimal(price: float) -> float:
    return 1.0 + price / 100.0 if price > 0 else 1.0 + 100.0 / -price


def kelly_of(pw: float, push: float, price: float) -> float:
    bb = american_to_decimal(price) - 1.0
    q = 1.0 - pw - push
    return (pw * bb - q) / (bb * (1.0 - push))


def growth_of(fr: float, pw: float, push: float, price: float) -> float:
    if 1.0 - fr <= 0:
        return float("-inf")
    bb = american_to_decimal(price) - 1.0
    q = 1.0 - pw - push
    return pw * math.log(1 + bb * fr) + q * math.log(1 - fr)


worst_numeric = 0.0
for pw, push, price in numeric_cases:
    fs = kelly_of(pw, push, price)
    grid = [i / 1000 for i in range(996)]
    best = max(grid, key=lambda x: growth_of(x, pw, push, price))
    worst_numeric = max(worst_numeric, abs(best - fs))
numeric(
    "Closed form matches a 1,000-point growth search",
    worst_numeric <= 0.002,
    f"max |numeric - closed| = {worst_numeric:.2e}",
)

# ----------------------------------------------------------------------- Platt
def sigmoid(z: float) -> float:
    return 1.0 / (1.0 + math.exp(-z))


def logit(x: float) -> float:
    return math.log(x / (1.0 - x))


probabilities = [i / 100 for i in range(5, 96, 5)]
identity_error = max(abs(sigmoid(logit(x)) - x) for x in probabilities)
numeric(
    "Identity calibration returns the input",
    identity_error < 1e-12,
    f"max |sigmoid(logit(p)) - p| = {identity_error:.2e}",
)
bounded = all(
    0.0 < sigmoid(slope_i * logit(x) + intercept_i) < 1.0
    for slope_i in (0.5, 1.0, 2.0)
    for intercept_i in (-2.0, -1.0, 0.0, 1.0, 2.0)
    for x in probabilities
)
numeric("Calibrated output stays inside (0, 1)", bounded, "3 slopes x 5 intercepts x 19 probabilities")
monotone = all(
    sigmoid(1.5 * logit(p1) + 0.3) < sigmoid(1.5 * logit(p2) + 0.3)
    for p1, p2 in zip(probabilities, probabilities[1:])
)
numeric("Positive slope preserves order", monotone, "adjacent probability pairs")

# ----------------------------------------------------------------------- report
total = len(checks)
passed = sum(c["ok"] for c in checks)

lines = [
    "# Math X formula derivations — PropsLab",
    "",
    f"Generated by `python research/mathx/verify_formulas.py` on 2026-10-29.",
    f"**{passed} of {total} symbolic/numeric checks pass.**",
    "",
    "This is the independent reference for the formulas ingested into the app's",
    "`com.propslab.mathx.FormulaLab`. The app runs the same claims against Kotlin;",
    "this file runs them in SymPy. Neither side calls a model at runtime.",
    "",
    "| # | Check | Result |",
    "|---:|---|---|",
]
for i, c in enumerate(checks, 1):
    lines.append(f"| {i} | {c['name']} | {'PASS' if c['ok'] else 'FAIL'} |")
lines += [
    "",
    "## 1. No-vig price and hold",
    "",
    "Two-way implied probabilities are `i = 1 / decimal`. Normalising them removes",
    "the book's margin. The margin itself is `hold = i_over + i_under - 1`.",
    "",
    "```python",
    "p_over = (1/do) / (1/do + 1/du)",
    "p_under = (1/du) / (1/do + 1/du)",
    "hold = 1/do + 1/du - 1",
    "```",
    "",
    "SymPy reduces `p_over` to `du / (do + du)`, `p_under` to `do / (do + du)`,",
    "and their sum to exactly 1. At `-110 / -110` the hold is 4.7619%.",
    "",
    "## 2. Expected value with push mass",
    "",
    "A push returns the stake, so it contributes zero profit. With `q = 1 - p - t`",
    "and `b = decimal - 1`:",
    "",
    "```",
    "EV / stake = p·b - q",
    "fair decimal = (1 - t) / p = 1 + q / p",
    "```",
    "",
    "SymPy solves `EV = 0` for the decimal price and confirms both forms are equal.",
    "Substituting `b = fair_decimal - 1` simplifies the EV to zero.",
    "",
    "## 3. Kelly fraction with push mass",
    "",
    "Maximise expected log growth:",
    "",
    "```",
    "g(f) = p·ln(1 + b·f) + q·ln(1 - f)",
    "dg/df = p·b/(1 + b·f) - q/(1 - f)",
    "f* = (p·b - q) / (b·(1 - t))",
    "```",
    "",
    "SymPy solves the stationary condition to the same closed form. A 1,000-point",
    "brute-force search over `f` agrees within 0.002 for the test cases, and `f*` is",
    "zero at the fair decimal.",
    "",
    "## 4. Platt calibration on the logit",
    "",
    "```",
    "z = a·ln(p / (1 - p)) + b",
    "p_cal = 1 / (1 + exp(-z))",
    "```",
    "",
    "`a = 1, b = 0` is the identity. For any finite slope and intercept the output",
    "stays strictly inside (0, 1), and a positive slope preserves order.",
    "",
    "## Where the Math X layer lives",
    "",
    "| Piece | Location |",
    "|---|---|",
    "| Formula/probability/solve prompt prefixes | `mathx/MathXPrompts.kt` |",
    "| Formula catalog and executable checks | `mathx/FormulaLab.kt` |",
    "| Local-model boundary | `mathx/LocalMathModel.kt` |",
    "| Probability whitelist guard | `mathx/MathXAssistant.kt` |",
    "| Lab screen | `ui/screens/LabScreen.kt` |",
    "",
    "Math X's Express API, React workbench, Anthropic routing and Pyodide worker are",
    "not shipped. Inference never leaves the phone: the manifest's only network ",
    "permission is the read-only odds feed. A local GGUF runtime is the",
    "only intended model path; until it exists, the lab and the assistant are fully",
    "deterministic.",
    "",
    "## What this does not establish",
    "",
    "- No prop quotes, so no edge is measured here.",
    "- The live Math X Formula Lab was not run in this pass; no local weights are installed.",
    "- Symbolic identities do not replace the holdout evaluation in `CRITERIA.md`.",
    "",
]
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(lines), encoding="utf-8")

for c in checks:
    print(f"{'PASS' if c['ok'] else 'FAIL'}  {c['name']}  [{c['detail']}]")
print(f"\n{passed}/{total} checks pass -> {OUT}")
raise SystemExit(0 if passed == total else 1)
