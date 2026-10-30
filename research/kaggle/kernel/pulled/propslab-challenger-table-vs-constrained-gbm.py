"""
PropsLab challenger run.

Pre-registered protocol: research/CRITERIA.md (v1.0, frozen before this kernel ran).
Method notes:            research/BEST_PRACTICES.md

Question: does a model that sees where the line sits relative to the player's mean
price P(points > line) better than the mu-keyed empirical table, out of time?

Everything here is deterministic: fixed seeds, fixed grid, fixed features, one
holdout evaluation. If the challenger fails the promotion rule, the table stays.
"""
import hashlib
import json
import os
import platform
import sys
import time
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
import sklearn
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.isotonic import IsotonicRegression
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import GroupKFold
from sklearn.preprocessing import StandardScaler

warnings.filterwarnings("ignore")

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

SEED = 20261029
N_BOOT = int(os.environ.get("PROPSLAB_NBOOT", "10000"))
N_SPLITS = int(os.environ.get("PROPSLAB_SPLITS", "4"))
SUBSAMPLE = int(os.environ.get("PROPSLAB_SUBSAMPLE", "0"))   # 0 = use everything
DELTAS = np.array([-3, -2, -1, 0, 1, 2, 3], dtype=np.int64)
KMAX = 60
BUCKET = 0.5
NB = 121                      # mu buckets 0 .. 60.5
ALPHA = 0.5                   # Laplace smoothing per cell
FIT_MAX_SEASON = "2022-23"
HOLDOUT_SEASONS = ["2023-24", "2024-25", "2025-26"]

EXPECTED_SHA = "962376828535ff49e58b92b4b98cd7c687eabc21326ad68a4ffc66deefba8dbc"
EXPECTED_ROWS = 665_203
EXPECTED_FIT = 594_251
EXPECTED_HOLD = 70_952

FEATURES = [
    "mu", "line", "delta",
    "t_sd", "t_min", "t_usg",
    "rest_days", "b2b", "is_home", "n_prior_season",
    "own_pace_trail", "opp_def100_trail",
]
MONO = [1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0]

CORE = ["mu", "line", "delta", "t_sd", "t_min", "t_usg",
        "is_home", "n_prior_season", "own_pace_trail"]

WORK = Path(os.environ.get("PROPSLAB_WORK", "/kaggle/working"))
INPUT = Path(os.environ.get("PROPSLAB_INPUT", "/kaggle/input/propslab-nba-prop-training"))
T0 = time.time()
CHECKS = {}


def log(msg):
    print(f"[{time.time() - T0:7.1f}s] {msg}", flush=True)


def gate(name, ok, detail=""):
    CHECKS[name] = {"passed": bool(ok), "detail": str(detail)}
    log(f"{'PASS' if ok else 'FAIL'}  {name}  {detail}")
    if not ok:
        raise SystemExit(f"GATE FAILED: {name} ({detail})")


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


# ------------------------------------------------------------------ 0. input
log("verifying dataset ...")
parquet = INPUT / "propslab_training.parquet"
digest = sha256(parquet)
gate("dataset sha256 matches manifest", digest == EXPECTED_SHA, digest[:16])

df = pd.read_parquet(parquet)
gate("row count", len(df) == EXPECTED_ROWS, f"{len(df):,}")
gate("no nulls", int(df.isna().sum().sum()) == 0, f"{int(df.isna().sum().sum())} nulls")

fit = df[df["season_year"] <= FIT_MAX_SEASON].reset_index(drop=True)
hold = df[df["season_year"].isin(HOLDOUT_SEASONS)].reset_index(drop=True)
gate("fit rows", len(fit) == EXPECTED_FIT, f"{len(fit):,}")
gate("holdout rows", len(hold) == EXPECTED_HOLD, f"{len(hold):,}")
gate("no season overlap", len(set(fit.season_year) & set(hold.season_year)) == 0)

if SUBSAMPLE:
    log(f"SMOKE TEST: subsampling to {SUBSAMPLE:,} fit rows and {SUBSAMPLE // 8:,} holdout rows")
    fit = fit.sample(n=min(SUBSAMPLE, len(fit)), random_state=SEED).reset_index(drop=True)
    hold = hold.sample(n=min(max(SUBSAMPLE // 8, 2000), len(hold)),
                       random_state=SEED).reset_index(drop=True)


# ------------------------------------------------------------------ 1. grid
def expand(frame):
    """One row per (player-game, line offset). Half-integer lines: no pushes."""
    n = len(frame)
    m = len(DELTAS)
    mu = frame["mu"].to_numpy(np.float64)
    base = np.floor(mu) + 0.5
    line = np.repeat(base, m) + np.tile(DELTAS, n)
    pts = np.repeat(frame["pts"].to_numpy(np.float64), m)

    X = np.empty((n * m, len(FEATURES)), dtype=np.float32)
    for j, f in enumerate(FEATURES):
        if f == "mu":
            X[:, j] = np.repeat(mu, m)
        elif f == "line":
            X[:, j] = line
        elif f == "delta":
            X[:, j] = np.tile(DELTAS.astype(np.float64), n)
        else:
            X[:, j] = np.repeat(frame[f].to_numpy(np.float64), m)

    y = (pts > line).astype(np.int8)
    meta = {
        "game_id": np.repeat(frame["game_id"].to_numpy(), m),
        "player_id": np.repeat(frame["player_id"].to_numpy(), m),
        "season_year": np.repeat(frame["season_year"].to_numpy(), m),
        "delta": np.tile(DELTAS, n),
        "line": line,
        "mu": np.repeat(mu, m),
        "y": y,
    }
    return X, y, meta


log("expanding grid ...")
Xf, yf, mf = expand(fit)
Xh, yh, mh = expand(hold)
log(f"fit grid {Xf.shape}  holdout grid {Xh.shape}")


# ------------------------------------------------------------- 2. champion
class EmpiricalTable:
    """mu-keyed empirical distribution, Laplace-smoothed, CDF-interpolated."""

    def __init__(self):
        self.cdf = None

    def fit(self, mu, pts):
        b = np.clip(np.floor(mu / BUCKET).astype(np.int64), 0, NB - 1)
        k = np.clip(pts.astype(np.int64), 0, KMAX)
        counts = np.bincount(b * (KMAX + 1) + k, minlength=NB * (KMAX + 1))
        counts = counts.reshape(NB, KMAX + 1).astype(np.float64)
        hist = (counts + ALPHA) / (counts.sum(axis=1, keepdims=True) + ALPHA * (KMAX + 1))

        # empty buckets inherit the nearest populated bucket (forward then backward)
        populated = counts.sum(axis=1) > 0
        for i in range(1, NB):
            if not populated[i] and populated[i - 1]:
                hist[i] = hist[i - 1]
                populated[i] = True
        for i in range(NB - 2, -1, -1):
            if not populated[i] and populated[i + 1]:
                hist[i] = hist[i + 1]
                populated[i] = True
        self.cdf = np.cumsum(hist, axis=1)
        self.cdf = np.clip(self.cdf, 0.0, 1.0)
        return self

    def predict_over(self, mu, line):
        b = np.clip(mu / BUCKET, 0, NB - 1 - 1e-9)
        b0 = np.floor(b).astype(np.int64)
        b1 = np.minimum(b0 + 1, NB - 1)
        w = b - b0
        k = np.floor(line).astype(np.int64)
        kk = np.clip(k, 0, KMAX)
        cdf = (1 - w) * self.cdf[b0, kk] + w * self.cdf[b1, kk]
        cdf = np.where(k < 0, 0.0, cdf)
        cdf = np.where(k >= KMAX, 1.0, cdf)
        return np.clip(1.0 - cdf, 1e-6, 1.0 - 1e-6)


log("fitting champion table ...")
champ = EmpiricalTable().fit(fit["mu"].to_numpy(np.float64), fit["pts"].to_numpy(np.float64))
p_champ = champ.predict_over(mh["mu"], mh["line"])
gate("champion probabilities in (0,1)",
     bool((p_champ > 0).all() and (p_champ < 1).all()),
     f"min {p_champ.min():.2e} max {p_champ.max():.6f}")


# ------------------------------------------------------- 3. challengers
class CalibratedEnsemble:
    """Season-grouped cross-fitted ensemble with per-fold isotonic calibration.

    Each fold model is trained on 3/4 of the fit seasons; its calibrator is fit on
    that fold's held-out predictions. Prediction is the mean of the calibrated
    folds. No fold's calibrator ever sees its own training data.
    """

    def __init__(self, make_model, n_splits=4):
        self.make_model = make_model
        self.n_splits = n_splits
        self.parts = []

    def fit(self, X, y, groups):
        gkf = GroupKFold(n_splits=self.n_splits)
        for i, (tr, va) in enumerate(gkf.split(X, y, groups)):
            t = time.time()
            model = self.make_model()
            model.fit(X[tr], y[tr])
            p_va = model.predict_proba(X[va])[:, 1]
            iso = IsotonicRegression(out_of_bounds="clip", y_min=0.0, y_max=1.0)
            iso.fit(p_va, y[va])
            self.parts.append((model, iso))
            log(f"    fold {i + 1}/{self.n_splits} rows={len(tr):,} "
                f"raw_va_mean={p_va.mean():.4f} ({time.time() - t:.0f}s)")
        return self

    def predict(self, X):
        acc = np.zeros(len(X), dtype=np.float64)
        for model, iso in self.parts:
            acc += iso.predict(model.predict_proba(X)[:, 1])
        # 1e-6 guard: isotonic can emit exactly 0/1 in pure extreme bins, which would
        # make log loss infinite. The shift is far below any decision-relevant edge.
        return np.clip(acc / len(self.parts), 1e-6, 1.0 - 1e-6)


groups_fit = mf["season_year"]

log("fitting C1 logistic (cross-fitted isotonic) ...")
c1 = CalibratedEnsemble(
    lambda: LogisticRegression(C=1.0, max_iter=200, solver="lbfgs", n_jobs=-1),
    n_splits=N_SPLITS,
)
scaler = StandardScaler().fit(Xf)
c1.fit(scaler.transform(Xf), yf, groups_fit)
p_c1 = c1.predict(scaler.transform(Xh))

log("fitting C2 constrained GBM (cross-fitted isotonic) ...")
c2 = CalibratedEnsemble(
    lambda: HistGradientBoostingClassifier(
        max_iter=200, learning_rate=0.08, max_leaf_nodes=31,
        min_samples_leaf=200, l2_regularization=1.0,
        monotonic_cst=MONO, early_stopping=False, random_state=SEED),
    n_splits=N_SPLITS,
)
c2.fit(Xf, yf, groups_fit)
p_c2 = c2.predict(Xh)

log("fitting C2-core ablation (H4: do rest/opponent-defense matter?) ...")
idx_core = [FEATURES.index(f) for f in CORE]
c2c = CalibratedEnsemble(
    lambda: HistGradientBoostingClassifier(
        max_iter=200, learning_rate=0.08, max_leaf_nodes=31,
        min_samples_leaf=200, l2_regularization=1.0,
        monotonic_cst=[MONO[i] for i in idx_core], early_stopping=False,
        random_state=SEED),
    n_splits=N_SPLITS,
)
c2c.fit(Xf[:, idx_core], yf, groups_fit)
p_c2c = c2c.predict(Xh[:, idx_core])

# base rate per delta, fit on the fit window
base_rates = pd.Series(yf).groupby(mf["delta"]).mean()
p_b0 = np.array([base_rates[d] for d in mh["delta"]], dtype=np.float64)


# ------------------------------------------------------------- 4. metrics
def brier(p, y):
    return float(np.mean((p - y) ** 2))


def logloss(p, y):
    p = np.clip(p, 1e-12, 1 - 1e-12)
    return float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))


def ece_equal_mass(p, y, n_bins=15):
    order = np.argsort(p, kind="mergesort")
    p_s, y_s = p[order], y[order]
    out = 0.0
    used = 0
    for chunk in np.array_split(np.arange(len(p)), n_bins):
        if len(chunk) < 2:
            continue
        out += abs(y_s[chunk].mean() - p_s[chunk].mean())
        used += 1
    return float(out / max(used, 1))


def calib_slope(p, y):
    z = np.log(np.clip(p, 1e-6, 1 - 1e-6) / (1 - np.clip(p, 1e-6, 1 - 1e-6)))
    lr = LogisticRegression(C=1e6, solver="lbfgs", max_iter=1000)
    lr.fit(z.reshape(-1, 1), y)
    return float(lr.coef_[0][0]), float(lr.intercept_[0])


def reliability(p, y, n_bins=15):
    order = np.argsort(p, kind="mergesort")
    p_s, y_s = p[order], y[order]
    rows = []
    for i, chunk in enumerate(np.array_split(np.arange(len(p)), n_bins)):
        if len(chunk) < 2:
            continue
        rows.append({"bin": i, "n": int(len(chunk)),
                     "pred": float(p_s[chunk].mean()),
                     "obs": float(y_s[chunk].mean())})
    return rows


MODELS = {
    "champion_table": p_champ,
    "C1_logistic": p_c1,
    "C2_gbm": p_c2,
    "C2_core_ablation": p_c2c,
    "B0_base_rate": p_b0,
}

log("scoring ...")
overall = {}
for name, p in MODELS.items():
    gate(f"{name} finite", bool(np.isfinite(p).all()))
    gate(f"{name} in (0,1)", bool((p > 0).all() and (p < 1).all()))
    slope, intercept = calib_slope(p, yh)
    overall[name] = {
        "brier": brier(p, yh),
        "logloss": logloss(p, yh),
        "ece": ece_equal_mass(p, yh),
        "calib_slope": slope,
        "calib_intercept": intercept,
        "mean_p": float(p.mean()),
        "observed": float(yh.mean()),
    }

gate("B0 base-rate Brier near 0.25", 0.20 <= overall["B0_base_rate"]["brier"] <= 0.30,
     f"{overall['B0_base_rate']['brier']:.4f}")
if not SUBSAMPLE:
    gate("champion Brier within 0.02 of prior 0.2486",
         abs(overall["champion_table"]["brier"] - 0.2486) < 0.02,
         f"{overall['champion_table']['brier']:.4f}")

# per-delta
per_delta = {}
for name, p in MODELS.items():
    rows = {}
    for d in DELTAS:
        mask = mh["delta"] == d
        rows[str(int(d))] = {
            "n": int(mask.sum()),
            "brier": brier(p[mask], yh[mask]),
            "logloss": logloss(p[mask], yh[mask]),
            "ece": ece_equal_mass(p[mask], yh[mask]),
            "mean_p": float(p[mask].mean()),
            "observed": float(yh[mask].mean()),
        }
    per_delta[name] = rows

# per-season
per_season = {}
for name, p in MODELS.items():
    rows = {}
    for s in HOLDOUT_SEASONS:
        mask = mh["season_year"] == s
        rows[s] = {"n": int(mask.sum()), "brier": brier(p[mask], yh[mask]),
                   "logloss": logloss(p[mask], yh[mask])}
    per_season[name] = rows

# monotonicity: within each player-game, P(over) must not rise with delta.
# `delta` is constrained as well as `line`: the pair (line, delta) moves together
# whenever mu is fixed, so constraining `line` alone does not bind along the data
# manifold. This was caught by the smoke test (0.64% violations) before the real run.
def violation_rate(p):
    mat = p.reshape(-1, len(DELTAS))
    d = np.diff(mat, axis=1)
    return float(np.mean(np.any(d > 1e-7, axis=1)))


def violation_max(p):
    mat = p.reshape(-1, len(DELTAS))
    return float(np.max(np.diff(mat, axis=1)))

mono = {name: violation_rate(p) for name, p in MODELS.items()}
mono_max = {name: violation_max(p) for name, p in MODELS.items()}
gate("GBM monotonicity violations < 0.5%", mono["C2_gbm"] < 0.005,
     f"{mono['C2_gbm'] * 100:.3f}% (max {mono_max['C2_gbm']:+.2e})")


# ------------------------------------------------------------ 5. bootstrap
def cluster_bootstrap(delta_se, clusters, n_boot=N_BOOT, seed=SEED):
    codes, uniq = pd.factorize(clusters)
    n_cl = len(uniq)
    sums = np.bincount(codes, weights=delta_se, minlength=n_cl)
    counts = np.bincount(codes, minlength=n_cl).astype(np.float64)
    rng = np.random.default_rng(seed)
    p = np.full(n_cl, 1.0 / n_cl)
    out = np.empty(n_boot)
    for i in range(n_boot):
        w = rng.multinomial(n_cl, p).astype(np.float64)
        out[i] = (w * sums).sum() / (w * counts).sum()
    return out


log("bootstrap ...")
boot = {}
for name in ("C1_logistic", "C2_gbm", "C2_core_ablation"):
    delta_se = (MODELS[name] - yh) ** 2 - (p_champ - yh) ** 2
    for cluster_name, key in (("game", "game_id"), ("player", "player_id")):
        draws = cluster_bootstrap(delta_se, mh[key])
        boot[f"{name}|{cluster_name}"] = {
            "mean": float(draws.mean()),
            "lo": float(np.percentile(draws, 2.5)),
            "hi": float(np.percentile(draws, 97.5)),
            "n_clusters": int(pd.Series(mh[key]).nunique()),
        }
        log(f"  {name} vs champion, {cluster_name}-clustered: "
            f"{draws.mean():+.6f} [{np.percentile(draws, 2.5):+.6f}, "
            f"{np.percentile(draws, 97.5):+.6f}]")

pd.DataFrame([
    {"model": k.split("|")[0], "cluster": k.split("|")[1], **v}
    for k, v in boot.items()
]).to_csv(WORK / "bootstrap.csv", index=False)

pd.DataFrame([
    {"model": name, "delta": d, **vals}
    for name, rows in per_delta.items() for d, vals in rows.items()
]).to_csv(WORK / "per_delta.csv", index=False)

pd.DataFrame([
    {"model": name, "bin": r["bin"], "n": r["n"], "pred": r["pred"], "obs": r["obs"]}
    for name, p in MODELS.items() for r in reliability(p, yh)
]).to_csv(WORK / "reliability.csv", index=False)


# ------------------------------------------------------------- 6. verdict
def evaluate_promotion(name):
    base = overall["champion_table"]["brier"]
    point = overall[name]["brier"] - base
    g = boot[f"{name}|game"]
    pl = boot[f"{name}|player"]
    seasons = [per_season[name][s]["brier"] - per_season["champion_table"][s]["brier"]
               for s in HOLDOUT_SEASONS]
    conds = {
        "delta_brier_negative": point < 0,
        "ci_game_excludes_zero": g["hi"] < 0,
        "ci_player_excludes_zero": pl["hi"] < 0,
        "ece_le_0_010": overall[name]["ece"] <= 0.010,
        "slope_in_range": 0.90 <= overall[name]["calib_slope"] <= 1.10,
        "two_of_three_seasons": sum(1 for s in seasons if s < 0) >= 2,
    }
    return {
        "model": name,
        "delta_brier": point,
        "per_season_delta": dict(zip(HOLDOUT_SEASONS, seasons)),
        "conditions": conds,
        "promoted": all(conds.values()),
    }


promotion = {n: evaluate_promotion(n) for n in ("C1_logistic", "C2_gbm")}
winner = [n for n, v in promotion.items() if v["promoted"]]
verdict = ("PROMOTE " + ", ".join(winner)) if winner else "KEEP CHAMPION TABLE"

# --------------------------------------------------------- 7. artifacts
table_export = {
    "kind": "propslab_empirical_distribution",
    "version": 1,
    "bucket_width": BUCKET,
    "kmax": KMAX,
    "laplace_alpha": ALPHA,
    "fit_max_season": FIT_MAX_SEASON,
    "fit_rows": int(len(fit)),
    "source_dataset": "tap919/propslab-nba-prop-training",
    "source_sha256": EXPECTED_SHA,
    "note": "cdf[b][k] = P(points <= k | mu bucket b); interpolate linearly in mu.",
    "cdf": [[round(float(v), 8) for v in row] for row in champ.cdf],
}
(WORK / "champion_table.json").write_text(json.dumps(table_export), encoding="utf-8")

env = {
    "python": platform.python_version(),
    "numpy": np.__version__,
    "pandas": pd.__version__,
    "sklearn": sklearn.__version__,
    "seed": SEED,
    "n_boot": N_BOOT,
    "deltas": DELTAS.tolist(),
    "features": FEATURES,
    "monotonic_cst": MONO,
    "fit_rows": int(len(fit)),
    "holdout_rows": int(len(hold)),
    "grid_rows_fit": int(Xf.shape[0]),
    "grid_rows_holdout": int(Xh.shape[0]),
    "runtime_seconds": round(time.time() - T0, 1),
}

results = {
    "verdict": verdict,
    "overall": overall,
    "per_delta": per_delta,
    "per_season": per_season,
    "monotonicity_violation_rate": mono,
    "monotonicity_max_violation": mono_max,
    "bootstrap": boot,
    "promotion": promotion,
    "gates": CHECKS,
    "environment": env,
}
(WORK / "results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
(WORK / "environment.json").write_text(json.dumps(env, indent=2), encoding="utf-8")

# ---------------------------------------------------------------- 8. report
lines = []
lines.append("# PropsLab challenger run — result\n")
lines.append(f"**Verdict: {verdict}**\n")
lines.append(f"Dataset `tap919/propslab-nba-prop-training` "
             f"(sha256 `{EXPECTED_SHA[:16]}…`). "
             f"Fit ≤{FIT_MAX_SEASON} ({len(fit):,} player-games), "
             f"holdout {', '.join(HOLDOUT_SEASONS)} ({len(hold):,}). "
             f"Grid: {len(DELTAS)} line offsets, {Xh.shape[0]:,} holdout evaluations.\n")
lines.append("## Headline\n")
lines.append("| model | Brier | log loss | ECE | slope | ΔBrier vs table |")
lines.append("|---|---:|---:|---:|---:|---:|")
for name in ("champion_table", "C1_logistic", "C2_gbm", "C2_core_ablation", "B0_base_rate"):
    m = overall[name]
    d = "" if name == "champion_table" else f"{m['brier'] - overall['champion_table']['brier']:+.6f}"
    lines.append(f"| {name} | {m['brier']:.6f} | {m['logloss']:.6f} | {m['ece']:.5f} | "
                 f"{m['calib_slope']:.3f} | {d} |")

lines.append("\n## Promotion rule (all six must hold)\n")
for name, pr in promotion.items():
    lines.append(f"### {name} — {'PROMOTED' if pr['promoted'] else 'REJECTED'}\n")
    for k, v in pr["conditions"].items():
        lines.append(f"- [{'x' if v else ' '}] {k}")
    lines.append(f"- per-season ΔBrier: " +
                 ", ".join(f"{s} {d:+.6f}" for s, d in pr["per_season_delta"].items()))
    lines.append("")

lines.append("## Bootstrap (ΔBrier vs champion, 95% CI)\n")
lines.append("| model | cluster | mean | lo | hi | clusters |")
lines.append("|---|---|---:|---:|---:|---:|")
for k, v in boot.items():
    name, cluster = k.split("|")
    lines.append(f"| {name} | {cluster} | {v['mean']:+.6f} | {v['lo']:+.6f} | "
                 f"{v['hi']:+.6f} | {v['n_clusters']:,} |")

lines.append("\n## Brier by line offset\n")
lines.append("| δ | n | champion | C1 logistic | C2 GBM |")
lines.append("|---:|---:|---:|---:|---:|")
for d in DELTAS:
    k = str(int(d))
    lines.append(f"| {d:+d} | {per_delta['champion_table'][k]['n']:,} | "
                 f"{per_delta['champion_table'][k]['brier']:.5f} | "
                 f"{per_delta['C1_logistic'][k]['brier']:.5f} | "
                 f"{per_delta['C2_gbm'][k]['brier']:.5f} |")

lines.append("\n## Monotonicity violations (fraction of player-games)\n")
for name, v in mono.items():
    lines.append(f"- {name}: {v * 100:.3f}%")

lines.append("\n## Scope limits (from CRITERIA.md §12)\n")
lines.append("- No real prop lines exist in this corpus: this prices a synthetic line, "
             "so **no edge claim is possible**.\n"
             "- All probabilities are `P(over | plays)`; availability is not modelled.\n"
             "- Playoffs excluded. No closing-line movement test.\n")

(WORK / "report.md").write_text("\n".join(lines), encoding="utf-8")

log(f"VERDICT: {verdict}")
log(f"done in {time.time() - T0:.0f}s")
print("\n".join(lines[:40]))
