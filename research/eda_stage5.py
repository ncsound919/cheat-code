"""
EDA stage 5 - is the lookup table better with a second key?

Stage 4 found the mu-keyed empirical distribution is well calibrated (ECE 0.006)
and beats both Poisson (0.019) and the player's own last 10 (0.072). But one key
cannot tell a 12-ppg starter from a 12-ppg bench player, and those have different
variance. This tests two second keys:

  emp_mu          one key: predicted mean
  emp_mu_sd       + trailing standard deviation of points (the player's own noise)
  emp_mu_min      + trailing minutes (role / playing-time risk)

Fit before 2023-24, evaluate on 2023-24..2025-26 at L = floor(mu)+0.5.
"""
import json
import sqlite3
from pathlib import Path

import numpy as np
import pandas as pd

DB = r"file:D:/datasets/sports-datasets/kaggle/chevronronson__nba-stats-dataset/sql/nba_stats.sqlite?mode=ro"
OUT = Path(r"C:\Users\tap45\Downloads\PropsLab\research")

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
df["t_sd"] = g["pts"].transform(lambda s: s.shift().rolling(10, min_periods=5).std())
df["t_min"] = g["min"].transform(lambda s: s.shift().rolling(10, min_periods=5).mean())
df["std_pts"] = (df.groupby(["player_id", "season_year"], sort=False)["pts"]
                   .transform(lambda s: s.shift().expanding(min_periods=5).mean()))

m = df.dropna(subset=["t_pts", "std_pts", "t_sd", "t_min"]).copy()
m["mu"] = 0.5 * m["t_pts"] + 0.5 * m["std_pts"]
m["line"] = np.floor(m["mu"]) + 0.5
m["is_holdout"] = m["season_year"].isin(["2023-24", "2024-25", "2025-26"])
fit, hold = m[~m["is_holdout"]], m[m["is_holdout"]]
print(f"fit {len(fit):,}  holdout {len(hold):,}")

KMAX = 60
SD_EDGES = [0, 3, 5, 7, 10, 99]
MIN_EDGES = [0, 18, 26, 32, 99]
MU_B = 0.5


def bucket(frame):
    mu_b = np.floor(frame["mu"] / MU_B).astype(int).clip(0, int(50 / MU_B))
    sd_b = np.searchsorted(SD_EDGES, frame["t_sd"], side="right") - 1
    min_b = np.searchsorted(MIN_EDGES, frame["t_min"], side="right") - 1
    return mu_b, np.clip(sd_b, 0, len(SD_EDGES) - 2), np.clip(min_b, 0, len(MIN_EDGES) - 2)


mu_b, sd_b, min_b = bucket(fit)


def build(key_arrays, frame):
    table = {}
    global_hist = (np.bincount(np.clip(frame["pts"].astype(int), 0, KMAX), minlength=KMAX + 1) + 0.5)
    global_hist = global_hist / global_hist.sum()
    acc = {}
    for key, p in zip(zip(*key_arrays), np.clip(frame["pts"].astype(int), 0, KMAX)):
        h = acc.get(key)
        if h is None:
            h = np.zeros(KMAX + 1)
            acc[key] = h
        h[p] += 1
    for key, h in acc.items():
        table[key] = (h + 0.5) / (h.sum() + 0.5 * (KMAX + 1))
    return table, global_hist


t1, g1 = build([mu_b], fit)
t2, _ = build([mu_b, sd_b], fit)
t3, _ = build([mu_b, min_b], fit)
print(f"cells: mu={len(t1)}  mu+sd={len(t2)}  mu+min={len(t3)}")

hold = m[m["is_holdout"]].reset_index(drop=True)
hmu, hsd, hmin = bucket(hold)
rng = np.random.default_rng(13)
pos = rng.choice(len(hold), size=min(25_000, len(hold)), replace=False)

rows = []
for i in pos:
    mu = max(0.2, float(hold["mu"].iloc[i]))
    L = float(hold["line"].iloc[i])
    y = float(hold["pts"].iloc[i] > L)
    k = int(np.floor(L))
    e1 = t1.get((hmu[i],), g1)[k + 1:].sum()
    e2 = t2.get((hmu[i], hsd[i]), g1)[k + 1:].sum()
    e3 = t3.get((hmu[i], hmin[i]), g1)[k + 1:].sum()
    rows.append({"y": y, "emp_mu": e1, "emp_mu_sd": e2, "emp_mu_min": e3})

ev = pd.DataFrame(rows)


def score(col):
    p = np.clip(ev[col].to_numpy(), 1e-6, 1 - 1e-6)
    y = ev["y"].to_numpy()
    brier = float(np.mean((p - y) ** 2))
    ll = float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))
    bins = pd.cut(ev[col], bins=np.linspace(0, 1, 11), include_lowest=True)
    ece = 0.0
    for _, grp in ev.groupby(bins, observed=True):
        if len(grp) < 50:
            continue
        ece += len(grp) / len(ev) * abs(grp["y"].mean() - grp[col].mean())
    return {"brier": brier, "logloss": ll, "ece": float(ece),
            "mean_p": float(ev[col].mean()), "observed": float(y.mean())}


res = {c: score(c) for c in ("emp_mu", "emp_mu_sd", "emp_mu_min")}
print("\n== second-key bake-off ==")
print(pd.DataFrame(res).T.to_string())

(OUT / "eda_stage5.json").write_text(json.dumps(res, indent=2), encoding="utf-8")
