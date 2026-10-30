"""Dry-run the DeepTest notebook script locally against a synthetic parquet.

The Kaggle kernel is generated from Kotlin source, so a Python error there only shows up
after a full upload. This extracts the same script, points its glob at a throwaway folder
and runs it for every factor and both sides, so the grammar and array logic are checked
before the app spends a kernel slot.
"""
import json
import os
import re
import subprocess
import sys
import tempfile

import numpy as np
import pandas as pd

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "PropsLab", "PropsLab", "app", "src", "main", "java", "com", "propslab", "claim", "DeepTest.kt")


def extract_script():
    body = open(SRC, encoding="utf-8").read()
    start = body.index('private val SCRIPT = """') + len('private val SCRIPT = """')
    end = body.index('""".trimIndent()', start)
    return body[start:end]


def training_frame(rows=20_000, seed=7):
    rng = np.random.default_rng(seed)
    mu = rng.normal(18, 5, rows)
    # Give every factor a real effect so the replay has something to bet on, the way a
    # genuine claim would. A null factor here would only prove the harness runs.
    opp = (110 + rng.normal(0, 4, rows)).round(2)
    b2b = rng.integers(0, 2, rows)
    rest = rng.integers(0, 6, rows)
    home = rng.integers(0, 2, rows)
    pace = (100 + rng.normal(0, 3, rows)).round(2)
    tmin = (32 + rng.normal(0, 4, rows)).round(1)
    pts = (
        mu
        + 0.45 * (opp - 110)
        - 2.0 * b2b
        + 0.30 * rest
        + 1.5 * home
        + 0.35 * (pace - 100)
        + 0.20 * (tmin - 32)
        + rng.normal(0, 6, rows)
    )
    return pd.DataFrame(
        {
            "game_id": np.repeat(np.arange(rows // 10), 10),
            "player_id": np.tile(np.arange(10), rows // 10),
            "game_date": pd.date_range("2023-10-24", periods=rows // 10, freq="D").repeat(10),
            "pts": np.maximum(pts, 0).round(1),
            "mu": mu.round(2),
            "t_sd": rng.uniform(4, 9, rows).round(2),
            "opp_def100_trail": opp,
            "b2b": b2b,
            "rest_days": rest,
            "is_home": home,
            "own_pace_trail": pace,
            "t_min": tmin,
        }
    )


def main():
    script = extract_script()
    work = tempfile.mkdtemp(prefix="deepdry-")
    inp = os.path.join(work, "input")
    os.makedirs(inp)
    training_frame().to_parquet(os.path.join(inp, "propslab_training.parquet"))
    script = script.replace("/kaggle/input/", inp.replace("\\", "/") + "/")
    asserts = [
        "LENS = json.loads('__LENS_JSON__')",
        "ft['expected'] =",
        "def replay_at(bar):",
    ]
    for needle in asserts:
        if needle not in script:
            sys.exit(f"generated script no longer contains {needle!r}; update this harness")

    failures = 0
    for factor in ("defense", "fatigue", "rest", "home", "pace", "minutes", "unknown"):
        for side in ("BOTH", "OVER_ONLY", "UNDER_ONLY"):
            lens = {"claim": f"{factor} claim", "factor": factor, "side": side, "min_edge": 0.5, "tilt": 0.0}
            body = script.replace("__LENS_JSON__", json.dumps(lens))
            with open(os.path.join(inp, "run.py"), "w", encoding="utf-8") as fh:
                fh.write(body)
            out = subprocess.run(
                [sys.executable, "run.py"], cwd=inp, capture_output=True, text=True, timeout=600,
            )
            tag = f"{factor:8s} {side:10s}"
            if out.returncode != 0:
                failures += 1
                print(f"FAIL {tag} {out.stderr.strip().splitlines()[-1] if out.stderr.strip() else ''}")
                continue
            result = json.load(open(os.path.join(inp, "claim_result.json")))
            rep = result["replay"]
            print(
                f"ok   {tag} verdict={result['verdict']:8s} rows={result['rows']} "
                f"bets={rep['bets']} roi={rep['roi']:+.3f} t={rep['t']:+.2f} "
                f"hold={rep['holdout_roi']}"
            )
    print(f"\n{21 - failures}/21 combinations ran")
    if failures:
        sys.exit(1)

    # A replay that only ever bets one side silently turns every over claim into a
    # no-bet, so check both sides actually fire on a factor that does move points.
    for side in ("OVER_ONLY", "UNDER_ONLY"):
        lens = {"claim": "side probe", "factor": "defense", "side": side, "min_edge": 0.5, "tilt": 0.0}
        body = script.replace("__LENS_JSON__", json.dumps(lens))
        with open(os.path.join(inp, "run.py"), "w", encoding="utf-8") as fh:
            fh.write(body)
        subprocess.run([sys.executable, "run.py"], cwd=inp, check=True, capture_output=True)
        bets = json.load(open(os.path.join(inp, "claim_result.json")))["replay"]["bets"]
        if bets == 0:
            sys.exit(f"FAIL: {side} never placed a bet, so that side of the lens is dead")
    print("side probe: both over and under fire")


if __name__ == "__main__":
    main()
