"""
EDA stage 4 - the honest bake-off between three ways to price a line.

Stage 3 showed the naive negative binomial was miscalibrated (ECE 0.045) and no
better than a coin flip on Brier, because the line placement was ad hoc. This
stage fixes the evaluation and compares three concrete candidates:

  A. NB(mean=mu, var=mu+mu^2/r), r = a*mu^b                      (parametric)
  B. Poisson(mean=mu)                                            (parametric)
  C. Empirical lookup table: histogram of points per mu bucket   (non-parametric)
  D. The player's own last-10 outcomes, Laplace smoothed         (non-parametric)

Line convention: L = floor(mu) + 0.5, i.e. the half point just below the
predicted mean. That is the realistic prop shape and it removes push mass, so
the comparison is about the distribution's shape, not about rounding luck.

Fit on everything before 2023-24; evaluate on 2023-24..2025-26.
"""
import json
import sqlite3
from math import lgamma, log, log1p
from pathlib import Path

import numpy as np
import pandas as pd

DB = r"file:D:/datasets/sports-datasets/kaggle/chevronronson__nba-stats-dataset/sql/nba_stats.sqlite?mode=ro"
OUT = Path(r"C:\Users\tap45\Downloads\PropsLab\research")
OUT.mkdir(parents=True, exist_ok=True)

con = sqlite3.connect(DB, uri=True)
df = pd.read_sql_query(
    """select season_year, player_id, game_id, game_date, min, pts, available_flag
       from player_boxscores where substr(game_id,1,3)='002'""", con)
df["date"] = pd.to_datetime(df["game_date"], errors="coerce")
for c in ("min", "pts"):
    df[c] = pd.to_numeric(df[c], errors="coerce")
df = df.dropna(subset=["date", "min", "pts"])
df = df[df["available_flag"].fillna(1).astype(float) > 0]
df = df[df["min"] > 0].sort_values(["player_id", "date"]).reset_index(drop=True)

g = df.groupby("player_id", sort=False)
df["t_pts"] = g["pts"].transform(lambda s: s.shift().rolling(10, min_periods=5).mean())
df["std_pts"] = (df.groupby(["player_id", "season_year"], sort=False)["pts"]
                   .transform(lambda s: s.shift().expanding(min_periods=5).mean()))

m = df.dropna(subset=["t_pts", "std_pts"]).copy()
m["mu"] = 0.5 * m["t_pts"] + 0.5 * m["std_pts"]
m["is_holdout"] = m["season_year"].isin(["2023-24", "2024-25", "2025-26"])

fit = m[~m["is_holdout"]]
hold = m[m["is_holdout"]].copy()
hold["line"] = np.floor(hold["mu"]) + 0.5
print(f"fit {len(fit):,}  holdout {len(hold):,}")

# ------------------------------------------------------- C: empirical lookup
KMAX = 60
BUCKET = 0.5
fit = fit.assign(bucket=np.floor(fit["mu"] / BUCKET).astype(int).clip(0, int(50 / BUCKET)))
table = {}
for b, grp in fit.groupby("bucket"):
    hist = np.bincount(np.clip(grp["pts"].astype(int), 0, KMAX), minlength=KMAX + 1).astype(float)
    # Laplace smoothing so a bucket never claims a zero-probability tail
    hist = (hist + 0.5) / (hist.sum() + 0.5 * (KMAX + 1))
    table[int(b)] = hist
print(f"lookup buckets fitted: {len(table)}")

# ------------------------------------------------------------- NB machinery
dd = pd.DataFrame(json.loads((OUT / "eda_stage3.json").read_text())["dispersion"])
finite = dd[np.isfinite(dd["nb_r"])]
b_, loga = np.polyfit(np.log(finite["mean"]), np.log(finite["nb_r"]), 1)
A = float(np.exp(loga))
B = float(b_)
print(f"NB dispersion r = {A:.4f} * mu^{B:.4f}")


def nb_cdf(kmax: int, mu: float, r: float) -> float:
    p = r / (r + mu)
    logp = r * log(p)
    tot = 0.0
    for k in range(kmax + 1):
        tot += np.exp(lgamma(k + r) - lgamma(k + 1) - lgamma(r) + logp + k * log1p(-p))
    return float(tot)


def nb_over(line: float, mu: float, r: float) -> float:
    k = int(np.floor(line))
    return float(max(0.0, min(1.0, 1.0 - nb_cdf(k, mu, r))))


def pois_over(line: float, mu: float) -> float:
    k = int(np.floor(line))
    pmf = np.array([np.exp(-mu + kk * log(mu) - lgamma(kk + 1)) if mu > 0 else 0.0
                    for kk in range(k + 1)])
    return float(max(0.0, min(1.0, 1.0 - pmf.sum())))


# ------------------------------------------------------------- evaluate
samp = hold.sample(n=min(25_000, len(hold)), random_state=11)

# per-player ordered history, so "own last 10" is exact and cheap
hist_by_player = {}
for pid, grp in df.groupby("player_id", sort=False):
    gids = grp["game_id"].tolist()
    hist_by_player[pid] = (gids, grp["pts"].tolist(), {gid: i for i, gid in enumerate(gids)})

rows = []
for _, r in samp.iterrows():
    mu = max(0.2, float(r["mu"]))
    L = float(r["line"])
    y = float(r["pts"] > L)

    nb = nb_over(L, mu, max(0.5, A * mu ** B))
    po = pois_over(L, mu)

    bk = int(np.floor(mu / BUCKET))
    hist = table.get(bk, table.get(max(table), np.ones(KMAX + 1) / (KMAX + 1)))
    emp = float(hist[int(L) + 1:].sum())

    gids, pts_seq, pos = hist_by_player[r["player_id"]]
    i = pos[r["game_id"]]
    prev = pts_seq[max(0, i - 10):i]
    own = float(((np.asarray(prev, dtype=float) > L).sum() + 0.5) / (len(prev) + 1.0))

    rows.append({"mu": mu, "line": L, "y": y, "nb": nb, "pois": po, "emp": emp, "own": own})

ev = pd.DataFrame(rows)


def score(pcol):
    p = np.clip(ev[pcol].to_numpy(), 1e-6, 1 - 1e-6)
    y = ev["y"].to_numpy()
    brier = float(np.mean((p - y) ** 2))
    ll = float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))
    bins = pd.cut(ev[pcol], bins=np.linspace(0, 1, 11), include_lowest=True)
    ece = 0.0
    for _, grp in ev.groupby(bins, observed=True):
        if len(grp) < 50:
            continue
        ece += len(grp) / len(ev) * abs(grp["y"].mean() - grp[pcol].mean())
    return {"brier": brier, "logloss": ll, "ece": float(ece),
            "mean_p": float(ev[pcol].mean()), "observed": float(y.mean())}


res = {c: score(c) for c in ("nb", "pois", "emp", "own")}
res["coin_flip_brier"] = 0.25
print("\n== half-line bake-off (L = floor(mu)+0.5) ==")
print(pd.DataFrame(res).T.to_string())

# --------------------------------------- integer line: does push get priced?
hold_i = samp.copy()
hold_i["line_int"] = np.floor(hold_i["mu"] + 0.5)
push_rows = []
for _, r in hold_i.iterrows():
    mu = max(0.2, float(r["mu"]))
    L = float(r["line_int"])
    r_ = max(0.5, A * mu ** B)
    p = r_ / (r_ + mu)
    pmf = np.exp([lgamma(k + r_) - lgamma(k + 1) - lgamma(r_) + r_ * log(p) + k * log1p(-p)
                  for k in range(0, KMAX + 1)])
    pmf = pmf / pmf.sum()
    li = int(L)
    pred_push = float(pmf[li])
    bk = int(np.floor(mu / BUCKET))
    hist = table.get(bk)
    emp_push = float(hist[li]) if hist is not None else float("nan")
    push_rows.append({"mu": mu, "line": li, "pred_push": pred_push,
                      "emp_push": emp_push,
                      "hit_push": float(r["pts"] == li)})
pj = pd.DataFrame(push_rows)
push_eval = {
    "n": int(len(pj)),
    "observed_push": float(pj["hit_push"].mean()),
    "mean_pred_push_nb": float(pj["pred_push"].mean()),
    "mean_pred_push_lookup": float(pj["emp_push"].mean()),
    "push_brier_nb": float(((pj["pred_push"] - pj["hit_push"]) ** 2).mean()),
    "push_brier_lookup": float(((pj["emp_push"] - pj["hit_push"]) ** 2).mean()),
}
print("\n== integer-line push ==")
print(push_eval)

# per-bucket detail for the lookup table
detail = []
for b in sorted(table):
    lo, hi = b * BUCKET, (b + 1) * BUCKET
    if hi < 1 or lo > 40:
        continue
    h = table[b]
    detail.append({"mu_lo": lo, "mu_hi": hi,
                   "p_over_mean": float(h[int(lo) + 1:].sum()),
                   "support_gt_0": float(h[1:].sum())})

out = {"nb_dispersion": {"a": A, "b": B},
       "half_line": res,
       "push": push_eval,
       "lookup_buckets": len(table),
       "buckets": detail}
(OUT / "eda_stage4.json").write_text(json.dumps(out, indent=2), encoding="utf-8")
