"""Gate the extract before it is uploaded. Any failure here stops the run."""
import sqlite3

import numpy as np
import pandas as pd

DB = r"file:D:/datasets/sports-datasets/kaggle/chevronronson__nba-stats-dataset/sql/nba_stats.sqlite?mode=ro"
P = r"C:\Users\tap45\Downloads\PropsLab\research\kaggle\dataset\propslab_training.parquet"
df = pd.read_parquet(P)
con = sqlite3.connect(DB, uri=True)
fails = []


def check(name, ok, detail=""):
    print(f"[{'PASS' if ok else 'FAIL'}] {name} {detail}")
    if not ok:
        fails.append(name)


# 1. split sizes
fit = df[df["season_year"] <= "2022-23"]
hold = df[df["season_year"].isin(["2023-24", "2024-25", "2025-26"])]
check("fit rows > 500k", len(fit) > 500_000, f"({len(fit):,})")
check("holdout rows > 60k", len(hold) > 60_000, f"({len(hold):,})")
check("no season overlap", len(set(fit.season_year) & set(hold.season_year)) == 0)
check("holdout seasons complete", set(hold.season_year.unique()) == {"2023-24", "2024-25", "2025-26"})

# 2. nulls
nulls = df.isna().sum()
check("no nulls anywhere", int(nulls.sum()) == 0, f"({int(nulls.sum())} nulls)")

# 3. the 2025-26 trap: opponent defense must still be populated and sane
h26 = df[df["season_year"] == "2025-26"]
check("2025-26 opp_def100_trail populated", h26["opp_def100_trail"].notna().all())
check("2025-26 opp_def100_trail sane", 95 < h26["opp_def100_trail"].mean() < 125,
      f"(mean {h26['opp_def100_trail'].mean():.1f})")
check("2025-26 pace sane", 90 < h26["own_pace_trail"].mean() < 115,
      f"(mean {h26['own_pace_trail'].mean():.1f})")

# 4. leakage spot-check: recompute t_pts for 3 players straight from SQL
print("\n-- leakage spot-check (recompute trailing mean from raw SQL) --")
bad = 0
checked = 0
for pid in df["player_id"].drop_duplicates().sample(3, random_state=0):
    raw = pd.read_sql_query(
        """select game_id, game_date, pts, min from player_boxscores
           where player_id=? and substr(game_id,1,3)='002' and min>0
           order by game_date, game_id""", con, params=(int(pid),))
    raw = raw.reset_index(drop=True)
    ours = df[df["player_id"] == pid].sort_values(["game_date", "game_id"]).reset_index(drop=True)
    for i in range(len(raw)):
        if i < 5:
            continue
        window = raw["pts"].iloc[max(0, i - 10):i]
        expect = window.mean()
        row = ours[ours["game_id"] == raw["game_id"].iloc[i]]
        if row.empty:
            continue
        got = float(row["t_pts"].iloc[0])
        checked += 1
        if abs(got - expect) > 1e-3:
            bad += 1
            if bad <= 3:
                print(f"   mismatch player={pid} game={raw['game_id'].iloc[i]} got={got:.4f} expect={expect:.4f}")
check("trailing mean excludes current game", bad == 0, f"({checked} rows checked, {bad} mismatches)")

# 5. the label must not be inside the features
corr = df[["mu", "t_pts", "s_pts", "pts"]].corr()["pts"]
print("\ncorrelation with pts:\n", corr.to_string())
check("mu correlation is plausible, not suspicious", 0.60 < corr["mu"] < 0.80, f"(r={corr['mu']:.3f})")

# 6. no duplicate player-game
check("unique player-game", not df.duplicated(["player_id", "game_id"]).any())

# 7. every game has exactly two teams
teams_per_game = df.groupby("game_id")["team_id"].nunique()
check("two teams per game", (teams_per_game <= 2).all() and (teams_per_game == 2).mean() > 0.99)

print("\nseason coverage:")
print(df.groupby("season_year").size().to_string())
print("\nRESULT:", "ALL GATES PASSED" if not fails else f"{len(fails)} FAILED: {fails}")
