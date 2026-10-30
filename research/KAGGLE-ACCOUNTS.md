# Kaggle accounts — inventory of runs, datasets and models

Queried 2026-10-29 against `api.kaggle.com/v1` with the Kaggle CLI 2.2.5, using the
three `KGAT_…` tokens from `D:\docs\opencode api.txt` via `KAGGLE_API_TOKEN`.

**No token is stored in this repository.** `research/kaggle/inventory_accounts.py`
reads them from environment variables.

```bash
export KAGGLE_TOKEN_DJOOCHIE=KGAT_…
export KAGGLE_TOKEN_TERENCEPERRY=KGAT_…
export KAGGLE_TOKEN_TAP919=KGAT_…
python research/kaggle/inventory_accounts.py
```

Auth note: the new `KGAT_` tokens work both as `Authorization: Bearer …` on the raw
REST API and as `KAGGLE_API_TOKEN` for the CLI. The legacy `kaggle.json`
username/key pair is no longer required.

## 1. `djoochie` — "DJ Oochie" (token labelled *Kaggle 1: tap4500*)

| | |
| --- | --- |
| Kernels | **100**, most recent run 2026-10-25 11:41 UTC |
| Datasets | **18** |
| Published models | **0** |
| GPU quota | 0.00 h of 30.00 h used (refreshes 2026-10-31) |
| TPU quota | 0.00 h of 20.00 h |

Themes: `onco-*` (oncology: csf-ctdna, gse45827, serena6, lesion, contamination, bias
canary, conformal/federated/concept trials), `eco-recourse-*`, `cellular-map-test-v11…v15`,
`recourse-repair-diagnose`, `gpuprobe-6867181`.

## 2. `terrenceperry` — "Terrence Perry" (token labelled *Kaggle 2: tap919beats*)

| | |
| --- | --- |
| Kernels | **4**, all on 2026-10-28 |
| Datasets | **0** |
| Published models | **0** |
| GPU quota | 5.71 h of 30.00 h used |

These four are the Qwen LoRA training runs:

| Kernel | Last run (UTC) |
| --- | --- |
| `terrenceperry/qwen-science-oncology` | 2026-10-28 16:46 |
| `terrenceperry/qwen-mix-full-train` | 2026-10-28 16:09 |
| `terrenceperry/qwen-coding-mobile` | 2026-10-28 16:05 |
| `terrenceperry/qwen-nc-studio-mixing` | 2026-10-28 07:30 |

`kaggle kernels files terrenceperry/qwen-mix-full-train` lists PEFT/unsloth training
output — `adapter_config.json`, `adapter_model.safetensors`, `chat_template.jinja`,
`trainer_state.json`, `optimizer.pt`, `scheduler.pt`, `rng_state.pth`, `tokenizer.json`,
`mixparams.parquet` — and the paging cursor decodes to a real checkpoint path:
`output/qwen_mix_adapter/checkpoint-1164/adapter_model.safetensors`.

## 3. `tap919` — "Tap919" (token labelled *Kaggle 3: ncsound919*)

| | |
| --- | --- |
| Kernels | **70**, most recent run 2026-10-29 03:58 UTC |
| Datasets | **20** |
| Published models | **0** |
| GPU quota | **16.63 h of 30.00 h used, 13.37 h left** (refreshes 2026-10-31) |
| TPU quota | 0.00 h of 20.00 h |

Runs from the last 24 hours:

| Kernel | Last run (UTC) |
| --- | --- |
| `tap919/propslab-challenger-table-vs-constrained-gbm` | 2026-10-29 03:58 |
| `tap919/concordance-fixture-run` | 2026-10-29 03:03 |
| `tap919/new-benchmark-task-3eb54` | 2026-10-29 01:58 |
| `tap919/concordance-gpu-check` | 2026-10-29 01:48 |
| `tap919/concordance-null-1000` | 2026-10-29 01:48 |
| `tap919/concordance-smoke-cpu` | 2026-10-29 01:48 |
| `tap919/jev-common-compare` | 2026-10-29 01:41 |
| `tap919/qwen-base-vs-adapters` | 2026-10-28 23:29 |
| `tap919/jev-modernbert-train-v3` | 2026-10-28 21:49 |
| `tap919/qwen-full-model-compare` | 2026-10-28 20:02 |
| `tap919/jev-modernbert-train-v2` | 2026-10-28 19:39 |
| `tap919/jev-modernbert-full-train-v1` | 2026-10-28 19:35 |

Datasets include `tap919/propslab-nba-prop-training` (updated 2026-10-29 02:14):

| File | Bytes |
| --- | --- |
| `propslab_training.parquet` | 18,118,646 |
| `propslab_training_sample.csv` | 1,722,004 |
| `schema.json` | 1,251 |
| `manifest.json` | 692 |

plus the `onco-*` family mirrored from the first account.

## What this confirms, and what it does not

**Confirmed: the training is real.** Six Qwen training kernels across two accounts,
GPU hours actually consumed (5.71 h + 16.63 h in the current quota window), and real
PEFT adapter artifacts in the kernel output including checkpointed
`adapter_model.safetensors` files.

**Not confirmed: the published-model surface.** All three accounts have **zero**
models on Kaggle Models, so the four LoRA trees are not published there — they exist
in kernel output and in the local `Pulse Daw/kaggle_qwen_*` trees you described
(0.24–1.96 GB each). Those local paths were not read: only a partial path was given
and it sits outside this workspace. Give the full path and they can be hashed,
size-checked and recorded.

**Measurement caveat.** `kaggle kernels files` returns a `size` field that is not a
byte count for kernel output — every value lands between 866 and 989, including a
`tokenizer.json` that must be megabytes. The same endpoint on a dataset reports true
bytes (see the 18.1 MB parquet above), so this is specific to output files. Per-checkpoint
adapter sizes therefore cannot be read from the listing; use the local trees or download
the output to measure them.

## On `KaggleComputeEngine`

`KaggleComputeEngine` is not in this workspace — a search of the PropsLab tree found no
match, and the only Kaggle code here is the real training pipeline under
`research/kaggle/` (dataset build, kernel, run logs). If that class in the Agent Support
app only writes a Room row, it is a stub, and this report is the shape of the real
client: bearer token → `kernels list -m`, `datasets list -m`, `models list --owner`,
`quota`, all read-only.
