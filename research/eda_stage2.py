"""
EDA stage 2 - what actually drives a player's points, and how spread out it is.

Reads the CC0 box-score DB (787,531 player-games, 1996-2026), keeps regular
season, builds leakage-free trailing features, and reports the numbers that
decide which formula is worth building.
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
select season_year, player_id, player_name, team_id, game_id, game_date, is_home,
       min, pts, fgm, fga, fg3m, fg3a, ftm, fta, usg_pct, pace, available_flag
from player_boxscores
where substr(game_id,1,3) = '002'
"""
df = pd.read_sql_query(q, con)
df["date"] = pd.to_datetime(df["game_date"], errors="coerce")
df["min"] = pd.to_numeric(df["min"], errors="coerce")
df["pts"] = pd.to_numeric(df["pts"], errors="coerce")
df = df.dropna(subset=["date", "min", "pts"])

n_raw = len(df)
# available_flag: 1 = played. Rows with 0 are DNP/inactive and are not labels.
if "available_flag" in df.columns:
    df = df[df["available_flag"].fillna(1).astype(float) > 0]
df = df[df["min"] > 0]
print(f"regular-season rows: {n_raw:,} -> played rows: {len(df):,}")

df = df.sort_values(["player_id", "date"]).reset_index(drop=True)
g = df.groupby("player_id", sort=False)

# Leakage-free trailing windows: shift(1) means tonight's game is never in its own features.
df["t_pts"] = g["pts"].transform(lambda s: s.shift().rolling(10, min_periods=5).mean())
df["t_min"] = g["min"].transform(lambda s: s.shift().rolling(10, min_periods=5).mean())
df["t_min_sd"] = g["min"].transform(lambda s: s.shift().rolling(10, min_periods=5).std())
df["t_pts_sd"] = g["pts"].transform(lambda s: s.shift().rolling(10, min_periods=5).std())
df["gap_days"] = g["date"].diff().dt.days
df["n_prior"] = g.cumcount()

m = df.dropna(subset=["t_pts", "t_min"]).copy()
print(f"rows with a full trailing window: {len(m):,}")

report = {}

# ---------------------------------------------------------------- 1. shape
pts = m["pts"].to_numpy()
report["distribution"] = {
    "n": int(len(pts)),
    "mean": float(pts.mean()),
    "var": float(pts.var(ddof=1)),
    "var_over_mean": float(pts.var(ddof=1) / pts.mean()),
    "share_zero": float((pts == 0).mean()),
    "q": {str(qq): float(np.percentile(pts, qq)) for qq in (1, 5, 25, 50, 75, 95, 99)},
    "skew": float(pd.Series(pts).skew()),
}

# ------------------------------------------- 2. minutes vs rate decomposition
# Within a player-season, how much of game-to-game points variance is minutes?
rs = []
for (pid, season), grp in m.groupby(["player_id", "season_year"], sort=False):
    if len(grp) < 20:
        continue
    x = grp["min"].to_numpy(float)
    y = grp["pts"].to_numpy(float)
    if x.std() == 0:
        continue
    r = np.corrcoef(x, y)[0, 1]
    rs.append((r * r, len(grp)))
rs = np.array(rs)
report["minutes_explain"] = {
    "player_seasons": int(len(rs)),
    "median_r2_pts_on_min": float(np.median(rs[:, 0])),
    "p25_r2": float(np.percentile(rs[:, 0], 25)),
    "p75_r2": float(np.percentile(rs[:, 0], 75)),
    "weighted_mean_r2": float(np.average(rs[:, 0], weights=rs[:, 1])),
}

# --------------------------------------------- 3. dispersion by trailing mean
m["bin"] = pd.cut(m["t_pts"], bins=[0, 5, 10, 15, 20, 25, 30, 40, 100], right=False)
rows = []
for label, grp in m.groupby("bin", observed=True):
    mu = grp["pts"].mean()
    var = grp["pts"].var(ddof=1)
    # negative binomial method-of-moments dispersion: var = mu + mu^2 / r
    r = mu * mu / (var - mu) if var > mu else None
    rows.append({
        "bin": str(label),
        "n": int(len(grp)),
        "mean_pts": float(mu),
        "var": float(var),
        "var_over_mean": float(var / mu),
        "nb_r": float(r) if r else None,
        "mean_min": float(grp["min"].mean()),
        "sd_min": float(grp["min"].std()),
        "sd_t_min": float(grp["t_min_sd"].mean()),
    })
report["dispersion_by_mean"] = rows

# ----------------------------------------- 4. integer-line push probability
# If the line is the integer nearest the trailing mean, how often does the
# player land exactly on it?
m["line_int"] = m["t_pts"].round().astype(int)
push_rows = []
for L, grp in m.groupby("line_int"):
    if len(grp) < 200:
        continue
    push_rows.append({
        "line": int(L),
        "n": int(len(grp)),
        "push": float((grp["pts"] == L).mean()),
        "over": float((grp["pts"] > L).mean()),
        "under": float((grp["pts"] < L).mean()),
    })
report["push_at_integer_line"] = push_rows[:40]

# weighted average push rate across lines
tot = sum(r["n"] for r in push_rows)
report["push_rate_mean"] = float(sum(r["push"] * r["n"] for r in push_rows) / tot)

# ------------------------------------------------ 5. home / rest effect size
home = m.groupby("is_home")["pts"].agg(["mean", "count"])
rest = m.copy()
rest["rest_bucket"] = pd.cut(rest["gap_days"], bins=[0, 1, 2, 3, 7, 400],
                             labels=["b2b", "1 day", "2 days", "3-7", "8+"])
report["home_away"] = {str(k): {"mean": float(v["mean"]), "n": int(v["count"])}
                       for k, v in home.iterrows()}
report["rest_effect"] = {
    str(k): {"mean": float(v["mean"]), "n": int(v["count"])}
    for k, v in rest.groupby("rest_bucket", observed=True)["pts"].agg(["mean", "count"]).iterrows()
}

# ------------------------------------------ 6. is the rate stable year to year?
ps = (m.groupby(["player_id", "season_year"])
        .agg(minutes=("min", "sum"), pts=("pts", "sum"), games=("pts", "size"))
        .reset_index())
ps = ps[(ps["minutes"] >= 500) & (ps["games"] >= 20)]
ps["rate"] = ps["pts"] / ps["minutes"]
ps["season_num"] = ps["season_year"].str[:4].astype(int)
ps = ps.sort_values(["player_id", "season_num"])
ps["next_rate"] = ps.groupby("player_id")["rate"].shift(-1)
pair = ps.dropna(subset=["next_rate"])
report["year_over_year_rate"] = {
    "pairs": int(len(pair)),
    "pearson_r": float(np.corrcoef(pair["rate"], pair["next_rate"])[0, 1]),
    "sd_rate": float(ps["rate"].std()),
}

# --------------------------------------------------- 7. trailing-window error
# How wrong is the obvious predictor one game ahead?
m["err"] = m["pts"] - m["t_pts"]
report["trailing_10_predictor"] = {
    "mae": float(m["err"].abs().mean()),
    "rmse": float(np.sqrt((m["err"] ** 2).mean())),
    "bias": float(m["err"].mean()),
}

report["windows"] = {
    "seasons": [str(m["season_year"].min()), str(m["season_year"].max())],
    "rows_modelled": int(len(m)),
}

(OUT / "eda_stage2.json").write_text(json.dumps(report, indent=2), encoding="utf-8")

# ---------------------------------------------------------------- print
print("\n== distribution ==")
d = report["distribution"]
print(f"n={d['n']:,} mean={d['mean']:.2f} var={d['var']:.1f} var/mean={d['var_over_mean']:.2f} "
      f"zero={d['share_zero']:.3f} skew={d['skew']:.2f}")
print("quantiles:", d["q"])

print("\n== minutes explain ==")
me = report["minutes_explain"]
print(f"player-seasons={me['player_seasons']:,} median R2(pts~min)={me['median_r2_pts_on_min']:.3f} "
      f"(p25 {me['p25_r2']:.3f}, p75 {me['p75_r2']:.3f})")

print("\n== dispersion by trailing mean ==")
print(pd.DataFrame(rows).to_string(index=False))

print("\n== push at the integer nearest the trailing mean ==")
print(pd.DataFrame(push_rows).head(12).to_string(index=False))
print(f"weighted mean push rate: {report['push_rate_mean']:.4f}")

print("\n== home/away ==", report["home_away"])
print("== rest ==", report["rest_effect"])
print("== year over year rate ==", report["year_over_year_rate"])
print("== trailing-10 predictor ==", report["trailing_10_predictor"])
