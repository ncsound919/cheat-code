# Math X ingestion — PropsLab

**Decision 2026-10-29:** take Math X's formula-development layer into the app, keep
the math deterministic, and allow only an on-device model at the LLM boundary. No
Anthropic client, no Express server, and no network call anywhere in the model path.
(The manifest's `INTERNET` permission exists for the read-only odds feed alone.)

Source: [ncsound919/math-x](https://github.com/ncsound919/math-x), MIT per the
project README (no LICENSE file was present when the prompt layer was ported). The
notice is vendored at
[`MATHX-LICENSE.txt`](../PropsLab/PropsLab/MATHX-LICENSE.txt). Math X is a
full-stack system (Express API + React workbench + Pyodide/SymPy + Anthropic/DeepSeek/
Qwen routing). PropsLab is an offline Android app. Ingesting the whole stack would
add a server, a network dependency and a model key to an app whose manifest
deliberately has no network permission. The useful part is narrower.

## What was ingested

| Math X piece | PropsLab implementation |
|---|---|
| `MATHX_SYSTEM` and `MODE_PREFIXES` for `formula`, `probability`, `solve` | [`MathXPrompts.kt`](../PropsLab/PropsLab/app/src/main/java/com/propslab/mathx/MathXPrompts.kt) — mode strings kept verbatim; system adapted and a guard appended |
| Formula Lab: derivation chain plus independent verification | [`FormulaLab.kt`](../PropsLab/PropsLab/app/src/main/java/com/propslab/mathx/FormulaLab.kt) — 4 formulas, 15 executable checks |
| "Formula → Qwen, probability → statistician" routing | One on-device model behind [`LocalMathModel`](../PropsLab/PropsLab/app/src/main/java/com/propslab/mathx/LocalMathModel.kt); no provider routing and no cloud fallback |
| Verification badges / trust score | Lab screen shows `passed/total` per formula and overall |
| SymPy step verification | [SymPy reference](mathx/verify_formulas.py) on the research side; the app uses native Kotlin checks, never evaluated expressions |
| Workbench UI | [Lab tab](../PropsLab/PropsLab/app/src/main/java/com/propslab/ui/screens/LabScreen.kt) |

The prompt port uses the same mode prefixes as Math X's `buildPrompt`: the prefix is
prepended to the last user message. The system prompt is shortened and its LaTeX
dollar delimiters removed; the appended `PROPSLAB GUARD` forbids the model from
originating a probability or giving betting advice.

## What was deliberately not ingested

- Express API, React frontend, DuckDB, RAG, bioinformatics, OCR, export panel.
- Anthropic/DeepSeek/Qwen provider routing and `ANTHROPIC_API_KEY`.
- Pyodide/SymPy in the app. Running Python/WASM to verify a formula would add tens of
  megabytes and an execution surface for text a model produced.
- Any network call in the model path. Inference and formula checks never leave the phone.

Math X's server routes are also documented as broken in the ecosystem plan
(`PROPSLAB-ECOSYSTEM-PLAN.md §6`): `/api/verify`, `/api/plan`, `/api/codegen` and
`/api/chat` construct an Anthropic client at module load and the deployed chat lane
returns no tokens. Depending on that from the phone would fail offline and at runtime.

## The local-model boundary

`LocalMathModel` is the only way text generation enters PropsLab:

```kotlin
interface LocalMathModel {
    val displayName: String
    fun status(): LocalModelStatus
    suspend fun complete(prompt: String, system: String, maxTokens: Int): String
}
```

`UninstalledLocalModel` is the shipped implementation. It reports where weights
belong and refuses to generate. `MathXAssistant` therefore falls back to the
deterministic `TemplateAssistant`, so first launch and fixture seeding work today.

When weights are provisioned:

1. Choose the runtime: **llama.cpp** (MIT, GGUF) or **LiteRT-LM** (Apache-2.0).
2. Implement `LocalMathModel` around it. Load the model once, off the main thread.
3. Put the weights at `filesDir/models/qwen3-1.7b-q4_k_m.gguf` (or change the path in
   `PropsLabApp`). Do not add a downloader: the app's one network path is the read-only feed.
4. Pass the implementation into `PropsLabApp` instead of `UninstalledLocalModel`.

Recommended weights: **Qwen3-1.7B, Q4_K_M, Apache-2.0**, about 1.1 GB. Qwen2.5-Math
is the thematic match but is larger and its licence must be checked for the exact
weights shipped. Gemma weights are not Apache-2.0; the Gemma Terms apply. Check the
weights, not just the runtime.

`MathXAssistant` wraps every call in a 6-second timeout and a 192-token cap, so a
slow model cannot block Room seeding or a screen. It also validates the reply:

- Any percentage that is not within 0.06 points of a supplied value rejects the whole
  reply and falls back to the template.
- Replies over 1,200 characters are rejected.
- No model reply is ever parsed or executed as code.

This is a whitelist, not a truth check. It guarantees provenance of numbers, not
correctness of prose.

## Formula catalog

The Lab tab ships four formulas and 15 checks:

| Formula | Mode | Checks |
|---|---|---|
| No-vig price and hold | formula | sum to one, −110 hold, edge antisymmetry, fair-price round trip |
| Expected value with push mass | probability | outcome sum, zero at fair, push raises price, stake scaling |
| Kelly with push mass | probability | zero at fair, numeric growth maximum, negative below fair, push denominator |
| Platt calibration on the logit | probability | identity, bounded output, monotone order |

The checks call the same `Pricing`, `Staking` and `Odds` functions the app uses. The
independent reference is `research/mathx/verify_formulas.py`, which derives the same
results in SymPy and writes
[`research/derivations/propslab-pricing.md`](derivations/propslab-pricing.md).

Current results: **16/16 SymPy checks pass**, and the JVM suite is green.

## Run it

```bash
python research/mathx/verify_formulas.py
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

## Open work

1. Implement the native runtime (`LlamaCppMathModel` or `LiteRtMathModel`) and measure
   load time, tokens/second, memory and thermals on the S25 Ultra.
2. Exercise `MathXAssistant` with real model output; the guard is tested with fakes,
   not a real quantised model.
3. Add a local-file import (SAF) if the APK should not carry weights.
4. Add instrumented UI tests for the Lab tab; the model card's "not installed" path is
   the only state that exists today.
