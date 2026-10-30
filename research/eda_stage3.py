"""
EDA stage 3 - pick the predictor and the distribution, then test both.

Stage 2 said three things worth acting on:
  * overdispersion is not constant (var/mean 4.5 at 4 pts, 2.8 at 26 pts)
  * year-over-year scoring rate correlates 0.86, so a player prior is real
  * the naive trailing-10 mean is already within ~4.5 points MAE

This stage answers the questions that decide the formula:
  1. how much of points variance is minutes? (fix the NaN in stage 2)
  2. does recency (last 10) beat season-to-date, or is it noise?
  3. does rate x minutes beat predicting points directly?
  4. is a negative binomial with mean-dependent dispersion calibrated?
  5. what is the integer-line push rate when rounding is done correctly?

Holdout: 2023-24 .. 2025-26. Everything before is fit/reference.
"""
import json
import sqlite3
from pathlib import Path

import numpy as np
import pandas as pd

DB = r"file:D:/datasets/sports-datasets/kaggle/chevronronson__nba-stats-dataset/sql/nba_stats.sqlite?mode=ro"
OUT = Path(r"C:\Users\tap45\Downloads\PropsLab\research")
OUT.mkdir(parents=True, exist_ok=True)

con = sqlite3.connect(DB, uri=True)
q = """
select season_year, player_id, game_id, game_date, is_home, min, pts, available_flag
from player_boxscores where substr(game_id,1,3)='002'
"""
df = pd.read_sql_query(q, con)
df["date"] = pd.to_datetime(df["game_date"], errors="coerce")
for c in ("min", "pts"):
    df[c] = pd.to_numeric(df[c], errors="coerce")
df = df.dropna(subset=["date", "min", "pts"])
df = df[df["available_flag"].fillna(1).astype(float) > 0]
df = df[df["min"] > 0].sort_values(["player_id", "date"]).reset_index(drop=True)

g = df.groupby("player_id", sort=False)
df["t_pts"] = g["pts"].transform(lambda s: s.shift().rolling(10, min_periods=5).mean())
df["t_min"] = g["min"].transform(lambda s: s.shift().rolling(10, min_periods=5).mean())
# season-to-date mean over prior games in the same season only
df["std_pts"] = (
    df.groupby(["player_id", "season_year"], sort=False)["pts"]
      .transform(lambda s: s.shift().expanding(min_periods=5).mean())
)
df["n_prior_season"] = df.groupby(["player_id", "season_year"], sort=False).cumcount()

m = df.dropna(subset=["t_pts", "t_min", "std_pts"]).copy()
m["is_holdout"] = m["season_year"].isin(["2023-24", "2024-25", "2025-26"])
print(f"rows: {len(m):,}  holdout: {int(m['is_holdout'].sum()):,}")

rep = {}

# ------------------------------------------------ 1. minutes share of variance
rs, ws = [], []
for _, grp in m.groupby(["player_id", "season_year"], sort=False):
    if len(grp) < 20:
        continue
    x = grp["min"].to_numpy(float)
    y = grp["pts"].to_numpy(float)
    if x.std() == 0 or y.std() == 0:
        continue
    r2 = np.corrcoef(x, y)[0, 1] ** 2
    if np.isfinite(r2):
        rs.append(r2)
        ws.append(len(grp))
rep["minutes_r2"] = {
    "player_seasons": len(rs),
    "median": float(np.median(rs)),
    "p25": float(np.percentile(rs, 25)),
    "p75": float(np.percentile(rs, 75)),
    "weighted_mean": float(np.average(rs, weights=ws)),
}

# ------------------------------------- 2. predictor bake-off (holdout RMSE/MAE)
h = m[m["is_holdout"]]
preds = {
    "trailing_10_pts": h["t_pts"],
    "season_to_date_pts": h["std_pts"],
    "blend_50_50": 0.5 * h["t_pts"] + 0.5 * h["std_pts"],
    "blend_30_70": 0.3 * h["t_pts"] + 0.7 * h["std_pts"],
}
# rate x minutes, with the rate shrunk toward the player's season rate
h = h.assign(t_rate=h["t_pts"] / h["t_min"].clip(lower=1),
             s_rate=h["std_pts"] / h.groupby(["player_id", "season_year"])["min"]
                                        .transform(lambda s: s.shift().expanding(min_periods=5).mean()).clip(lower=1))
h = h.assign(shrink_rate=0.5 * h["t_rate"] + 0.5 * h["s_rate"])
preds["rate_x_min_trailing"] = h["t_rate"] * h["t_min"]
preds["rate_x_min_shrunk"] = h["shrink_rate"] * h["t_min"]

bake = {}
y = h["pts"].to_numpy(float)
for name, p in preds.items():
    p = np.asarray(p, dtype=float)
    ok = np.isfinite(p)
    err = y[ok] - p[ok]
    bake[name] = {"n": int(ok.sum()), "rmse": float(np.sqrt((err ** 2).mean())),
                  "mae": float(np.abs(err).mean()), "bias": float(err.mean())}
rep["predictor_bakeoff"] = bake

# ------------------------------- 3. does recency add anything over season rate?
sub = h.dropna(subset=["t_pts", "std_pts"])
X = np.column_stack([np.ones(len(sub)), sub["std_pts"], sub["t_pts"]])
beta, *_ = np.linalg.lstsq(X, sub["pts"].to_numpy(float), rcond=None)
resid = sub["pts"].to_numpy(float) - X @ beta
r2_full = 1 - resid.var() / sub["pts"].var()
r2_std = 1 - (sub["pts"] - sub["std_pts"]).var() / sub["pts"].var()
rep["recency_regression"] = {
    "intercept": float(beta[0]),
    "coef_season_to_date": float(beta[1]),
    "coef_trailing_10": float(beta[2]),
    "r2_both": float(r2_full),
    "r2_season_to_date_only": float(r2_std),
}

# --------------------------- 4. dispersion as a function of the mean (NB fit)
m["bin"] = pd.cut(m["t_pts"], bins=[0, 5, 10, 15, 20, 25, 30, 40, 100], right=False)
disp = []
for label, grp in m.groupby("bin", observed=True):
    if len(grp) < 500:
        continue
    mu = float(grp["pts"].mean())
    var = float(grp["pts"].var(ddof=1))
    r = mu * mu / (var - mu) if var > mu else np.nan
    disp.append({"bin": str(label), "n": int(len(grp)), "mean": mu, "var": var,
                 "var_over_mean": var / mu, "nb_r": float(r),
                 "mean_min": float(grp["min"].mean()), "sd_min": float(grp["min"].std())})
rep["dispersion"] = disp

# fit r as a power law of mu:  r = a * mu^b
dd = pd.DataFrame([d for d in disp if np.isfinite(d["nb_r"])])
b_, loga = np.polyfit(np.log(dd["mean"]), np.log(dd["nb_r"]), 1)
a_ = float(np.exp(loga))
rep["nb_r_fit"] = {"a": a_, "b": float(b_),
                   "form": "r = a * mu^b",
                   "points": [{"mu": float(x), "r": float(a_ * x ** b_)} for x in (4, 8, 12, 16, 20, 25, 30)]}


# -------------------------------------------------------------- NB machinery
def nb_pmf(k: np.ndarray, mu: float, r: float) -> np.ndarray:
    """Negative binomial with mean mu and dispersion r (var = mu + mu^2/r)."""
    from math import lgamma, log, log1p

    p = r / (r + mu)
    return np.array([
        np.exp(lgamma(kk + r) - lgamma(kk + 1) - lgamma(r) + r * log(p) + kk * log1p(-p))
        for kk in k
    ])


def over_under(pts: int, mu: float, r: float, kmax: int = 80):
    k = np.arange(0, kmax + 1)
    pmf = nb_pmf(k.astype(float), mu, r)
    push = pmf[pts] if 0 <= pts <= kmax else 0.0
    over = pmf[pts + 1:].sum()
    under = pmf[:pts].sum()
    total = push + over + under
    return push / total, over / total, under / total


# ---------------------- 5. calibration on the holdout, at the nearest half-line
# Line = ceil(trailing mean) - 0.5, i.e. the half-point just below the mean, a
# realistic prop line shape. No push mass at a half line.
samp = h.dropna(subset=["t_pts"]).copy()
samp = samp.sample(n=min(20_000, len(samp)), random_state=7)
rows = []
for _, r_ in samp.iterrows():
    mu = max(1e-3, float(r_["t_pts"]))
    disp_r = max(0.5, a_ * mu ** b_)
    lines = [np.floor(mu) + 0.5, np.ceil(mu) - 0.5]
    L = float(np.median(lines))
    push, over, under = over_under(int(np.floor(L)) if L % 1 == 0.5 else int(L), mu, disp_r)
    rows.append({"mu": mu, "line": L, "p_over": over, "p_under": under, "p_push": push,
                 "y_over": float(r_["pts"] > L)})
cal = pd.DataFrame(rows)
cal["brier"] = (cal["p_over"] - cal["y_over"]) ** 2
# bins on predicted probability
cal["pb"] = pd.cut(cal["p_over"], bins=np.linspace(0, 1, 11), include_lowest=True)
ece = 0.0
rel = []
for label, grp in cal.groupby("pb", observed=True):
    if len(grp) < 50:
        continue
    gap = abs(grp["y_over"].mean() - grp["p_over"].mean())
    ece += len(grp) / len(cal) * gap
    rel.append({"bin": str(label), "n": int(len(grp)),
                "pred": float(grp["p_over"].mean()), "obs": float(grp["y_over"].mean()),
                "gap": float(gap)})
rep["holdout_calibration_half_line"] = {
    "n": int(len(cal)), "brier": float(cal["brier"].mean()),
    "brier_baseline_mu_only_0_5": None,
    "ece": float(ece), "reliability": rel,
    "mean_p_over": float(cal["p_over"].mean()), "observed_over": float(cal["y_over"].mean()),
}

# Poisson comparison: same mean, var = mu (no dispersion)
pois = []
for _, r_ in cal.iterrows():
    mu = r_["mu"]
    k = np.arange(0, 80)
    from math import exp, lgamma
    pmf = np.array([exp(-mu + kk * np.log(mu) - lgamma(kk + 1)) if mu > 0 else 0 for kk in k])
    idx = int(np.floor(r_["line"]))
    over = pmf[idx + 1:].sum()
    pois.append((over - r_["y_over"]) ** 2)
rep["holdout_calibration_half_line"]["brier_poisson"] = float(np.mean(pois))

# --------------------------- 6. push rate at integer lines, correct rounding
m["line_int"] = np.floor(m["t_pts"] + 0.5).astype(int)
push_rows = []
for L, grp in m.groupby("line_int"):
    if L < 1 or len(grp) < 500:
        continue
    push_rows.append({"line": int(L), "n": int(len(grp)),
                      "push": float((grp["pts"] == L).mean()),
                      "over": float((grp["pts"] > L).mean()),
                      "under": float((grp["pts"] < L).mean())})
tot = sum(r["n"] for r in push_rows)
rep["push_at_integer_line"] = push_rows
rep["push_rate_weighted"] = float(sum(r["push"] * r["n"] for r in push_rows) / tot)

(OUT / "eda_stage3.json").write_text(json.dumps(rep, indent=2), encoding="utf-8")

print("\n== minutes share of points variance (player-seasons) ==")
print(rep["minutes_r2"])
print("\n== predictor bake-off on holdout ==")
print(pd.DataFrame(bake).T.to_string())
print("\n== recency regression ==")
print(rep["recency_regression"])
print("\n== dispersion / NB r ==")
print(pd.DataFrame(disp).to_string(index=False))
print("NB r fit:", rep["nb_r_fit"])
print("\n== holdout calibration at a half line ==")
print({k: v for k, v in rep["holdout_calibration_half_line"].items() if k != "reliability"})
print(pd.DataFrame(rel).to_string(index=False))
print("\n== push at integer line (correct rounding) ==")
print(pd.DataFrame(push_rows).head(14).to_string(index=False))
print("weighted push rate:", round(rep["push_rate_weighted"], 4))
