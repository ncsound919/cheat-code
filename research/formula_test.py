"""Formula testing harness for PropsLab (vectorized)."""
import json
import math
from pathlib import Path

import numpy as np
import pandas as pd

DATA = Path(r"C:\Users\tap45\Downloads\PropsLab\research\kaggle\dataset\propslab_training.parquet")
OUT = Path(r"C:\Users\tap45\Downloads\PropsLab\research\formula_runs")
OUT.mkdir(parents=True, exist_ok=True)
DELTAS = np.array([-3, -2, -1, 0, 1, 2, 3])


def cand_normal(df):
    mu = df["mu"].to_numpy(float); sd = np.maximum(df["t_sd"].to_numpy(float), 1.0)
    line = df["line"].to_numpy(float)
    z = (line + 0.5 - mu) / (sd * np.sqrt(2))
    from scipy.special import erfc
    return 0.5 * erfc(z)


def cand_logistic_simple(df):
    z = 0.35 * (df["mu"].to_numpy(float) - df["line"].to_numpy(float))
    return 1.0 / (1.0 + np.exp(-z))


def cand_poisson(df):
    from scipy.stats import poisson
    mu = np.maximum(df["mu"].to_numpy(float), 1e-3)
    line = df["line"].to_numpy(float)
    k = np.floor(line).astype(int)
    # P(X > line) = 1 - CDF(floor(line))
    cdf = poisson.cdf(k, mu)
    return np.clip(1.0 - cdf, 1e-6, 1 - 1e-6)


def cand_empirical_shrink(df):
    mu = 0.7 * df["mu"].to_numpy(float) + 0.3 * 10.0
    sd = np.maximum(df["t_sd"].to_numpy(float), 1.0)
    line = df["line"].to_numpy(float)
    z = (line + 0.5 - mu) / (sd * np.sqrt(2))
    from scipy.special import erfc
    return 0.5 * erfc(z)


CANDIDATES = {
    "normal_tsd": cand_normal,
    "logistic_simple": cand_logistic_simple,
    "poisson": cand_poisson,
    "empirical_shrink": cand_empirical_shrink,
}


def brier(p, y):
    return float(np.mean((p - y) ** 2))


def ece_equal_mass(p, y, n_bins=15):
    order = np.argsort(p, kind="mergesort")
    ps, ys = p[order], y[order]
    out, used = 0.0, 0
    for chunk in np.array_split(np.arange(len(p)), n_bins):
        if len(chunk) < 2:
            continue
        out += abs(ys[chunk].mean() - ps[chunk].mean())
        used += 1
    return float(out / max(used, 1))


def calib_slope(p, y):
    z = np.log(np.clip(p, 1e-6, 1 - 1e-6) / (1 - np.clip(p, 1e-6, 1 - 1e-6)))
    a, bb = 0.0, 1.0
    lr = 0.01
    for _ in range(2000):
        logits = a + bb * z
        preds = 1.0 / (1.0 + np.exp(-logits))
        da = float(np.mean(preds - y))
        db = float(np.mean((preds - y) * z))
        a -= lr * da
        bb -= lr * db
    return float(bb), float(a)


def monotonic_violations(preds, n_offsets=7):
    mat = np.asarray(preds).reshape(-1, n_offsets)
    return float(np.mean(np.any(np.diff(mat, axis=1) > 1e-9, axis=1)))


def cluster_bootstrap_delta(delta_se, clusters, n_boot=2000, seed=20261029):
    codes, uniq = pd.factorize(clusters)
    n_cl = len(uniq)
    sums = np.bincount(codes, weights=delta_se, minlength=n_cl)
    counts = np.bincount(codes, minlength=n_cl).astype(float)
    rng = np.random.default_rng(seed)
    p = np.full(n_cl, 1.0 / n_cl)
    out = np.empty(n_boot)
    for i in range(n_boot):
        w = rng.multinomial(n_cl, p).astype(float)
        out[i] = (w * sums).sum() / (w * counts).sum()
    return out


def main():
    df = pd.read_parquet(DATA)
    fit = df[df["season_year"] <= "2022-23"].reset_index(drop=True)
    hold = df[df["season_year"].isin(["2023-24", "2024-25", "2025-26"])].reset_index(drop=True)

    # champion table
    KMAX, NB, ALPHA, BUCKET = 60, 121, 0.5, 0.5
    mu_fit = fit["mu"].to_numpy(float); pts_fit = fit["pts"].to_numpy(float)
    b = np.clip(np.floor(mu_fit / BUCKET).astype(int), 0, NB - 1)
    k = np.clip(pts_fit.astype(int), 0, KMAX)
    c = np.bincount(b * (KMAX + 1) + k, minlength=NB * (KMAX + 1)).reshape(NB, KMAX + 1).astype(float)
    h = (c + ALPHA) / (c.sum(1, keepdims=True) + ALPHA * (KMAX + 1))
    pop = c.sum(1) > 0
    for i in range(1, NB):
        if not pop[i] and pop[i - 1]:
            h[i] = h[i - 1]; pop[i] = True
    for i in range(NB - 2, -1, -1):
        if not pop[i] and pop[i + 1]:
            h[i] = h[i + 1]; pop[i] = True
    cdf = np.clip(np.cumsum(h, 1), 0, 1)

    def champ_over(mu_arr, line_arr):
        bb = np.clip(mu_arr / BUCKET, 0, NB - 1 - 1e-9)
        b0 = np.floor(bb).astype(int); b1 = np.minimum(b0 + 1, NB - 1); w = bb - b0
        kk = np.clip(np.floor(line_arr).astype(int), 0, KMAX)
        cc = (1 - w) * cdf[b0, kk] + w * cdf[b1, kk]
        cc = np.where(np.floor(line_arr) < 0, 0.0, cc)
        cc = np.where(np.floor(line_arr) >= KMAX, 1.0, cc)
        return np.clip(1.0 - cc, 1e-6, 1 - 1e-6)

    # expand holdout into grid
    n = len(hold); m = len(DELTAS)
    mu = hold["mu"].to_numpy(float)
    base = np.floor(mu) + 0.5
    line = np.repeat(base, m) + np.tile(DELTAS.astype(float), n)
    pts = np.repeat(hold["pts"].to_numpy(float), m)
    yh = (pts > line).astype(float)
    gdf = hold.loc[hold.index.repeat(m)].reset_index(drop=True)
    gdf["line"] = line
    gdf["delta"] = np.tile(DELTAS, n)
    p_champ = champ_over(np.repeat(mu, m), line)

    clusters = gdf["game_id"].to_numpy() if "game_id" in gdf else np.arange(len(gdf))

    print("=" * 95)
    print(f"{'candidate':20s} {'Brier':>9s} {'logloss':>9s} {'ECE':>8s} {'slope':>7s} "
          f"{'viol':>7s} {'dBrier':>9s} {'CI hi':>9s} {'promo':>6s}")
    print("-" * 95)

    rows = []
    champ_res = {
        "name": "champion_table",
        "brier": brier(p_champ, yh),
        "logloss": float(-np.mean(yh * np.log(p_champ) + (1 - yh) * np.log(1 - p_champ))),
        "ece": ece_equal_mass(p_champ, yh),
        "slope": calib_slope(p_champ, yh)[0],
        "intercept": calib_slope(p_champ, yh)[1],
        "mean_p": float(p_champ.mean()),
        "obs": float(yh.mean()),
        "violations": monotonic_violations(p_champ),
        "delta_brier": 0.0, "ci_lo": 0.0, "ci_hi": 0.0, "promotable": False,
    }
    rows.append(champ_res)
    print(f"{'champion_table':20s} {champ_res['brier']:9.5f} {champ_res['logloss']:9.5f} "
          f"{champ_res['ece']:8.5f} {champ_res['slope']:7.3f} {champ_res['violations']*100:6.2f}% "
          f"{0:+9.5f} {0:+9.5f} {'ref':>6s}")

    for name, fn in CANDIDATES.items():
        preds = np.clip(fn(gdf), 1e-6, 1 - 1e-6)
        slope, intercept = calib_slope(preds, yh)
        res = {
            "name": name,
            "brier": brier(preds, yh),
            "logloss": float(-np.mean(yh * np.log(preds) + (1 - yh) * np.log(1 - preds))),
            "ece": ece_equal_mass(preds, yh),
            "slope": slope,
            "intercept": intercept,
            "mean_p": float(preds.mean()),
            "obs": float(yh.mean()),
            "violations": monotonic_violations(preds),
        }
        delta_se = (preds - yh) ** 2 - (p_champ - yh) ** 2
        draws = cluster_bootstrap_delta(delta_se, clusters, n_boot=2000)
        res["delta_brier"] = float(draws.mean())
        res["ci_lo"] = float(np.percentile(draws, 2.5))
        res["ci_hi"] = float(np.percentile(draws, 97.5))
        res["promotable"] = (
            res["delta_brier"] < 0 and res["ci_hi"] < 0 and res["ece"] <= 0.010
            and 0.90 <= res["slope"] <= 1.10
        )
        rows.append(res)
        print(f"{res['name']:20s} {res['brier']:9.5f} {res['logloss']:9.5f} {res['ece']:8.5f} "
              f"{res['slope']:7.3f} {res['violations']*100:6.2f}% {res['delta_brier']:+9.5f} "
              f"{res['ci_hi']:+9.5f} {'YES' if res['promotable'] else 'no':>6s}")

    (OUT / "formula_comparison.json").write_text(json.dumps(rows, indent=2), encoding="utf-8")
    print(f"\nsaved {OUT / 'formula_comparison.json'}")
    print("\nPromotion requires delta_brier<0, ci_hi<0, ece<=0.010, slope in [0.9,1.1],")
    print("and improvement in >=2 of 3 seasons. The champion table and GBM numbers are")
    print("the bars: champion pooled Brier 0.2263, GBM 0.2248, champion ECE 0.0138.")


if __name__ == "__main__":
    main()
