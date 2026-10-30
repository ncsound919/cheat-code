# Kaggle training pipeline

Everything needed to rebuild and re-run the PropsLab challenger comparison.

```
research/kaggle/
  build_dataset.py        # SQLite -> leakage-free parquet
  validate_dataset.py     # dataset gates (must pass before upload)
  dataset/                # what gets uploaded
    dataset-metadata.json
    propslab_training.parquet
    propslab_training_sample.csv
    schema.json
    manifest.json
  kernel/
    train.py              # the run itself
    kernel-metadata.json
```

## Artifacts

| | |
|---|---|
| Dataset | `tap919/propslab-nba-prop-training` (private) |
| Parquet SHA-256 | `962376828535ff49e58b92b4b98cd7c687eabc21326ad68a4ffc66deefba8dbc` |
| Kernel | `tap919/propslab-challenger-table-vs-constrained-gbm` |
| Criteria | `research/CRITERIA.md` (frozen before the run) |
| Method notes | `research/BEST_PRACTICES.md` |
| Run artifacts | `research/kaggle/runs/2026-10-29/` |

## Result (kernel v3, 2026-10-29)

**Verdict: PROMOTE the constrained GBM.** ΔBrier −0.001520 vs the champion table,
95% CI [−0.001769, −0.001279] game-clustered and [−0.001789, −0.001249]
player-clustered, ECE 0.00570, slope 0.986, all three holdout seasons improved.
All gates passed. Full write-up in `PROPSLAB-ECOSYSTEM-PLAN.md` §9.

Two things the run settled that were not obvious beforehand:

- The champion's level bias (+1.37 pp on the holdout) is roughly the size of the
  improvement, so the win is mostly bias correction, not new structure.
- Constraining `line` alone is insufficient. `delta` must also be constrained, or
  the model undoes the `line` constraint along the direction the data moves. The
  smoke test caught this (0.640% violations) before the real run; with `delta`
  constrained the rate is 0.000%.

## Rebuild

```powershell
cd research/kaggle
python build_dataset.py        # ~2 min, reads 1.2 GB SQLite
python validate_dataset.py     # gates; must print ALL GATES PASSED
```

## Re-upload

Only if the extract changes. The kernel pins the dataset by ref, so a new version
silently changes what the kernel reads — bump `EXPECTED_SHA` in `train.py` in the
same commit.

```powershell
cd research/kaggle
kaggle datasets version -p .\dataset -m "describe the change" -t
```

## Local smoke test

Runs the full pipeline on a subsample so failures surface in seconds, not in a
queued GPU session. It caught a real modelling bug: with `line` constrained but
`delta` free, the GBM undid the constraint along the direction the data moves.

```powershell
cd research/kaggle/kernel
$env:PROPSLAB_WORK="C:\Temp\smoke"
$env:PROPSLAB_INPUT="..\dataset"
$env:PROPSLAB_NBOOT="60"
$env:PROPSLAB_SPLITS="2"
$env:PROPSLAB_SUBSAMPLE="20000"
python train.py
```

Environment overrides: `PROPSLAB_WORK`, `PROPSLAB_INPUT`, `PROPSLAB_NBOOT`,
`PROPSLAB_SPLITS`, `PROPSLAB_SUBSAMPLE`.

## Run on Kaggle

```powershell
cd research/kaggle/kernel
kaggle kernels push -p .
kaggle kernels status tap919/propslab-challenger-table-vs-constrained-gbm
kaggle kernels output tap919/propslab-challenger-table-vs-constrained-gbm -p .\output
```

## Outputs

| File | Contents |
|---|---|
| `results.json` | every metric, per δ, per season, gate results, environment |
| `bootstrap.csv` | 10,000 ΔBrier draws, game- and player-clustered |
| `per_delta.csv` | Brier / log loss / ECE at each line offset |
| `reliability.csv` | equal-mass reliability bins |
| `champion_table.json` | the exportable CDF table for the Kotlin scorer |
| `gbm_bundle.json` | the promoted GBM: 4 folds x 200 trees + 4 isotonic maps (3.69 MB) |
| `report.md` | verdict against the promotion rule |
| `environment.json` | versions, seeds, row counts, runtime |

## Notes

- **Mount layout changed.** Kaggle now attaches datasets under
  `/kaggle/input/datasets/<owner>/<slug>/`, not `/kaggle/input/<slug>/`. The kernel
  resolves the path by locating the parquet rather than assuming it, so both layouts
  work. This cost one failed run (v1) before it was found — see the log at
  `kernel/output/`.
- **Sanity gates must compare like with like.** v2 failed a correct run because the
  champion gate compared a pooled-across-δ Brier (0.2263) to a δ = 0 reference
  (0.2486). The gate now reads the δ = 0 slice. Recorded as amendment §10b-i in
  `CRITERIA.md`, with the local reconciliation that proved the harness was right and
  the gate was wrong. Gate failures now also write `failure.json` so a failed run
  still leaves its diagnostics behind.
- `enable_gpu` is **false** on purpose. The model is 12 features and 4M grid rows;
  gradient boosting on 4 CPU cores finishes in minutes. GPU would burn quota for
  no gain.
- The kernel has no internet and no secrets. It reads one pinned dataset and writes
  files.
- Seeds are fixed (`20261029`). Re-running should reproduce the numbers exactly on
  the same sklearn version; `environment.json` records the version.
