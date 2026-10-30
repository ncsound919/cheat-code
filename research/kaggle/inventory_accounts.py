"""Inventory the three Kaggle accounts: kernels (runs), datasets, models, GPU quota.

Tokens are read from the environment, never from a file in this repository:

    KAGGLE_TOKEN_DJOOCHIE=KGAT_…
    KAGGLE_TOKEN_TERENCEPERRY=KGAT_…
    KAGGLE_TOKEN_TAP919=KGAT_…
    python research/kaggle/inventory_accounts.py

Read-only: it calls `kaggle kernels list -m`, `kaggle datasets list -m`,
`kaggle models list --owner` and `kaggle quota`. Findings from the 2026-10-29 run
are written up in research/KAGGLE-ACCOUNTS.md.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys

ACCOUNTS = [
    ("djoochie", "KAGGLE_TOKEN_DJOOCHIE"),
    ("terrenceperry", "KAGGLE_TOKEN_TERENCEPERRY"),
    ("tap919", "KAGGLE_TOKEN_TAP919"),
]


def run(args: list[str], token: str) -> str:
    env = dict(os.environ, KAGGLE_API_TOKEN=token)
    proc = subprocess.run(
        args, capture_output=True, text=True, encoding="utf8", errors="replace",
        env=env, timeout=120,
    )
    return proc.stdout


def parse_json(text: str):
    index = text.find("[")
    if index < 0:
        index = text.find("{")
    if index < 0:
        return None
    try:
        return json.loads(text[index:])
    except json.JSONDecodeError:
        return None


def main() -> int:
    missing = [name for _, name in ACCOUNTS if not os.environ.get(name)]
    if missing:
        print("Set these environment variables first: " + ", ".join(missing), file=sys.stderr)
        return 2

    for username, var in ACCOUNTS:
        token = os.environ[var]
        print("=" * 72)
        print(username)

        kernels = parse_json(run(
            ["kaggle", "kernels", "list", "-m", "--format", "json", "--page-size", "200"], token)) or []
        print(f"  kernels: {len(kernels)}")
        for kernel in sorted(kernels, key=lambda k: k.get("lastRunTime") or "", reverse=True)[:8]:
            print(f"    {kernel.get('lastRunTime')}  {kernel.get('ref')}")

        datasets = parse_json(run(
            ["kaggle", "datasets", "list", "-m", "--format", "json", "--page-size", "200"], token)) or []
        print(f"  datasets: {len(datasets)}")
        for dataset in datasets[:8]:
            print(f"    {dataset.get('ref')}  updated={dataset.get('lastUpdated')}")

        models = parse_json(run(
            ["kaggle", "models", "list", "--owner", username, "--format", "json", "--page-size", "200"], token))
        items = models if isinstance(models, list) else (models or {}).get("models", [])
        print(f"  published models: {len(items)}")
        for model in items:
            print(f"    {model.get('ref')}")

        quota = run(["kaggle", "quota"], token).splitlines()
        for line in quota[:4]:
            print(f"    {line}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
