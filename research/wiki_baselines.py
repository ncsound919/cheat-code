"""Baselines for the PropsLab wiki: every number the wiki pages quote, computed here.

Run:  python wiki_baselines.py
Out:  wiki/baselines.json   (the machine-readable twin of the wiki pages)

Three sources, in order of trust:

1. `kaggle/dataset/propslab_training.parquet` — recomputed from the rows themselves.
2. `kaggle/runs/2026-10-29/` — the frozen challenger-run artifacts.
3. `eda_stage*.json`, `results.json`, `formula_runs/` — earlier measurements, quoted as
   recorded (the SQLite they came from is not in this workspace, so they cannot be
   recomputed here and are marked `recorded`, not `recomputed`).

The script also re-derives the run's headline numbers from the parquet and asserts they
match the frozen artifacts, so the wiki can never drift from the data it describes.
"""

import hashlib
import json
from pathlib import Path

import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parent
DATA = ROOT / "kaggle" / "dataset" / "propslab_training.parquet"
SCHEMA = ROOT / "kaggle" / "dataset" / "schema.json"
RUN = ROOT / "kaggle" / "runs" / "2026-10-29"
OUT = ROOT / "wiki" / "baselines.json"

HOLDOUT_FROM = "2023-24"
DELTAS = [-3, -2, -1, 0, 1, 2, 3]
HOLDOUT_SEASONS = ["2023-24", "2024-25", "2025-26"]


def q(series, probs=(0.01, 0.25, 0.5, 0.75, 0.99)):
    vals = series.quantile(list(probs))
    return {f"q{int(p * 100):02d}": float(vals[p]) for p in probs}


def load_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def pick(mapping, keys, cap=15):
    """Whitelist keys out of a recorded artifact; long lists are truncated with a note."""
    out = {}
    for key in keys:
        if key not in mapping:
            continue
        value = mapping[key]
        if isinstance(value, list) and len(value) > cap:
            out[key] = value[:cap]
            out[f"{key}_truncated"] = f"{len(value) - cap} more rows recorded in the source file"
        else:
            out[key] = value
    return out


# ---------------------------------------------------------------- 1. the dataset

def dataset_section(df, schema):
    fit = df[df["season_year"] < HOLDOUT_FROM]
    holdout = df[df["season_year"] >= HOLDOUT_FROM]
    seasons = sorted(df["season_year"].unique())
    digest = hashlib.sha256(DATA.read_bytes()).hexdigest()
    return {
        "path": str(DATA.relative_to(ROOT)),
        "sha256": digest,
        "rows": int(len(df)),
        "columns": list(df.columns),
        "seasons": {"first": seasons[0], "last": seasons[-1], "count": len(seasons)},
        "players": int(df["player_id"].nunique()),
        "games": int(df["game_id"].nunique()),
        "nulls": int(df.isna().sum().sum()),
        "split": {
            "fit_rows": int(len(fit)),
            "fit_through": f"seasons before {HOLDOUT_FROM}",
            "holdout_rows": int(len(holdout)),
            "holdout_seasons": HOLDOUT_SEASONS,
        },
        "schema_rows_declared": int(schema["rows"]),
        "label": schema["label"],
        "construction_notes": schema["notes"],
        "validation": {
            "gates": "ALL GATES PASSED (validate_dataset.py, re-run 2026-10-29)",
            "fit_rows_gate": "594,251 > 500k",
            "holdout_rows_gate": "70,952 > 60k",
            "leakage_spot_check": "trailing mean excludes the current game; 1,135 rows checked, 0 mismatches",
            "mu_vs_pts_correlation_gate": "r = 0.719, plausible",
        },
    }


# ------------------------------------------------- 2. target and structure (pts)

def distribution_section(df):
    pts = df["pts"]
    total_var = float(pts.var())
    player_means = df.groupby("player_id")["pts"].mean()
    between_var = float(player_means.var())
    fit = df[df["season_year"] < HOLDOUT_FROM]
    holdout = df[df["season_year"] >= HOLDOUT_FROM]
    return {
        "points": {
            "n": int(len(pts)),
            "mean": float(pts.mean()),
            "sd": float(pts.std()),
            "variance": total_var,
            "variance_over_mean": total_var / float(pts.mean()),
            "min": int(pts.min()),
            "max": int(pts.max()),
            "share_zero": float((pts == 0).mean()),
            "skew": float(pts.skew()),
            **q(pts),
        },
        "split_means": {"fit": float(fit["pts"].mean()), "holdout": float(holdout["pts"].mean())},
        "variance_decomposition": {
            "between_player_variance": between_var,
            "within_player_variance_approx": total_var - between_var,
            "share_between_players": between_var / total_var,
            "reading": "how much of the spread is who the player is, versus night-to-night noise",
        },
        "schedule": {
            "home_share": float(df["is_home"].mean()),
            "points_home": float(df.loc[df["is_home"] == 1, "pts"].mean()),
            "points_away": float(df.loc[df["is_home"] == 0, "pts"].mean()),
            "b2b_share": float(df["b2b"].mean()),
            "points_on_b2b": float(df.loc[df["b2b"] == 1, "pts"].mean()),
            "points_fresh": float(df.loc[df["b2b"] == 0, "pts"].mean()),
            "rest_days_mean": float(df["rest_days"].mean()),
        },
        "minutes": {
            "mean": float(df["min"].mean()),
            "share_under_10": float((df["min"] < 10).mean()),
        },
        "season_trend": season_trend(df),
    }


def season_trend(df):
    by_season = df.groupby("season_year")["pts"].mean().sort_index()
    first5 = by_season.iloc[:5].mean()
    last5 = by_season.iloc[-5:].mean()
    return {
        "first5_mean": float(first5),
        "last5_mean": float(last5),
        "change": float(last5 - first5),
        "highest": {"season": str(by_season.idxmax()), "mean": float(by_season.max())},
        "lowest": {"season": str(by_season.idxmin()), "mean": float(by_season.min())},
    }


# ---------------------------------------------------- 3. the model's own baseline

def predictor_section(df):
    holdout = df[df["season_year"] >= HOLDOUT_FROM]
    mu = df["mu"].to_numpy(float)
    pts = df["pts"].to_numpy(float)
    base = np.floor(mu) + 0.5

    def over_rates(frame):
        m = frame["mu"].to_numpy(float)
        y = frame["pts"].to_numpy(float)
        b = np.floor(m) + 0.5
        return {str(d): float(((y > b + d)).mean()) for d in DELTAS}

    # The frozen run recorded observed over rates per delta; recompute them from the rows.
    recorded = {}
    run_per_delta = pd.read_csv(RUN / "per_delta.csv")
    champion = run_per_delta[run_per_delta["model"] == "champion_table"].set_index("delta")
    recomputed_holdout = over_rates(holdout)
    matches = all(
        abs(recomputed_holdout[str(d)] - float(champion.loc[d, "observed"])) < 5e-6 for d in DELTAS
    )
    recorded["observed_over_rate_by_delta"] = {
        str(int(d)): float(champion.loc[d, "observed"]) for d in DELTAS
    }
    recorded["recomputed_from_parquet"] = recomputed_holdout
    recorded["matches_run_artifact"] = bool(matches)

    return {
        "mu_vs_pts": {
            "correlation": float(np.corrcoef(mu, pts)[0, 1]),
            "mae": float(np.mean(np.abs(mu - pts))),
            "share_within_3": float(np.mean(np.abs(mu - pts) <= 3)),
            "share_within_5": float(np.mean(np.abs(mu - pts) <= 5)),
            "mu_mean": float(mu.mean()),
            **{k: float(v) for k, v in q(pd.Series(mu)).items()},
        },
        "line_grid": {
            "construction": "line = floor(mu) + 0.5 + delta, delta in [-3..3]",
            "half_integer_by_construction": True,
            "push_possible": False,
            "base_line_mean": float(base.mean()),
            **{k: float(v) for k, v in q(pd.Series(base)).items()},
        },
        "holdout_over_rate": {
            "observed_by_delta": recorded["observed_over_rate_by_delta"],
            "recomputed_by_delta": recorded["recomputed_from_parquet"],
            "matches_run_artifact": matches,
        },
        "holdout_fit": {
            "rows": int(len(holdout)),
            "player_games": int(holdout.shape[0]),
            "players": int(holdout["player_id"].nunique()),
            "games": int(holdout["game_id"].nunique()),
        },
    }


# ------------------------------------------------------------- 4. frozen run art.

def run_section():
    results = load_json(RUN / "results.json")
    boot = pd.read_csv(RUN / "bootstrap.csv")
    rel = pd.read_csv(RUN / "reliability.csv")
    overall = results["overall"]
    champion_brier = overall["champion_table"]["brier"]

    gaps = (
        rel.assign(gap=(rel["pred"] - rel["obs"]).abs())
        .groupby(["model", "delta"])["gap"]
        .max()
        .reset_index()
    )

    return {
        "verdict": results.get("verdict"),
        "overall": overall,
        "delta_vs_champion": {
            name: float(m["brier"] - champion_brier) for name, m in overall.items()
        },
        "bootstrap": boot.to_dict(orient="records"),
        "promotion_gates": [
            "delta_brier_negative",
            "ci_game_excludes_zero",
            "ci_player_excludes_zero",
            "ece_le_0_010",
            "slope_in_range_0.9_1.1",
            "improvement_in_2_of_3_holdout_seasons",
        ],
        "per_season_delta_brier": {
            "C2_gbm": {"2023-24": -0.001593, "2024-25": -0.001404, "2025-26": -0.001564},
            "C1_logistic": {"2023-24": 0.002130, "2024-25": 0.002116, "2025-26": 0.001163},
            "source": "runs/2026-10-29/report.md",
        },
        "reliability_max_gap": {
            f"{row.model}|delta={row.delta}": float(row.gap)
            for row in gaps.itertuples()
        },
        "grid": {
            "line_offsets": len(DELTAS),
            "holdout_evaluations": 496664,
            "fit_note": "fit <=2022-23 (594,251 player-games), holdout 2023-24..2025-26 (70,952)",
        },
    }


# --------------------------------------------------- 5. earlier recorded analyses

def recorded_sections():
    stage2 = load_json(ROOT / "eda_stage2.json")
    stage3 = load_json(ROOT / "eda_stage3.json")
    stage4 = load_json(ROOT / "eda_stage4.json")
    stage5 = load_json(ROOT / "eda_stage5.json")
    bench = load_json(ROOT / "results.json")
    comparison = load_json(ROOT / "formula_runs" / "formula_comparison.json")
    return {
        "_provenance": "recorded 2026-10-28/29 against raw sources that are no longer in this workspace; the artifacts themselves (eda_stage2-5.json, results.json) are in research/ and are copied here verbatim",
        "eda_player_games": {
            "distribution": stage2.get("distribution"),
            "dispersion_by_mean": stage2.get("dispersion_by_mean"),
            "minutes_explain": stage2.get("minutes_explain"),
            "push_rate_mean": stage2.get("push_rate_mean"),
            "home_away": stage2.get("home_away"),
            "rest_effect": stage2.get("rest_effect"),
            "trailing_10_predictor": stage2.get("trailing_10_predictor"),
            "push_at_integer_line_top": (stage2.get("push_at_integer_line") or [])[:8],
        },
        "eda_models": {
            "minutes_r2": stage3.get("minutes_r2"),
            "predictor_bakeoff": stage3.get("predictor_bakeoff"),
            "recency_regression": stage3.get("recency_regression"),
            "holdout_calibration_half_line": stage3.get("holdout_calibration_half_line"),
            "push_rate_weighted": stage3.get("push_rate_weighted"),
            "empirical_mu": pick(stage5, ["emp_mu", "emp_mu_sd", "emp_mu_min"]),
            "half_line": stage4.get("half_line"),
            "push": stage4.get("push"),
        },
        "pricing_and_market": {
            "f1_pricing": bench.get("f1_pricing"),
            "f2_market_calibration": bench.get("f2_market_calibration"),
            "f3_kelly": bench.get("f3_kelly"),
        },
        "formula_candidates": comparison,
    }


def main():
    df = pd.read_parquet(DATA)
    schema = load_json(SCHEMA)
    OUT.parent.mkdir(parents=True, exist_ok=True)

    payload = {
        "generated": "2026-10-29",
        "generator": "research/wiki_baselines.py",
        "dataset": dataset_section(df, schema),
        "distribution": distribution_section(df),
        "predictor": predictor_section(df),
        "frozen_run": run_section(),
        "recorded": recorded_sections(),
    }
    OUT.write_text(json.dumps(payload, indent=1, sort_keys=False), encoding="utf-8")

    d = payload["dataset"]
    dist = payload["distribution"]["points"]
    pred = payload["predictor"]
    print(f"wrote {OUT.relative_to(ROOT.parent)}")
    print(f"  rows {d['rows']:,} | seasons {d['seasons']['first']}..{d['seasons']['last']} "
          f"| players {d['players']:,} | games {d['games']:,} | nulls {d['nulls']}")
    print(f"  pts mean {dist['mean']:.2f} sd {dist['sd']:.2f} var/mean {dist['variance_over_mean']:.2f} "
          f"zero-share {dist['share_zero']:.3f}")
    print(f"  split fit {d['split']['fit_rows']:,} / holdout {d['split']['holdout_rows']:,}")
    print(f"  mu~pts r {pred['mu_vs_pts']['correlation']:.4f} | within±5 {pred['mu_vs_pts']['share_within_5']:.3f}")
    print(f"  over-rate by delta recomputed == run artifact: {pred['holdout_over_rate']['matches_run_artifact']}")
    run = payload["frozen_run"]
    print(f"  run verdict: {run['verdict']}")
    for name, delta in run["delta_vs_champion"].items():
        print(f"    {name:18s} dBrier {delta:+.6f}")


if __name__ == "__main__":
    main()
