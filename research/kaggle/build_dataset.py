"""
Build the PropsLab training extract.

One row per player-game, regular season, games actually played, with a full
leakage-free trailing window. Everything is lagged: a game never contributes to
its own features.

Design notes
------------
* Team pace and defensive strength are derived from RAW COUNTING STATS
  (fga, fta, oreb, tov, pts). The precomputed `def_rating` / `pace` / `poss`
  columns are 0 for 93% of 2025-26, which is inside the holdout, so trusting them
  would silently blank the evaluation season.
* `player_boxscores` contains only appearances (56 zero-minute rows out of
  787,531; `min` is never null). There are no DNP rows, so "did he play" is not a
  label this dataset can carry. All labels are conditional on playing, which
  matches how books settle props.
* `mu` is the champion mean: 0.5 * season-to-date + 0.5 * trailing-10, floored
  at 0. The equal weighting came from a regression on this same corpus
  (coefficients 0.469 / 0.471), not from preference.

Output: propslab_training.parquet + schema.json + manifest.json
"""
import hashlib
import json
import sqlite3
from pathlib import Path

import numpy as np
import pandas as pd

DB = r"file:D:/datasets/sports-datasets/kaggle/chevronronson__nba-stats-dataset/sql/nba_stats.sqlite?mode=ro"
OUT = Path(r"C:\Users\tap45\Downloads\PropsLab\research\kaggle\dataset")
OUT.mkdir(parents=True, exist_ok=True)

ROLL = 10
MIN_PERIODS = 5


def log(msg: str) -> None:
    print(msg, flush=True)


con = sqlite3.connect(DB, uri=True)

# ------------------------------------------------------------------ players
log("reading player_boxscores ...")
players = pd.read_sql_query(
    """
    select season_year, player_id, player_name, team_id, game_id, game_date,
           is_home, min, pts, fga, fta, tov, usg_pct
    from player_boxscores
    where substr(game_id,1,3) = '002'
    """,
    con,
)
players["date"] = pd.to_datetime(players["game_date"], errors="coerce")
for c in ("min", "pts", "fga", "fta", "tov", "usg_pct", "is_home"):
    players[c] = pd.to_numeric(players[c], errors="coerce")

n0 = len(players)
players = players.dropna(subset=["date", "min", "pts", "team_id"])
players = players[players["min"] > 0]
log(f"  rows {n0:,} -> played {len(players):,}")

players = players.sort_values(["player_id", "date", "game_id"]).reset_index(drop=True)
g = players.groupby("player_id", sort=False)

# ------------------------------------------------------- player trailing form
players["t_pts"] = g["pts"].transform(lambda s: s.shift().rolling(ROLL, min_periods=MIN_PERIODS).mean())
players["t_min"] = g["min"].transform(lambda s: s.shift().rolling(ROLL, min_periods=MIN_PERIODS).mean())
players["t_sd"] = g["pts"].transform(lambda s: s.shift().rolling(ROLL, min_periods=MIN_PERIODS).std())
players["t_usg"] = g["usg_pct"].transform(lambda s: s.shift().rolling(ROLL, min_periods=MIN_PERIODS).mean())

# season-to-date, same season only, still lagged
gs = players.groupby(["player_id", "season_year"], sort=False)
players["s_pts"] = gs["pts"].transform(lambda s: s.shift().expanding(min_periods=MIN_PERIODS).mean())
players["s_min"] = gs["min"].transform(lambda s: s.shift().expanding(min_periods=MIN_PERIODS).mean())
players["n_prior_season"] = gs.cumcount()

# rest
players["rest_days"] = g["date"].diff().dt.days
players["rest_days"] = players["rest_days"].clip(lower=1, upper=14).fillna(7)
players["b2b"] = (players["rest_days"] <= 1).astype(int)

# champion mean
players["mu"] = (0.5 * players["t_pts"] + 0.5 * players["s_pts"]).clip(lower=0.0)

# -------------------------------------------------------------------- teams
log("reading team_boxscores ...")
teams = pd.read_sql_query(
    """
    select season_year, team_id, game_id, game_date, is_home,
           fga, fta, oreb, tov, pts
    from team_boxscores
    where substr(game_id,1,3) = '002'
    """,
    con,
)
teams["date"] = pd.to_datetime(teams["game_date"], errors="coerce")
for c in ("fga", "fta", "oreb", "tov", "pts"):
    teams[c] = pd.to_numeric(teams[c], errors="coerce")
teams = teams.dropna(subset=["date", "pts"]).sort_values(["team_id", "date", "game_id"]).reset_index(drop=True)

# possessions estimate, standard form: FGA + 0.44*FTA - OREB + TOV
teams["poss"] = teams["fga"] + 0.44 * teams["fta"] - teams["oreb"] + teams["tov"]
teams["poss"] = teams["poss"].clip(lower=1.0)

# opponent per game, from the two rows sharing a game_id
opp = teams[["game_id", "team_id", "pts", "poss"]].rename(
    columns={"team_id": "opp_team_id", "pts": "opp_pts", "poss": "opp_poss"}
)
teams = teams.merge(opp, on="game_id", how="left")
teams = teams[teams["team_id"] != teams["opp_team_id"]]
assert len(teams) == 2 * teams["game_id"].nunique(), "team rows are not exactly two per game"

teams["game_poss"] = 0.5 * (teams["poss"] + teams["opp_poss"])
teams["def100"] = teams["opp_pts"] / teams["game_poss"] * 100.0

# trailing team form, lagged
tg = teams.groupby("team_id", sort=False)
teams["own_pace_trail"] = tg["game_poss"].transform(
    lambda s: s.shift().rolling(ROLL, min_periods=MIN_PERIODS).mean())
teams["def100_trail"] = tg["def100"].transform(
    lambda s: s.shift().rolling(ROLL, min_periods=MIN_PERIODS).mean())

ctx = teams[["game_id", "team_id", "opp_team_id", "own_pace_trail", "def100_trail"]]

# --------------------------------------------------------------------- join
log("joining team context ...")
df = players.merge(ctx, on=["game_id", "team_id"], how="left", validate="many_to_one")

# opponent defensive strength = the opponent team's trailing def100
opp_def = ctx[["game_id", "team_id", "def100_trail"]].rename(
    columns={"team_id": "opp_team_id", "def100_trail": "opp_def100_trail"})
df = df.merge(opp_def, on=["game_id", "opp_team_id"], how="left", validate="many_to_one")

df = df.dropna(subset=["t_pts", "t_min", "t_sd", "s_pts", "s_min", "mu"]).copy()
log(f"  rows with a full trailing window: {len(df):,}")

df["t_usg"] = df["t_usg"].fillna(df["t_usg"].median())
df["own_pace_trail"] = df["own_pace_trail"].fillna(df["own_pace_trail"].median())
df["opp_def100_trail"] = df["opp_def100_trail"].fillna(df["opp_def100_trail"].median())
df["n_prior_season"] = df["n_prior_season"].clip(upper=82)

keep = [
    "season_year", "game_id", "game_date", "player_id", "player_name",
    "team_id", "opp_team_id", "is_home", "rest_days", "b2b", "n_prior_season",
    "t_pts", "t_min", "t_sd", "t_usg", "s_pts", "s_min", "mu",
    "own_pace_trail", "opp_def100_trail",
    "pts", "min",
]
out = df[keep].sort_values(["game_date", "game_id", "player_id"]).reset_index(drop=True)

for c in ("is_home", "b2b", "n_prior_season", "pts", "min"):
    out[c] = out[c].astype("int64")
for c in ("rest_days", "t_pts", "t_min", "t_sd", "t_usg", "s_pts", "s_min",
          "mu", "own_pace_trail", "opp_def100_trail"):
    out[c] = out[c].astype("float32")

# --------------------------------------------------------------- sanity gate
assert out["mu"].between(0, 60).all(), "mu out of range"
assert out["pts"].between(0, 120).all(), "points out of range"
assert not out.duplicated(["player_id", "game_id"]).any(), "duplicate player-game"
assert out["game_date"].notna().all()
assert out["opp_team_id"].notna().all(), "opponent join failed"
assert (out["team_id"] != out["opp_team_id"]).all()

seasons = sorted(out["season_year"].unique())
log(f"  seasons {seasons[0]} .. {seasons[-1]} ({len(seasons)})")
log(f"  final rows: {len(out):,}  players: {out['player_id'].nunique():,}  games: {out['game_id'].nunique():,}")

# ------------------------------------------------------------------- export
parquet_path = OUT / "propslab_training.parquet"
out.to_parquet(parquet_path, index=False, compression="zstd")

sample = out.sample(n=min(10_000, len(out)), random_state=42).sort_values(["game_date", "game_id"])
sample.to_csv(OUT / "propslab_training_sample.csv", index=False)

schema = {
    "rows": int(len(out)),
    "columns": {c: str(out[c].dtype) for c in out.columns},
    "seasons": [seasons[0], seasons[-1]],
    "season_count": len(seasons),
    "players": int(out["player_id"].nunique()),
    "games": int(out["game_id"].nunique()),
    "label": "pts (points scored in the game); binary target is built in the kernel as pts > line",
    "notes": [
        "Regular season only (game_id prefix 002).",
        "Games actually played only: player_boxscores contains no DNP rows.",
        "All player features are shift(1)-lagged; a game never contributes to its own features.",
        "mu = 0.5*season_to_date + 0.5*trailing_10, floored at 0.",
        "own_pace_trail and opp_def100_trail are derived from raw counting stats, not the precomputed rating columns.",
    ],
}
(OUT / "schema.json").write_text(json.dumps(schema, indent=2), encoding="utf-8")

def sha256(p: Path) -> str:
    h = hashlib.sha256()
    with p.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()

manifest = {
    "source_db": "chevronronson__nba-stats-dataset/sql/nba_stats.sqlite",
    "source_license": "CC0-1.0",
    "built_at_utc": pd.Timestamp.utcnow().isoformat(),
    "rows": int(len(out)),
    "files": [
        {"path": p.name, "bytes": p.stat().st_size, "sha256": sha256(p)}
        for p in (parquet_path, OUT / "propslab_training_sample.csv", OUT / "schema.json")
    ],
}
(OUT / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")

log("\nwrote:")
for f in sorted(OUT.iterdir()):
    log(f"  {f.name:34s} {f.stat().st_size/1e6:8.2f} MB")
log("\nmu / pts summary:")
log(out[["mu", "pts", "t_sd", "t_min"]].describe().to_string())
