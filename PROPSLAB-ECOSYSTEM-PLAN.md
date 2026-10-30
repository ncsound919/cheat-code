# PropsLab × ecosystem — research and plan

Researched 2026-10-28 against the checked-out copies of `eco/`, `phone-apps/` and
this repo. Every claim below names the file it came from. Where something is
**not** wired, it says so instead of assuming it is.

## Verdict

PropsLab can join the ecosystem, but only one of the four asks is a drop-in today:

| Ask | State | What it really takes |
| --- | --- | --- |
| Sieve telemetry | **Ready to integrate** | Copy a proven Kotlin client, add `propslab-android` to one allowlist, add a Room outbox |
| Agent memory | **Ready, with caveats** | Sieve `/recall` works through the mobile gateway today; Recourse writes need one gateway route; the on-device hub needs a `propslab` member and a foreground service |
| Recourse self-learning | **Not a fit as-is** | Recourse learns *code/tool templates*, not probabilities. Borrow the promotion-gate pattern, not the code |
| Math X | **Blocked** | Last recorded live probe returned `200` with `No output generated`. Four of its routes need an Anthropic key it does not have |

There is also one structural conflict: PropsLab's manifest deliberately has **no
INTERNET permission** (`app/src/main/AndroidManifest.xml:3`). Telemetry and
memory are network features. That has to be resolved before any of this is
built, and the resolution should not quietly break the app's stated guarantee.

And one product-level finding from the dataset audit (§9): `D:\datasets\sports-datasets`
has plenty of player-game labels but **no historical player-prop lines**. Model
calibration can be trained and evaluated; edge versus the market cannot be
backtested, because there is no market quote to compare against.

---

## 1. What PropsLab is today (verified)

- Kotlin/Compose, Room, WorkManager, `minSdk 29`, no HTTP client dependency
  (`app/build.gradle.kts:32-48`).
- `README.md:3`: "Offline, local-first, paper trading only. The manifest has no
  INTERNET permission on purpose."
- State is local: `predictions`, `paper_bets`, `settlements`, `model_versions`,
  and append-only `audit_events` (`data/Entities.kt`).
- Probabilities have exactly one source: `StatisticalScorer`
  (`inference/Scorer.kt:14-17`). The assistant only writes prose
  (`inference/LocalAssistant.kt:14-21`).
- `domain/Odds.kt` already owns the math that matters: American↔decimal,
  no-vig, push-aware settlement.
- A calibration hook exists but is frozen: `LogisticBundle` carries `plattA` /
  `plattB` (`Scorer.kt:27-28`, applied at `Scorer.kt:47`), fitted offline.
- No model registry workflow beyond a `model_versions` table (`Entities.kt:87-95`)
  with `evalStatus` and `active`, which nothing currently writes.

Two copies of `PropsRepository.kt` and `UiModels.kt` sit at the workspace root
(`C:\Users\tap45\Downloads\PropsLab\`) and duplicate the app sources. Editing the
wrong one is a real risk; delete them or add a note.

---

## 2. The blocking decision: offline purity vs. connectivity

Everything PropsLab wants from the ecosystem is a network hop. Three options:

| Option | Keeps "no INTERNET" true? | Cost |
| --- | --- | --- |
| **A. Two flavors** — `offline` (default, ships) and `linked` | Yes, literally | Two build variants, one extra manifest |
| B. Single build + "Connect" toggle | No — permission is always in the merged manifest | Simplest code, breaks the promise |
| C. Keep v1 offline, build ecosystem features later | Yes | Ecosystem work has no home |

**Decided 2026-10-28: A.** `productFlavors { offline {} linked {} }` with
`src/linked/AndroidManifest.xml` adding `INTERNET` + `ACCESS_NETWORK_STATE`. All
new code lives in `src/main`, guarded by a `BuildConfig.CONNECTED` flag or by
the flavor's empty client implementation. The default artifact keeps
`README.md` true, and CI can assert it:

```powershell
# must print nothing for the offline APK
aapt dump permissions app-offline-release.apk | Select-String INTERNET
```

Telemetry should also be **opt-in** with a visible "what is sent" list, because
this is a betting app. Default off in `linked`, seeded by a first-run prompt.

---

## 3. Lane 1 — Sieve telemetry

### What already exists

- Contract: `wiring/contracts/ecosystem-event.v1.schema.json`
  (`overlay.ecosystem.event.v1`, `additionalProperties: false`).
- Public keyless write edge: `wiring/cloudflare/sieve-ingest/` →
  `POST /ecosystem/event` on `https://sieve-ingest.tap4500.workers.dev`.
  Rate limits 60/min, 2000/day per IP; body ≤ 8 KiB; `data_class` **must** be
  `aggregate` (`src/worker.mjs:91-93`).
- Reference client: `phone-apps/concordance/app/src/main/java/com/example/concordance/telemetry/EcosystemTelemetry.kt`.
  It is exactly the shape PropsLab needs: deterministic SHA-256 event id,
  daemon thread, 2 s budget, fail-open, no request/response bodies.

### What is missing

1. `ALLOWED_SOURCES` in `wiring/cloudflare/sieve-ingest/src/worker.mjs:31-40`
   has no `propslab-android`. A trial event is refused `422` until it is added.
   Do **not** reuse `concordance-android`; a mislabelled event corrupts the
   fleet summary.
2. The `id` regexes differ and PropsLab must satisfy the stricter one:
   - edge: `[A-Za-z0-9_.:-]{1,128}` (no `/`, no `@`)
   - sieve: `^[A-Za-z0-9][A-Za-z0-9_.:@/-]{0,119}$`
   Generate `evt_<24 hex>` like the reference; never include `/`.
3. `subject.id` must start with a letter or digit — sanitize raw ids.

### Event catalog (props only; no player names, no stakes, no profit)

| kind | subject | props |
| --- | --- | --- |
| `propslab.app.open` | — | `version`, `flavor`, `telemetry_opt_in` |
| `propslab.slate.loaded` | — | `props`, `available`, `fixtures` |
| `propslab.prop.viewed` | `prediction/<id>` | `market`, `has_edge`, `model_version` |
| `propslab.paper_bet.placed` | `bet/<id>` | `market`, `side`, `edge_bucket`, `model_version`, `is_fixture` |
| `propslab.bet.settled` | `bet/<id>` | `outcome`, `rule_version`, `reconciliation`, `days_held` |
| `propslab.prediction.expired` | `prediction/<id>` | `reasons` (joined names, not text) |
| `propslab.agent.explain` | — | `latency_ms`, `accepted`, `rejected_numbers`, `model_version` |
| `propslab.memory.recall` | — | `layers_available`, `hits`, `latency_ms` |
| `propslab.calibration.decided` | `model/<version>` | `decision`, `n`, `logloss_delta`, `ece` |

`edge_bucket` as a bucket (`"0-2"`, `"2-5"`, `"5+"`) keeps the event useful
without shipping an exact model output tied to a user's bet.

### PropsLab changes

- `telemetry/EcosystemTelemetry.kt` — port of the reference client. Build the
  JSON by hand as it does; do not add a serializer dependency for one envelope.
- `BuildConfig.PROPSLAB_TELEMETRY_URL` — empty in `offline`, set in `linked`.
- `data/TelemetryOutboxEntity` + DAO — the reference client drops events when
  offline, which is fine for a doctor's research call and wrong here (the phone
  is offline most of the time).
  - `send` = enqueue first, then attempt; `TelemetryWorker`
    (`NetworkType.CONNECTED`, exponential backoff, max ~8 attempts, prune > 7 days).
  - Deterministic ids make the retry idempotent on Sieve's side.
- Enforce ≤ 8 KiB locally too, so a big `reasons` string fails at the source
  rather than being rejected at the edge.
- Tests (JVM + MockWebServer 4.10.0, same as concordance): envelope validates
  against the schema, id is stable across retries, size cap, and a test that
  greps the payload for player/team names and fails if any appear.

---

## 4. Lane 2 — Agent memory

There are three different "memory" products in this ecosystem. They are not
interchangeable, and PropsLab should use two of them.

### 4.1 Sieve `/recall` — the read-side memory spine (ready today)

`sieve/src/http.mjs:321-419` fans out one query over three layers and reports
per-layer health:

```
POST /v1/sieve/recall        (mobile-gateway → sieve /recall)
{ "query": "...", "k": 5, "layers": "local,recourse" }

→ { hits: [{ id, text, score, layers: ["recourse"] }],
    layers: { local: {available, count}, recourse: {available, count, reason} },
    available: true }
```

- The gateway route already exists (`wiring/cloudflare/mobile-gateway/src/worker.mjs:203-216`)
  and holds `SIEVE_AUTH_KEY` server-side. **No new route needed.**
- Send `layers:"local,recourse"` (or `code:false`): the `code` layer is a source
  index and irrelevant to a props question.
- The `layers` block is the point: `available:false` with a `reason` must be
  surfaced as "could not look", never as "nothing known".

### 4.2 Recourse `/fleet/memory` — the write side (needs one route)

`recourse/src/routes/memory.ts:99-113` and `src/lib/fleetMemory.ts`:

```
POST /api/recourse/fleet/memory      (guarded by RECOURSE_API_SECRET)
{ "source": "propslab-android", "kind": "lesson",
  "id": "propslab.settled.<betId>",
  "goal": "...", "status": "win|loss|push", "summary": "...",
  "data": { "market": "player_points", "line": 24.5, "p_over": 0.58,
            "outcome": "WIN", "model": "lr-2026-10-01" } }
```

- `kind` ∈ `gene | lesson | hypothesis | signal | snapshot`
  (`src/lib/vectorMemory.ts:25`). Use `lesson` for settled outcomes and
  `hypothesis` for a proposed recalibration.
- `text` **or** `goal/status/summary` is required; text ≤ 10 000 chars, `data`
  preserved up to 20 000 (`fleetMemory.ts:20-21`).
- Trust is derived, not claimed (`deriveTrustLevel`): a self-reported outcome is
  T1 unless a deterministic artefact backs it. A settled prop **does** have an
  artefact (the settlement row), so pass `deterministicCheck` + `checkEvidence`
  rather than `declaredTrust`.
- **Gap:** `RECOURSE_ALLOW` in `mobile-gateway/src/worker.mjs:78-83` does not
  list `/fleet/memory`. Add it. This is a public write path — keep the per-IP
  budget, the 256 KiB body cap, and the existing source/kind constraints; do not
  widen it to a generic proxy.

### 4.3 Agent-support hub — the on-device memory reference

The hub is a real, running Android app at
`C:\Users\tap45\Downloads\phone-apps\agent-support` (Kotlin/Compose, package
`com.example`, 112 JVM tests per `evidence/2026-10-28-unit-tests.md`). Docs:
`ECOSYSTEM_HUB.md`; contract: `contracts/agent-support.openapi.json`.

**Transport.** `integrations/ecosystem/EcosystemContextServer.kt:80` binds
`ServerSocket(8766, 50, InetAddress.getByName("127.0.0.1"))` — loopback only.
`EcosystemLoopbackSocketTest.kt` drives it over a real `HttpURLConnection`, so
the wire format is proven, not just the handler. It has **not** been exercised
across two app processes on a physical device.

**The reliability problem.** The server is started in
`AgentSupportApp.onCreate()` (`:135`) and stopped in `onTerminate()` (`:141`),
which Android never calls on a real device. There is no foreground service, so
the hub is reachable only while the agent-support **process** is alive. Once the
OS reclaims it, `127.0.0.1:8766` returns connection refused and every PropsLab
memory call fails — silently, if you don't check.

That was the single thing to fix on the hub side, and it is now implemented:
`EcosystemHubService` (a `specialUse` foreground service started from
`MainActivity`) owns the server lifecycle, and `start()` is guarded against a
double bind plus worker-pool reuse after `stop()`. `specialUse` rather than
`dataSync`, because Android 15 caps `dataSync` at six hours per day and this is
local IPC.

Two caveats remain: it has not been exercised on hardware, and OEM task-killers
can still end a foreground service — so PropsLab must probe
`GET /api/ecosystem/health` and fall back to the cloud lanes rather than assume
reachability.

**Four preconditions before PropsLab can call it**

1. `propslab` is seeded as a member. `EcosystemRegistry.SIBLINGS`
   (`EcosystemRegistry.kt:268-300`) lists five apps and PropsLab is not one.
   Add a `Quadruple("propslab", "PropsLab",
   "Player-prop probability lab: calibration and paper-trade outcomes.",
   listOf("props", "probability", "calibration"))`. `EcosystemScopes.DEFAULT`
   already grants `context:read`, `shared:read`, `memory:write`,
   `registry:read` — everything a write-scoped member needs.
2. The member token is copied into PropsLab. Tokens are **derived**, not stored:
   `"{appId}.{base64url(HMAC-SHA256(hubSecret, appId:tokenSalt))}"`
   (`EcosystemRegistry.kt:28`, `:161-163`). PropsLab cannot derive it without
   the hub secret, so the token has to be copied from the **Eco** tab and held in
   EncryptedSharedPreferences. Handle `401` by telling the user to re-copy —
   a token rotation on one member invalidates only that member, but it does
   invalidate PropsLab.
3. PropsLab's `linked` flavor has `INTERNET` (§2). Loopback still needs it.
4. PropsLab permits cleartext to loopback. Android blocks cleartext from
   targetSdk 28; copy the hub's own posture (`app/src/main/res/xml/network_security_config.xml`):
   `<base-config cleartextTrafficPermitted="false"/>` plus a `domain-config`
   allowing only `127.0.0.1` and `localhost`. Never widen this to the gateway.

**Context read** — `POST /api/ecosystem/context`, needs `context:read`:

```json
{ "query": "points over 24.5 model reliability",
  "max_tokens": 4096, "model_kind": "API", "allow_shared": true }
```

Returns `rendered` (a prompt-ready string), `bundle` (structured pools),
`token_estimate`, `block_count`, `budget_exhausted`. Use `LOCAL` for an
on-device model and `API` for the gateway LLM; the projections differ on purpose
(dense tag block vs. structured JSON with provenance and scores).

**Write** — `POST /api/ecosystem/ingest`, needs `memory:write`:

```json
{ "title": "Settlement lesson: player_points 24.5",
  "content": "Paper bet settled WIN. p_over 0.58, market 0.55, edge 3.0 pts.",
  "tier": "TIER_2_ARCHIVAL", "category": "EPISODIC",
  "tags": "settlement,player_points", "provenance": "propslab:settle:<betId>",
  "provenance_type": "INTERNAL_VERIFIED" }
```

- `title` ≤ 200 chars, `content` ≤ 10 000 (`EcosystemContextHub.kt:362-363`).
- Writes land only in the caller's own scope; there is no parameter that reaches
  a sibling or the shared reference (`EcosystemContextHub.kt:178-183`).
- `PoisonDefenseEngine` rejects injection-shaped content with `422`
  (`:216-222`). Send composed summaries, never raw user text.
- Tiers: `TIER_1_CORE` (needs `memory:core:write`, not granted),
  `TIER_2_ARCHIVAL`, `TIER_3_PROCEDURAL`. Categories: `PROJECT`, `DECISION`,
  `EPISODIC`, `PROCEDURAL`, `FAILURE`, `PROFILE`.
  Use `EPISODIC` for settled bets, `DECISION` for a promotion/rejection,
  `FAILURE` for a prediction that expired on stale inputs.

**What PropsLab should use it for**

- Before generating an explanation: assemble context with the prop's market and
  model version. The hub's own retrieval is deterministic (64-dim hashed
  embedding, cosine × 0.55 + keyword 0.30 + tier 0.15) — no inference needed,
  which is why it is safe to call on the UI path.
- After settlement or a calibration decision: ingest one structured memory.
- Keep the same `MemoryStore` abstraction as §4.1/4.2 so `HubMemory`,
  `SieveMemory` and `RecourseMemory` can be composed (hub first when reachable,
  cloud second), and a hub outage degrades instead of failing.

`FACTORY_HUB_INTERCONNECTIVITY.md` also lists a planned `/v1/hub/*` mobile-gateway
proxy. That is for *cloud* apps reaching a *phone* hub; PropsLab is on the phone
already, so it does not need it.

### 4.4 What the assistant may do with memory

`LocalAssistant` must keep its existing guarantee: it never supplies a number
that competes with the scorer. Recall text may inform *prose* and *which flags
are worth surfacing*; a number found in a memory hit is not evidence and must be
labelled as prior context with its source and age. Mirror Sieve's provenance
decay: store `fetchedAt` + `score` + `layer`, show "as of" in the UI, and drop
hits older than the model's feature cutoff.

---

## 5. Lane 3 — Recourse self-learning (honest mapping)

Read the actual loop before promising this:

- `recourse/src/routes/learn.ts` — `/learn/status|episode|run|replay|directives`,
  `/evolve`, `/verify`, `/approve`, `/crossover`. The learner
  (`src/dream/learner.ts`) scores **genes** that generate code, verified by
  property tests (`README.md:22-40`).
- `sieve/src/learn/` — an offline loop over **coding sessions**: ingest edits,
  detect review rules, label outcomes, `summarize()` → per-rule precision →
  `calibrationAction` (`learn/calibrate.mjs`). The `calibration` table is
  keyed by `rule_id` and counts `fixed/confirmed/dismissed/unresolved/ignored`.
  It is not a probability calibration store.

**Neither learns sports probabilities.** Claiming otherwise would be the same
kind of false green that `sieve_verify` exists to catch. What genuinely
transfers is the *discipline*, and it maps cleanly onto PropsLab:

| Recourse/Sieve principle | PropsLab equivalent |
| --- | --- |
| A promotion needs a real sandboxed test suite | A model promotion needs a held-out, time-ordered evaluation |
| A gene with no verified backing tool is `skipped`, never promoted | A candidate with insufficient samples is `insufficient_evidence`, never promoted |
| Deterministic replay recomputes counters from raw records | Recompute Brier/log-loss from `settlements`, never trust a stored summary |
| Every outcome folds into a durable ledger | Each settled paper bet becomes one `lesson` memory + one telemetry event |

### The PropsLab learning loop (build this)

```
settleBet() ──> settlements (existing, append-only)
                   │
                   ├──> CalibrationWorker (nightly, charging + network, opt-in)
                   │       ├─ recompute metrics from settlements
                   │       ├─ fit candidate recalibration on the *old* slice
                   │       ├─ evaluate on the *new* slice (no leakage)
                   │       ├─ gate: n >= N, Δlogloss > 0, ECE <= threshold
                   │       └─ promote | reject | insufficient_evidence
                   │
                   ├──> recourse /fleet/memory  (lesson, with the settlement as evidence)
                   └──> sieve telemetry         (propslab.calibration.decided)
```

New pure Kotlin in `domain/Calibration.kt` (unit-testable, no Android):

```kotlin
object Calibration {
    fun brier(p: Double, y: Boolean): Double
    fun logLoss(p: Double, y: Boolean): Double
    fun reliability(pairs: List<Pair<Double, Boolean>>, bins: Int): List<Bin>
    fun expectedCalibrationError(pairs: List<Pair<Double, Boolean>>, bins: Int): Double
    fun fitPlatt(pairs: List<Pair<Double, Boolean>>): Platt   // closed form on the logit
    fun kellyFraction(p: Double, decimalOdds: Double): Double
}

sealed interface PromotionDecision {
    data class Promote(val from: String, val to: String, val n: Int, val dLogLoss: Double) : PromotionDecision
    data class Reject(val reason: String) : PromotionDecision
    data object InsufficientEvidence : PromotionDecision
}
```

Room additions (one migration, version bump): `calibration_runs`
(id, modelVersion, n, brier, logLoss, ece, sliceStartUtc, sliceEndUtc,
decision, createdAt) and `memory_outbox` (same shape as the telemetry outbox).
Every promotion/rejection also writes an `audit_events` row — the table is
already append-only and is the most honest artefact in the app.

Guard rails worth writing down now, because they are easy to lose later:

- Minimum sample size (start at 200 settled bets per market) and a minimum
  time span; otherwise `InsufficientEvidence`.
- Chronological split only. A random split leaks future information into the
  past and will always look like an improvement.
- One market in scope (`player_points`) until the gate passes there. The app
  already rejects out-of-scope markets (`RejectReason.OUT_OF_SCOPE`).
- A promotion changes only `plattA/plattB` (or the isotonic table), never the
  feature set. Changing features is a new model, not a recalibration.

Optional, later: push `hypothesis` memories to Recourse and let the fleet
comment on them. `POST /v1/recourse/learn/episode` is already allowlisted,
but it expects the learner's own episode shape — do not force it.

---

## 6. Lane 4 — Math X

### What it is

`apps/math-x` — Express API + React workbench + `math-core`. 14 route groups
(`apps/api/src/index.ts:57-70`): chat, chat-ai, codegen, plan, verify,
literature, domain, autoformalize, ocr, bio, hypothesis, analogies, models,
export. `MODE_PREFIXES` includes a real `probability` mode
(`packages/math-core/src/prompts.ts:55`).

### The blocker

`cloud/endpoints.json` (probed 2026-10-23) records `math-x /api/chat-ai` as
`200` with body:

```
data: {"error":"No output generated. Check the stream for errors."}
```

and `/api/models` as `claude:false, ollama.available:false, qwen.available:false`.
Separately, `verify.ts`, `plan.ts`, `codegen.ts` and `chat.ts` each do
`new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY })` **at module load**
(`apps/api/src/routes/verify.ts:6`, `plan.ts:6`, `codegen.ts:6`), and
`cloud/README.md:232` confirms no Anthropic key is configured. So:

- `/api/verify`, `/api/plan`, `/api/codegen`, `/api/chat` will not work as
  deployed.
- `/api/models` is misleading: `qwen.available` is derived only from local
  Ollama tags (`models.ts:15-24`), so a hosted OpenRouter Qwen reports
  `false` even when the lane is healthy. Do not use it as a health check.

**Fix before depending on it** (eco-side, not PropsLab):

1. Re-probe `/api/chat-ai` with `provider:"qwen"` and a model the gateway
   actually serves; the last recorded call produced no tokens.
2. Either add `ANTHROPIC_API_KEY` to the `mathx-keys` secret, or move
   `verify`/`plan`/`codegen` onto `services/modelRouter.ts` so a Qwen lane works.
3. Make `/api/models` report the *configured provider* (base URL + model +
   reachability), not just Ollama tags.
4. Add the OpenAPI document that `wiring/registry.json:351` already claims
   exists at `apps/math-x/docs/openapi.json` — the directory does not exist.
5. Add a gateway route (`/v1/math/*` with `MATHX_URL` + `MATHX_API_KEY`)
   before any phone calls it directly.

### How PropsLab should actually use it

Runtime math must stay deterministic and on-device. A model that re-derives a
probability on every render is unauditable, slow, and violates the app's own
rule that the scorer is the only source of probabilities.

So split the use:

- **Derivation and review (offline / CI, not runtime).** Use `/api/chat-ai` with
  `mode:"probability"` to derive or critique formulas — e.g. the push-aware EV
  expression, the Platt closed form, the isotonic PAVA step, the Kelly cap.
  Record each derivation in `docs/derivations/<topic>.md` with the prompt,
  model, and a SymPy check.
- **Verification.** `/api/verify` extracts steps and returns `sympyCode` for a
  **client-side Pyodide** run (`verify.ts:110-118`) — it does not verify
  server-side. That is useful in the workbench, not in Kotlin. Accept it as a
  workbench tool; do not plan to call it from the app.
- **Parity test.** Freeze the derived formula's reference values into a JSON
  fixture and assert them in `DomainTest`, exactly like the existing
  `logisticScorerParityAndGuard` test does for the Kaggle bundle
  (`DomainTest.kt:40-49`).

---

## 7. Eco-side contract changes (one table)

| Repo / file | Change | Why |
| --- | --- | --- |
| `wiring/cloudflare/sieve-ingest/src/worker.mjs` | add `propslab-android` to `ALLOWED_SOURCES`; add one acceptance test | otherwise every event is `422` |
| `wiring/cloudflare/mobile-gateway/src/worker.mjs` | add `/fleet/memory` to `RECOURSE_ALLOW` | write-side memory |
| same | add `/v1/math/health`, `/v1/math/chat`, `/v1/math/verify` behind `MATHX_URL`/`MATHX_API_KEY` | keep the key off the phone |
| same | add `/v1/propslab/health` (sieve + recourse + math reachability) | one call for the Settings screen |
| `wiring/registry.json` | add a `propslab-android` service entry (url null, source path, consumers) | the registry is the map; omitting it is how things get forgotten |
| `wiring/registry.json` | fix or remove the `math-x` `docs/openapi.json` claim | the file does not exist |
| `apps/math-x` | see §6 fixes | math-x cannot answer today |
| website `lib/mobile-apps.ts` + `app/.well-known/*` | add `propslab` SHA-256, App Link `pathPrefix="/propslab"`, browser fallback page | only if deep links are wanted; that registry file is not in this checkout, so this step needs the website repo |
| `phone-apps/agent-support` → `features/ecosystem/EcosystemRegistry.kt` | **done** — added a `Quadruple("propslab", …)` to `SIBLINGS` | without it every hub call is `401 Unknown application 'propslab'` |
| `phone-apps/agent-support` → `integrations/ecosystem/EcosystemHubService.kt` | **done** — `specialUse` foreground service owns the server lifecycle | otherwise the hub is unreachable whenever the app is backgrounded |
| `phone-apps/agent-support` → `contracts/agent-support.openapi.json` | document the `propslab` member and any new route | the contract is the only cross-app source of truth today |

Registry/assetlinks work is **not** in this workspace's `concordance-web`
checkout (`apps/concordance-web/lib` has no `mobile-apps.ts`). Treat it as a
separate change in the website repo.

---

## 8. Work breakdown

### Phase 0 — decisions and contracts (½ day)

- [ ] Confirm flavor strategy (§2). This gates everything else.
- [ ] Add `propslab-android` to the ingest allowlist + test; deploy.
- [ ] Add the three gateway routes + `RECOURSE_ALLOW` entry; deploy; `GET /health`
      reports every upstream configured.
- [ ] Re-probe math-x; decide "dependency" vs "workbench only".
- [ ] Delete or annotate the duplicate root-level `.kt` files.

**Done when:** a hand-built envelope from PowerShell returns `202` with
`upstream_status: 200`, and the event is readable back from the spine as
`propslab-android / <kind>`.

### Phase 1 — telemetry (2–3 days)

- [ ] `libs.versions.toml`: `okhttp 4.10.0`, `mockwebserver 4.10.0`.
- [ ] Flavors + `src/linked/AndroidManifest.xml` (INTERNET, NETWORK_STATE).
- [ ] `telemetry/EcosystemTelemetry.kt` (port), `data/TelemetryOutbox*`,
      `work/TelemetryWorker`.
- [ ] Settings → Telemetry: toggle, endpoint (read-only in release), "what is
      sent", "last delivery".
- [ ] Tests: schema shape, id determinism, 8 KiB cap, no PII strings.

**Done when:** airplane-mode bet placement queues an event; reconnecting
delivers exactly one copy (dedupe by deterministic id); offline APK has no
INTERNET permission.

### Phase 2 — memory (3–4 days)

- [ ] `memory/MemoryStore` interface + three implementations:
      `HubMemory` (loopback `127.0.0.1:8766`), `SieveMemory` (gateway recall),
      `RecourseMemory` (fleet write). All no-ops on `offline`.
- [x] Hub: seed the `propslab` member; add a foreground service so the hub
      survives backgrounding (`EcosystemHubService`, `specialUse`).
- [ ] `linked` flavor: `network_security_config.xml` allowing cleartext to
      `127.0.0.1`/`localhost` only.
- [ ] Settings → Agent hub: paste token (EncryptedSharedPreferences),
      `GET /api/ecosystem/health` probe, clear message on `401` (re-copy) and on
      connection refused (hub not running).
- [ ] `AssistantContext`: builds a hub context query from the prop
      (`market + model version`, no stake), caches the bundle with `fetchedAt`;
      composes hub first, cloud second.
- [ ] `ExplainInput` extended with `memories: List<MemoryHit>`; assistant prompt
      constrained to prose; a post-filter drops any number not present in the
      scorer output (extend the existing rule, add a test).
- [ ] On settle: ingest an `EPISODIC` hub memory **and** a Recourse `lesson`
      (the settlement is the deterministic evidence), plus a
      `propslab.bet.settled` event.

**Done when:** with the hub app running, the detail screen shows "prior
context (3)" opening a sheet with source + age; with the hub force-stopped it
shows "hub unavailable" and still explains from cloud memory; with both down it
shows the reason, not an empty list.

### Phase 3 — data extract, then the self-learning gate (5–6 days)

- [ ] Build `propslab-training.sqlite` from §9 with a `manifest.json` (source
      hashes, row counts, date range, the SQL used). Deterministic, re-runnable.
- [ ] Chronological split: fit on seasons through 2022-23, hold out 2023-24
      through 2025-26. Record the split boundaries in the manifest.
- [ ] Generate the distribution table: `P(pts = k | μ bucket)` on a 0.5-point
      grid, Laplace-smoothed, ~5k floats, exported as JSON with the fit window
      and the source hash (§9).
- [ ] `inference/EmpiricalDistributionScorer : StatisticalScorer` — mean blend
      (`0.5·season-to-date + 0.5·trailing-10`) into the table, out comes
      P(over)/P(push)/P(under). This also removes the "integer lines need a push
      model" refusal at `Scorer.kt:37`.
- [ ] `domain/Calibration.kt` + `PromotionGate` (pure, no Android).
- [ ] Room migration: `calibration_runs`.
- [ ] `work/CalibrationWorker` (nightly; charging + `NetworkType.CONNECTED`;
      opt-in), gated by `InsufficientEvidence` rules.
- [ ] `model_versions` gets a writer; promotion flips `active` in a transaction
      and writes an audit event.
- [ ] Parity fixture: 50 player-games scored in Python and in Kotlin, asserted
      equal to 1e-9 (extends `DomainTest.kt:40-49`).
- [ ] `Outcome.VOID` + the settlement path for it. A void returns the stake like
      a push but is a different audit fact (§9), and it is 5.8% of graded props.

**Done when:** a synthetic stream where the candidate is genuinely better
promotes; one where it is equal or worse rejects; 50 samples says
`insufficient_evidence`. All three cases are unit tests, not manual checks.

### Phase 4 — math-x bindings (1–2 days, after §6 is fixed)

- [ ] `scripts/derive.ps1` (or `.mjs`) that calls `/api/chat-ai` with
      `mode:"probability"`, writes `docs/derivations/*.md`.
- [ ] Gateway `/v1/math/*` smoke test from the app's Settings → Diagnostics.

---

## 9. Data foundation — `D:\datasets\sports-datasets`

Inspected 2026-10-28. This is a Kaggle/GitHub corpus (23 dataset folders under
`kaggle/` plus loose exports beside them), not a curated feed. Every fetch in
`kaggle/fetch_log.txt` succeeded (`ok=18 fail=0`), so nothing is silently
missing. What matters is which files carry the three
things a props model needs: **labels**, **features**, **market quotes**.

### Usable

| Need | Source | Shape | Verdict |
| --- | --- | --- | --- |
| **Labels + features, 1996 → 2026** | `kaggle/chevronronson__nba-stats-dataset/sql/nba_stats.sqlite` (1.2 GB) | `player_boxscores` **787,531 rows**, 30 seasons (`1996-97 … 2025-26`), `min, pts, usg_pct, pace, available_flag, …` | **primary source of record** — CC0-1.0, already SQLite |
| Game index | same DB, `games_index` | 38,023 games with date, matchup, home/away, result | join key |
| Market, timestamped | same DB, `game_odds` | 514,660 rows — **moneyline only**, `open`/`close`/intraday snapshots | freshness + implied strength; not props |
| Schedule / season type | same DB, `games_schedule` | 1,406 rows, `season_type`, `completed` | the freshness gate's clock |
| Historical backfill, 1950 → 2018 | `ehallmar__…/nba_players_game_stats.csv` | measured **1,268,211 rows**, 68 season-years, `1950-10-31 → 2018-06-08` | research only — Kaggle license is **unknown** |
| 2010-11 → 2023-24 box scores | `NBA data/NBA-Data-2010-2024-main/…` | player-game, MIT | cross-check for the extract |
| Referee assignments | `kyleknowles1__nba-referee-assignments-2016-2025` (2.4 MB) | 4 officials per game, 2016 → 2026 | optional feature — **CC-BY-NC-SA-4.0, non-commercial** |
| Team Elo, pre-game | `fivethirtyeight__…/nbaallelo.csv` (18 MB) | `elo_i`, `elo_n`, `forecast`, 1946 → | CC0-1.0 team-strength feature |
| Injuries | `loganlauton__…` | roster transactions 1951-2023, CC0 | superseded by `available_flag` |
| DraftKings DFS | `alandu20__…` | salaries + projections per slate, no prop lines | not a quote source |
| Play-by-play | `chevron…/play_by_play.csv` (3.2 GB) / `xocelyk` (2.9 GB) | event level | avoid; the box score already has the label |

Two traps in those files:

- A DNP row has `min = null` and `pts = 0`. It is **not** an under; it is an
  excluded observation. Use chevron's `available_flag`, not a `pts is not null`
  filter.
- The three eras do not share a `game_id` space: chevron uses `0042500405`,
  the referee file uses `42500317`, ehallmar uses `0021000358`. Join referee
  data on date + teams, never on the raw id.

### The gap that decides the product

**There are no historical player-prop lines.** The only files with "props" in
the name are three identical copies of a season-statistics export
(`NBA Stats 202324 All Stats  NBA Player Props Tool (4).csv`) — PPG, RPG, APG,
no over/under anywhere in the header.

The one genuine prop-quote file is `NBA data/odds_fpts.csv` (and `odds_ftts.csv`),
which is *first player to score* / *first team to score*: per-book lines
(`br_`, `dk_`, `fd_`, `mgm_`, `pb_`), a model probability, edge, Kelly and units.
That is an excellent **format reference** for what a prop quote looks like, and
it is not a points dataset.

This was checked twice, not assumed: a filename scan across the whole tree found
only three copies of the season-stat export, and the largest odds file in the
corpus (`chevronronson/…/game_odds.csv`, 95 MB / 514,660 rows) has exactly one
market — `decimal_home, decimal_away, moneyline_home, moneyline_away`.

Consequence, stated plainly:

- The model's **calibration** can be fitted and validated here.
- Its **edge versus the market** cannot. `PropUi.marketOver` and `edgePts`
  require a quote the corpus does not contain.

So v1 stays paper/fixture, and the README's "model minus market" number is
untestable until a prop-quote source is added. Do not substitute a game
spread/total for a player line to make the gap look filled.

### Artifact mismatch worth knowing

`dataset-metadata.json` describes a 235-table star schema
(`fact_player_game_traditional`, `dim_game`, …) in DuckDB **and** SQLite. The
actual `nba.sqlite` on disk is the older 16-table `nba_api` dump:

`game` 65,698 · `line_score` 58,053 · `play_by_play` 13,592,899 ·
`inactive_players` 110,191 · `officials` 70,971 · `player` 4,815 ·
`common_player_info` 3,632 · plus 9 smaller tables.

There is no `fact_player_game_traditional` and no player box-score table at all.
`nba.duckdb` is 12 KB. Query the file, not the README.

### Extract, don't consume in place

The primary source is already a SQLite database, so do not re-export 766 MB of
CSV. Derive a slim, versioned `propslab-training.sqlite` from it:

- `player_game(player_id, game_id, date, season, team, opp, home, min, pts, available, usg_pct, pace ± derived lags)`
- `game(game_id, date, home_id, away_id, moneyline_close_home, moneyline_close_away, implied_home, implied_away)`
  — derived from `game_odds` at the `close` snapshot; there is no spread/total here
- `official(game_id, date, official_name)` — only if referees are used, and only
  in a non-commercial build (CC-BY-NC-SA)
- `manifest.json` — source path, sha256, row counts, date range, extraction command, and the SQL used

Rules:

- Extraction is deterministic and re-runnable; the manifest **is** the provenance.
- Chronological split only: fit on seasons through 2022-23, hold out 2023-24,
  2024-25 and 2025-26. A random split here would leak the future into the past
  and would always look like an improvement.
- Exclude rows with `available_flag = false` from labels; they are not unders.
- The APK ships the fitted `LogisticBundle` JSON (a few KB) and its parity
  fixture. Never the corpus, never a queryable database.

Licensing is the part that decides what can ship:

| Source | License | Ship? |
| --- | --- | --- |
| `chevronronson` (primary) | CC0-1.0 | yes |
| `fivethirtyeight` Elo | CC0-1.0 | yes |
| `loganlauton` injuries | CC0-1.0 | yes |
| NocturneBear 2010-2024 box scores | MIT | yes |
| `kyleknowles1` referees | CC-BY-NC-SA-4.0 | **non-commercial only** |
| `pablote` | CC-BY-SA-4.0 | share-alike propagates |
| `jamiewelsh2` | ODC-By | attribution |
| `anandaramg` RAPTOR | LGPL-3.0 | avoid in a shipped artifact |
| `ehallmar` (1.27M historical rows) | **unknown** | research only, do not ship |

Attribution belongs in the derived manifest and the app's about screen. If
PropsLab is ever monetized, the referee dataset and ehallmar must be dropped, not
argued about.

### What the size buys

- 787,531 player-games (1996-97 → 2025-26) is enough to fit *and* honestly
  evaluate a per-market model, instead of a 50-row "validation".
- A real parity fixture: extract 50 player-games, score in Python and Kotlin,
  assert equality to 1e-9 — the pattern already in `DomainTest.kt:40-49`.
- Calibration bins with thousands of samples each, so ECE is a measurement and
  not noise.

### Offline set downloaded 2026-10-28

Five more datasets pulled with the Kaggle CLI into
`D:\datasets\sports-datasets\kaggle-downloads\2026-10-28\`, hashed in
`manifest.json` beside them:

| ref | file | bytes | why |
| --- | --- | --- | --- |
| `cviaxmiwnptr/nba-betting-data-october-2007-to-june-2024` | `nba_2008-2026.csv` | 2,493,308 | 24,440 games, 2007→2026, spread/total/moneyline + 2H |
| `minmaxer/nba-closing-lines-ats-2024-25` | `nba_2024_25_closing_lines_ats.csv` | 326,163 | closing lines plus **de-vigged** fair odds and cover probs |
| `minmaxer/nba-closing-lines-ats-2025-26` | `nba_2025_26_closing_lines_ats.csv` | 326,818 | same, current season |
| `caseydurfee/mgm-grand-nba-betting-data` | `all_odds.csv` | 1,598,822 | MGM, with stake/wager percentages and won flags |
| `andyt338/graded-player-props-mlb-nfl-23-sportsbooks` | `nfl_graded_props_14d.csv` | 62,145,904 | 213,190 graded props, 23 books — schema reference only, **no NBA** |

Search coverage: `kaggle datasets list` for "player props", "nba prop odds",
"prizepicks", "sportsbook odds nba" returned no NBA prop-line dataset. The gap
is not a search failure; it is the market.

### Formula bench — measured 2026-10-28

`research/formula_bench.py` → `research/report.md` + `research/results.json`.
Source: `cviaxmiwnptr` moneylines, **19,810 of 24,440** games with both sides
priced.

**F1 — pricing.** `no-vig(-110, -110) = 0.5`; hold `4.7619%`. Across the book:
mean hold **3.745%**, median 3.759%, p95 4.708%, and **0** rows with negative
hold (no arbitrage in the sample).

**F2 — market calibration, i.e. the number to beat.** Home win rate 0.5866.
Brier **0.202329** (0.25 is a coin flip), log loss **0.589246**, ECE (10 bins)
**0.005610**. De-vigging beats using the raw implied price on both scores
(0.202962 / 0.591227), which is the expected direction and a useful regression
check. The bin table is in `research/report.md`; the largest gap is 1.8 points in
the 0.0-0.1 bin, which has only 63 games.

**F3 — Kelly.** The closed form matches a 200k-point numeric maximisation of
`p·ln(1+bf) + q·ln(1−f)` in every synthetic case (max error 2e−6). At the vigged
price, **0 of 19,810** rows have a positive stake (max f* = −0.0003).

That last line is the whole point: the de-vigged market probability is not an
edge against the same book. PropsLab needs its own model probability, and
0.202329 Brier on the holdout is what it has to beat to mean anything.

### Working backwards: the formula the data supports

Full working in `research/eda_report.md` (scripts `eda_stage2..5.py`). 736,410
regular-season player-games, fit before 2023-24, evaluated on 70,952 holdout
rows, every feature lagged.

What the data decided, against the obvious choices:

| candidate | Brier | ECE | verdict |
|---|---|---|---|
| empirical table keyed by predicted mean | **0.2486** | **0.0027** | ship this |
| Poisson(μ) | 0.2499 | 0.0192 | fallback only |
| negative binomial, pooled `r` | 0.2537 | 0.0554 | **worst parametric** |
| player's own last 10 games | 0.2608 | 0.0720 | too noisy |
| table keyed by μ × own sd | 0.2490 | 0.0059 | second key hurts |

The basis, stated as the thing to build:

1. **Mean:** `μ = 0.5·season-to-date + 0.5·trailing-10`, floored at 0. The
   regression put the coefficients at 0.469 / 0.471 — two independent windows
   agreeing on equal weight. Holdout RMSE 6.09, MAE 4.63.
2. **Distribution:** `P(points = k | μ)` from a Laplace-smoothed empirical table
   on a 0.5-point μ bucket. 83 buckets × 61 values ≈ 5k floats, deterministic,
   ships as JSON beside the `LogisticBundle`.
3. **Push:** read `P(points = line)` from the same table. Integer-line push mass
   is **7.33%** weighted and ranges 2.9%-19.1% by line — not a constant, and far
   from the zero the current scorer assumes by rejecting integer lines
   (`Scorer.kt:37`).

Three measured reasons not to do the obvious thing:

- Overdispersion is 6.8× and *falls* with the mean, so a pooled negative binomial
  misprices the near-mean region (ECE 0.055 vs 0.003).
- Minutes explain only **37.5%** of within-player variance (median R²), so a
  minutes model alone cannot fix the distribution.
- Adding the player's own sd or minutes as a second key **makes calibration
  worse** (ECE 0.006 / 0.008 vs 0.003). The mean is doing all the work.

What this still cannot do: price a real line (no prop quotes in the corpus), and
handle a cold start (needs 5 prior games).

### One schema lesson from the graded-props file

213,190 rows, 23 books, 685 players, 4 markets. Resolution vocabulary:
`lost` 113,078 · `won` 87,715 · **`void` 12,359** · `push` 38.

- `payout_multiplier` is **not** a payout (mean 0.976 on `won`, range 0.6-7.9) —
  ignore it and trust `resolution` + `price_decimal`.
- `line` is null in 34% of rows and `actual_value` in 5.8%: do not assume a graded
  prop is complete.
- **PropsLab has no `VOID`.** `Outcome` is `WIN | LOSS | PUSH`
  (`domain/Odds.kt:30`), and `settleBet(actual: Double)` cannot express "the book
  voided it". In this sample void is 5.8% of graded props — bigger than push.
  Economically it returns the stake like a push, but it is a different audit fact
  and should be its own enum case before real settlement exists.

### The challenger run — measured 2026-10-29, verdict PROMOTE

Pre-registered in `research/CRITERIA.md` before the kernel ran. One holdout
evaluation, no re-tuning. Kernel:
`tap919/propslab-challenger-table-vs-constrained-gbm` (v3, 529 s, CPU, no internet).
Artifacts: `research/kaggle/runs/2026-10-29/`.

Dataset: `tap919/propslab-nba-prop-training`, 665,203 player-games, 30 seasons,
sha256 `9623768…efba8dbc`. Fit ≤ 2022-23 (594,251), holdout 2023-24 → 2025-26
(70,952 player-games, 496,664 grid evaluations).

| model | Brier | log loss | ECE | slope | ΔBrier |
|---|---:|---:|---:|---:|---:|
| **C2 constrained GBM** | **0.224787** | **0.637540** | **0.00570** | 0.986 | **−0.001520** |
| champion table | 0.226308 | 0.640863 | 0.01376 | 0.966 | — |
| core-only GBM (ablation) | 0.225573 | 0.639265 | 0.00488 | 0.985 | −0.000735 |
| C1 logistic | 0.228109 | 0.648117 | 0.01033 | 0.938 | +0.001801 |
| base rate | 0.229415 | 0.650969 | 0.00678 | 0.946 | +0.003107 |

All six promotion conditions passed, and both cluster bootstraps agree
(10,000 draws, seed 20261029):

| cluster | ΔBrier | 95% CI | clusters |
|---|---:|---|---:|
| by `game_id` | −0.001520 | [−0.001769, −0.001279] | 3,478 |
| by `player_id` | −0.001519 | [−0.001789, −0.001249] | 746 |

Per season: −0.001593, −0.001404, −0.001564 — consistent, not one lucky year.
Monotonicity violations 0.000% (max violation exactly 0), so `P(over)` never rises
with the line.

**My prior was wrong.** I expected the table to win, and said so in
`CRITERIA.md §1`. The GBM won on Brier *and* on ECE, so this is not the
negative-binomial failure mode (sharper but falser) — it is better on both axes.

**H3 is rejected.** I predicted the gain would concentrate at |δ| ≥ 2, because the
table's blind spot is the line's position relative to the mean. It does not: tail
mean −0.001432 vs middle mean −0.001638, best at δ = −2, worst at δ = +3. The gain
is broad and slightly *stronger* near the mean. So the mechanism is not "line
position far from μ". The most likely source is the champion's level bias: it
predicts mean 0.4973 against an observed 0.4836 (+1.37 pp), while the GBM predicts
0.4892 (+0.56 pp). Flattening that bias is worth about what the Brier improvement
is. This is a hypothesis about the mechanism, not a measured decomposition, and it
is not needed for the verdict.

**H4 confirmed.** Dropping `rest_days`, `b2b`, `opp_def100_trail` and `t_usg` costs
0.000785 Brier — under the 0.001 threshold. Opponent defense and rest are real but
marginal; `t_sd`, `t_min` and `mu` carry the model.

**C1 logistic is rejected outright.** It loses to the champion by +0.0018 with a CI
that excludes zero, and only clears the slope bar. A linear model in 12 features
cannot represent the distribution's shape. Recorded so nobody tries it again.

**A caveat about the rule itself.** The promotion rule applies the ECE ≤ 0.010 bar
to challengers only. Measured pooled across all seven offsets, the *champion* has
ECE 0.01376 and would fail its own bar. At δ = 0 it was 0.0027; pooling sharpens the
tails and inflates binned ECE. The rule is asymmetric and this run did not change
it after seeing results. The verdict stands because the GBM beats the table on every
metric simultaneously — Brier, log loss, ECE, slope, all three seasons, both
clusters — so no reading of the numbers favours the table.

**What this does not mean.** Brier 0.2248 against a synthetic line is not an edge.
There are still no player-prop quotes in any dataset we hold (`§9`, gap section), so
`marketOver` and `edgePts` remain untestable, and every probability is
`P(over | plays)`. Availability is not modelled. A 0.67% relative Brier improvement
is a better price, not a profitable one.

**Engineering consequence.** The champion table ships as a 121×61 CDF JSON — trivial
for Kotlin. The promoted model is 4 cross-fitted `HistGradientBoostingClassifier`
ensembles (200 trees × 31 leaves each) plus 4 isotonic step functions. There is no
sklearn on Android, so this needs an exporter: trees and isotonic knots to a compact
JSON, and a Kotlin tree-walker behind `StatisticalScorer`. That work is gated on the
parity fixture below, and until it exists the table stays the shipped fallback —
exactly as `CRITERIA.md §11` prescribes.

---

## 10. What not to do

- Do not put a Sieve or Recourse **read** key in the APK. The read key can see
  the whole ecosystem summary; that is exactly why `sieve-ingest` exists
  (`wiring/cloudflare/sieve-ingest/README.md:12-22`).
- Do not call `math-x` at render time to produce a probability.
- Do not describe Recourse's learner as "learning the props model". It does not.
- Do not reuse `concordance-android` as the telemetry source.
- Do not send `data_class:"deidentified"` through the phone lane; the edge
  refuses anything but `aggregate` (`sieve-ingest/src/worker.mjs:91`).
- Do not random-split the calibration data.
- Do not add a generic pass-through proxy to the mobile gateway. Every route is
  an allowlist entry for a reason.
- Do not call the hub without probing `/api/ecosystem/health` first. Its server
  lives in another app's process and disappears without notice (§4.3).
- Do not POST raw user text to `/api/ecosystem/ingest`. `PoisonDefenseEngine`
  returns `422` and, more importantly, the hub is a shared memory — send
  composed facts with provenance, not transcripts.
- Do not treat `vegas.txt` or a game spread/total as a player prop line. It is
  not one, and substituting it makes every edge number fictional.
- Do not ship the dataset or a queryable DB inside the APK (§9).
- Do not fit on seasons that postdate the holdout, and never let 2024-25 into
  the fit.
- Do not ship `ehallmar` (license unknown) or `kyleknowles1` referees
  (CC-BY-NC-SA, non-commercial) in a monetized build. The CC0 primary
  (`chevronronson`) exists precisely so this is not a judgement call.
- Do not use `payout_multiplier` from the graded-props file as a payout; it does
  not behave like one. `resolution` + `price_decimal` are the trustworthy pair.
- Do not record a voided market as a push. Same money, different fact.

---

## 11. Open questions

1. ~~**Flavor vs. toggle**~~ — **decided: two flavors** (§2). `offline` is the
   default and keeps the no-INTERNET guarantee; `linked` carries the ecosystem
   lanes.
2. ~~**Is the agent-support hub going to exist on-device?**~~ — **yes**, it is
   `phone-apps/agent-support` (§4.3). Open sub-question: do we add a foreground
   service there, or accept "open the hub app first" and degrade to cloud
   memory? Recommendation: foreground service, because a best-effort hub makes
   the memory feature appear broken at random.
3. **Telemetry opt-in default** — off is safer; on is more useful. Recommend off
   with a one-time prompt.
4. **Does PropsLab ever take real odds?** If so, the telemetry allowlist and the
   privacy story need a second look before shipping, not after.
5. **Token handoff.** Copy-paste from the Eco tab is honest and works. A
   signature-scoped token exchange would be nicer, but it requires both APKs to
   be signed with the same key — true only if PropsLab ships under the same
   publisher identity. Decide before designing the Settings screen.
