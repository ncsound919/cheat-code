# OSS tooling for PropsLab — evaluated 2026-10-29

Scope: what open-source tooling is actually worth adding to the Android app and
the formula bench, given the toolchain that is in the tree. Every recommendation
below is constrained by the existing build, not by what is newest.

## The constraint that decides most of this

| | |
|---|---|
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.01 (Compose 1.7.x) |
| AGP / Gradle | 8.7.3 / Gradle 8.13 |
| minSdk / compileSdk | 29 / 35 |
| Permissions | `INTERNET`, for the read-only feed only |
| Data | Room, seeded locally |

Several 2026 releases have moved past this generation. Adding them means either a
coordinated toolchain upgrade or a compile failure, so they are listed as *defer*,
not *adopt*. No runtime dependency was added in this pass.

## UI and performance

| Tool | License | Version seen | Verdict for this tree |
|---|---|---:|---|
| Hand-rolled Compose `Canvas` | AndroidX | resolved 1.7.x | **Keep.** The split ring, split bar and portfolio sparkline are tiny, fixed-size, non-interactive drawings. A chart library buys nothing here and adds an upgrade constraint. Add semantics for screen readers instead (done). |
| [Vico](https://github.com/patrykandpatrick/vico) | Apache-2.0 | 3.3.1 | **Defer.** Compose-first and well maintained, but 3.3.1 publishes against Kotlin 2.4.10 / Compose 1.12. Do not mix it into Kotlin 2.0.21. Revisit only for a real, interactive chart after a toolchain upgrade. |
| [Paparazzi](https://github.com/cashapp/paparazzi) | Apache-2.0 | 1.3.5 stable | **Adopt next, on 1.3.5 only.** Its changelog targets Kotlin 2.0.21 / Compose 1.7.5 — the closest match to this project. Renders composables on the host JVM, no emulator. The 2.0 alpha line needs JDK 21 and re-records; avoid it. |
| [Roborazzi](https://github.com/takahirom/roborazzi) | Apache-2.0 | 1.77.0 | **Later.** Use when screenshots need an Activity or interaction, via Robolectric. More moving parts than Paparazzi; adopt after component goldens exist. |
| Compose Preview screenshot testing | Android screenshot engine | 0.0.1-alpha16 | **Do not adopt.** The maintained path needs AGP 9.5 alpha and the old standalone plugin is retired. Wrong direction for a stable project. |
| [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview) + Macrobenchmark | Apache-2.0 | Macrobenchmark 1.5.0, ProfileInstaller 1.4.1 | **Adopt when a physical device is in the loop.** Highest measurable win for cold start on the S25 Ultra. Requires a benchmark module and a device run; do not claim a number without measuring. |
| Material 3 `PullToRefreshBox` | Apache-2.0 | resolved by BOM | **Skip.** There is no network refresh. Room emits changes immediately; a spinner would be theatre. |
| compose-placeholder | Apache-2.0 | 1.0.12 | **Skip.** Latest release targets Kotlin 2.2.10 / CMP 1.9. The Slate should show cached rows, not a skeleton. |
| Compose accessibility checks (`ui-test-junit4-accessibility`) | Apache-2.0 | Compose 1.12.1 | **Blocked.** The checks start at Compose 1.8; this BOM is 1.7.x. Manual TalkBack until the upgrade. |
| Built-in `LocalHapticFeedback` | AndroidX | resolved by Compose | **Optional.** Use it for paper-bet confirmation if desired; no extra library. |

## Testing the formulas

| Tool | Version seen | Verdict |
|---|---:|---|
| JUnit 4 + table-driven sweeps | 4.13.2 (already present) | **Chosen.** 22 JVM tests now cover odds, hold, edge, fair price, EV, Kelly, probability validation and gate boundaries. No runner migration. |
| [Kotest](https://kotest.io) property testing | 6.2.6 | **Defer.** Kotest 6 requires Kotlin 2.2+, so it cannot be added to this module as-is. Kotest 5 works but pulls the JUnit Platform into an Android local-test setup for little gain over explicit grids and shrinking-by-hand. |
| [jqwik](https://jqwik.net) | 1.10.2 | **Avoid.** Needs JUnit Platform 1.14.4 and ships a conspicuous anti-AI runtime banner. Not appropriate for this build. |
| JUnit Jupiter parameterized | 5.14.4 / 6.1.3 | **Not worth it.** Android local tests are still JUnit 4-first; JUnit 6 needs Java 17 and an integration plugin. |
| [pytest](https://pytest.org) + [Hypothesis](https://hypothesis.readthedocs.io) | 9.2.0 / 6.170.13 | **Adopt if the bench grows.** Generate adversarial price/probability cases in Python, then commit the interesting ones as Kotlin vectors. Do not duplicate the Kotlin algorithm in Python and call it an oracle. |
| [pandera](https://pandera.readthedocs.io) | 0.35.0 | **Optional.** Useful if the bench validates DataFrames; unnecessary for the current scalar/array script. |
| Great Expectations | 1.24.1 | **No.** Pipeline machinery for a one-file bench. |

Golden files are regression baselines, not proof of correctness. The Python bench
(`research/formula_bench.py`) is the independent reference whenever a number moves:
it reproduced the plan's values exactly on 2026-10-29 (mean hold 3.745%, Brier
0.202329, ECE 0.005610, Kelly closed-form max error 2e-6).

## On-device inference

The app already has a hand-written `GbmBundleScorer` tree walker. That remains the
right shape for one promoted model: no native runtime, no ABI packaging, and the
JSON is already exported by `research/kaggle/export_gbm.py`.

- **Keep the walker, but make parity a gate.** sklearn's `HistGradientBoosting`
  routes missing values, compares `x <= threshold`, sums fold baselines and applies
  isotonic interpolation; all four must be tested against Python, including values
  exactly on a split and NaN inputs.
- **ONNX Runtime Android 1.31.1** (MIT) is the maintained alternative if the app
  grows to several models or complex preprocessing. Cost: a native AAR (about
  24 MB at 1.18, less with a custom build) and a conversion path via `skl2onnx`.
- **LiteRT-LM** (v0.18.1) is the strongest 2026 option for local explanations: stable
  Kotlin API, Galaxy S25 Ultra results published for a 1.6 GB Qwen2.5-1.5B model
  (roughly 34 tok/s CPU decode, 31 GPU). Init can take about 10 s, so it belongs in
  a background load with a deterministic text fallback.
- **llama.cpp** (MIT) is the alternative if GGUF portability matters more than
  Kotlin integration. It brings NDK/JNI packaging and ABI work.
- **No network path for weights** means they must be bundled or imported from local
  storage. A 1.6 GB model is a product decision, not a library choice. Qwen3-1.7B
  weights are Apache-2.0; Gemma weights are under the Gemma Terms, not the Apache-2.0
  license of the demo app. Check the weights, not just the runtime.
- Math X's prompt and formula-lab layer is already ingested behind this boundary; see
  `research/MATHX-INTEGRATION.md`. The runtime is the remaining piece, not the API
  design.
- The assistant must continue to verbalise facts the scorer produced. It must not
  recompute probabilities.

## Adoption order

1. **Paparazzi 1.3.5** — golden tests for Slate (populated, empty, edge-sorted),
   Detail (available, unavailable, push mass) and Portfolio (empty, open, settled).
   This is the cheapest regression net for the UI work now in the tree.
2. **Baseline Profile + Macrobenchmark** — measure cold start and the Slate journey
   on the S25 Ultra before/after. Report the measurement, not the vendor claim.
3. **Toolchain upgrade in a branch** (Kotlin 2.2+/Compose 1.8+/AGP 9) — then, and only
   then, Compose accessibility checks, Vico for a real chart, and newer screenshot
   tooling become available.
4. **Formula parity fixtures** — when a bundle is imported, export 50 player-games
   from the Python reference, score both sides, assert equality to 1e-9. The test
   pattern already exists in `DomainTest.kt`.
5. **Local LLM** — only after a measured size/latency decision on the target device.

## What was not verified

Exact compatibility of every future release with Kotlin 2.0.21/BOM 2024.10, actual
APK/AAR sizes for current LLM runtimes, and real cold-start gains on this handset.
Those are measurements, not assumptions; the document treats them as open work.
