# Cheat Code

Android app for the NBA Player Props AI blueprint. Kotlin, Jetpack Compose, Room, WorkManager. Local-first, paper trading only. The app's only network paths are read-only HTTPS GETs — prop prices, box scores, league injury reports and public sports news — and none of them can place a wager.

Five screens, in the order the work happens: **Today** (the board, with each game's moneyline, spread and total above its props), **Best** (what the promoted formula would take), **Sandbox** (the fake bankroll and the formula ladder), **Bets** (the paper ledger), **Lab** (Math X formulas, the local-model boundary, screenshot import, the live feed, the agent console). The audit trail sits behind the sandbox, the parlay builder behind Best, and the model shop, share cards, bet slips, Premium, updates, settings, the fantasy desk and the sports wire behind the lab. A parlay card can become a shareable bet slip with the board's players and club marks already attached; a leg can be swapped for another priced candidate and each player leg has its own two-way line slider, with the chance, price and shared image recomputed from the edited legs. Gambly accepts the image and Pikkit's QuickPickBot accepts the text, while the final book button only opens the book because no sportsbook allows an external prefill.

The app is free. A $5 monthly Premium tier adds the ten-leg ticket, the fantasy desk, the sports wire, watermark-free cards and a cloud-model fallback; everything analytical stays free. The marketing site lives in `site/` at the workspace root, and the offline manual is copied into `app/src/main/assets/` for the APK.

## Easy mode is the default

Easy mode is not a different model or a weaker gate. It is the same arithmetic with the explanation order reversed: a novice sees what the app is doing, what it already knows, and the one action left, while the formulas, t-statistics, drawdown and tool schemas sit behind **Show the work**.

- **Autopilot card.** The Best and Lab screens lead with the three components that run on their own: the price board, the formula search and the learning ledger. A fourth line appears only when imported history is actually present. The card reports stored state, never a promise, and **Run everything now** performs one ordered pass: sync, round, learn, refresh the live book.
- **Bootstrap on launch.** First launch seeds fixtures, syncs if a key is present, runs the first formula round only when none exists, updates the learning ledger and refreshes the live book. Every step is idempotent; a failed network call is reported and the rest continue. A 12-hour worker keeps the learning ledger current with no screen open.
- **One missing step, named.** With no real price sync the card says so and offers the Live data screen. It does not make the user hunt through settings or infer why the board is synthetic.
- **Advanced stays reachable.** Lab, Settings and Sandbox each collapse their technical surfaces behind one toggle; selecting Full mode reveals them permanently. No feature is removed.

## It works without a programming step

The agent tools already know today's board. `get_edge`, `stage_ticket`, `open_book` and `set_formula` fill any omitted argument from the same state the Best screen is showing: the top pick's id and side, a supported book, the 1-unit paper stake and `best` for the leading passing formula. A supplied argument always wins. The console exposes the same shortcuts in plain words — *show the best pick*, *stage the best pick*, *confirm* — and the model prompt carries a one-paragraph live briefing with the top pick, formula, bankroll, settled count and imported-history size.

## Open it

1. Open this folder in Android Studio (Ladybug or newer). Let it sync. If you build from a terminal, run `gradle wrapper` once first, because the wrapper jar is not included.
2. `vendor/llama.cpp` must be present at the workspace root (one level above the `Cheat Code/Cheat Code` project): the native runtime is built from it. Without it the app still compiles and runs — the model card reports that the runtime did not load.
3. Run on the S25 Ultra or an emulator, API 29 or higher. The native library ships for arm64-v8a only.
3. First launch seeds synthetic fixtures, runs the first sandbox round, syncs if a key is already present, and starts the learning ledger. Every screen that shows invented data says so; the Autopilot card names the one missing step.
4. To trade real prices instead, open **Live data** from the Autopilot card (or `Lab → Show settings and tools → Live data`) and add a prop-price key and a box-score key. The first successful sync retires the fixtures and everything after that updates itself.

The build targets and compiles against API 36 with AGP 8.13, and release builds are minified and resource-shrunk with keep rules for the JNI bridge and WorkManager. Text recognition is `com.google.mlkit:text-recognition` 16.0.1 (bundled), which adds roughly 40 MB to the debug APK because the model ships inside the app rather than being downloaded; 16.0.1 is also the first bundled version whose native library passes the 16 KB alignment check.

## What is here

| Path | Purpose |
| --- | --- |
| `domain/` | Odds math, settlement with push handling, `OutcomeProbabilities` validation, `PredictionGate` (stale, missing, unmatched, out of scope), pricing and Kelly. Pure Kotlin. |
| `sim/` | The formula sandbox: a deterministic simulated season, the strategy grid, the bankroll engine, the promotion gate and the autonomous plan. Pure Kotlin. |
| `inference/` | `StatisticalScorer` interface, `LogisticBundleScorer` (standardised logistic regression plus Platt calibration, the Kaggle export format), `LocalAssistant` with a template implementation. |
| `mathx/` | Math X prompt modes (MIT), the on-device formula catalog and checks, the `LocalMathModel` boundary, `LlamaCppModel` (llama.cpp runtime) and the probability whitelist guard. |
| `src/main/cpp/` | The native runtime: CMake for llama.cpp plus a four-function JNI bridge. |
| `ocr/` | Bundled on-device text recognition and the pure `SlipParser` that turns a bet slip into picks. |
| `feed/` | The two read-only providers, their parsers, the name/team join, the game-log model and the sync that writes the board and settles it. |
| `agent/` | The tool registry (ticket tools plus the system tools), the router that turns a sentence into one tool call, the console engine and transcript, the strategy notes, and the policy that makes all of it safe. |
| `data/` | Room schema for eleven tables, DAOs, repository, fixture seeder, sandbox persistence. `audit_events` has insert and read only. |
| `work/` | `FreshnessWorker` re-runs the gate every 15 minutes; `SandboxWorker` runs a formula round every 6 hours; `FeedWorker` syncs prices, box scores and the injury wire every 30; `LearningWorker` updates the ledger and calibration every 12. |
| `ui/` | Theme, components, Today, Best, Sandbox, Portfolio, Formula lab, Import, Audit, Prediction detail, Model shop, Premium, Account, Updates, Share cards, the offline manual activity. |
| `brand/` | Every public address in one place: repo, site, community, support email, price. |
| `billing/` | Free/Premium entitlement, the Stripe link, the seven-day trial and the offline licence checksum. |
| `hub/` | Device measurement, the Hugging Face model shop and downloader, and the GitHub release updater. |
| `flyer/` | The 1080×1350 share-card renderer and the share/save actions. |
| `slip/` | The bet-slip model, the 1080-pixel slip renderer, the bundled club-mark fallback and the Gambly/Pikkit/book handoff. |
| `account/` | Optional Supabase GoTrue email-OTP sign-in. |

## The sandbox: a fake bankroll that has to earn the live book

A formula in Cheat Code is not an expression string; it is a selection rule with four numbers — how much model-versus-market disagreement it demands, which side it may take, and the price band it accepts. That is a small enough space to search exhaustively and too small for a string to become code.

Every round:

1. **Generate a season.** 150 days × 80 props = 12,000 props, derived from a seed. Nothing is stored: a season is a pure function of its seed, and each round uses a new one, so a formula has to work on data it has not seen.
2. **Run all 21 formulas** over it, flat 1-unit stakes, 250 units of fake bankroll.
3. **Gate the results.** A formula moves to the live book only when it clears all five checks: at least 400 bets, return on stake above 1.5%, a t-statistic above 2.0 (so the profit is distinguishable from luck), worst drawdown under 45%, and a hit rate above the break-even the prices it actually took demanded.
4. **Read the losing record.** Settled props are split by side, price band, edge size, market, line shape and recency. A segment with at least 12 decisions can become a finding, and a finding can become a candidate rule: drop a losing side or market, tighten a losing edge bucket, narrow the price band, or focus a paying market into its own formula. Candidates run on the same fresh season as every other row; the gate decides, never the insight.
5. **Re-test the lessons on real history.** When an imported dataset with at least 400 usable rows is present, a rule learned from the losing record cannot be promoted on simulation alone: every lesson candidate runs over those recorded prices and results, and one that fails is vetoed and cannot hold or defend the live book. With no dataset, or too few usable rows, the round says the historical check was skipped instead of treating silence as proof.
6. **Trade the live board.** Only the promoted formula takes picks, and it takes them on the real board with real quotes. Those picks are the Best screen.
7. **Re-check later.** An incumbent that stops clearing the gate is retired and the best passing candidate takes the book. A loss-informed challenger can also take it while the incumbent still passes, but only when the challenger clears the gate and earns at least one full point more return on stake with more profit on the fresh season.

On a 12,000-prop season the ladder separates cleanly:

| Formula | Bets | ROI | t | Gate |
| --- | --- | --- | --- | --- |
| Any disagreement, both sides | 12,000 | +1.56% | 1.79 | fails — not distinguishable from luck |
| Edge ≥ 2.0, both sides | 9,114 | +2.65% | 2.65 | passes, promoted |
| Edge ≥ 4.0, both sides | 6,355 | +3.61% | 3.01 | passes |
| Edge ≥ 4.0, over only | 3,241 | +2.12% | 1.26 | fails — half the volume, half the evidence |

The simulated world is deliberately beatable, and that is spelled out in `SandboxTruth`: its hidden truth is **70% the model and 30% the market**, plus a small per-prop wobble. So a wide disagreement really is worth money there. That makes the sandbox a test of the *search* — can the grid recover an edge that is known to be present — not evidence that the real market can be beaten. On a season where the model simply repeats the price, every formula earns about zero and nothing is promoted, which `SandboxTest` pins down.

Results are keyed by prop id, so a re-run is identical to the cent and two formulas betting the same prop always see the same outcome. For fixtures the result comes from the sandbox oracle and is labelled synthetic; a real quote imported from a screenshot only settles once a result has actually been recorded for it.

The loop is autonomous: launch runs a one-time bootstrap (sync, first round, learning, live-book refresh), WorkManager covers the recurring passes (prices every 30 minutes, formula search every six hours, learning every twelve), and **Run everything now** on the autopilot card performs the same sequence on demand. It is written down as an append-only audit event each time, including which formulas failed and why.

## Screenshot import

`Lab → Open the importer` reads a bet slip out of a screenshot with bundled on-device text recognition, then hands the text to `SlipParser`. The parser understands the shapes the books print (`LeBron James Over 24.5 Points -115`, `More 24.5 Points` under a name, `O 31.5 PTS`, `Less 12.5 Rebounds`), normalises unicode minus signs, and drops anything it cannot read rather than guessing. The screen shows the text it actually saw.

An imported pick enters the board as a **market-only quote**: the estimate is the market's own no-vig price, so the edge is zero by construction and the sandbox will not bet it. That is the honest position — no model has scored it — and it gives the board a real line, a real price and something to settle against. If no price was legible, −110 is assumed and the row says so.

## The feed: real prices, real results

`Lab → Live data` takes two keys, both read-only: a prop-price provider and a box-score provider. With them the live book stops trading a simulation.

1. **Box scores first.** The last two days of finished games are reduced to `pts`, `reb`, `ast`, `fg3m` per player. That is the model's input, and it is what settles props whose game has finished.
2. **Prices second.** Upcoming games are read for the selected books and markets. One two-way pair survives per game, player, market and line — the pair with the lowest hold, from a single book, so the model is never flattered by mixing a DraftKings over with a PrizePicks under.
3. **The estimate** is a normal approximation of the player's own last 20 games, with the spread floored at 30% of the mean and 8 games required before anything is published. Integer lines carry a real push mass. Five markets are priced: points, rebounds, assists, threes and PRA — each a column of a box score or a sum of them.
4. **The fixtures retire.** The first sync that writes real quotes marks every synthetic prediction unavailable and closes its game. They keep their place in the ledger; they lose the right to be traded.

`FeedWorker` runs the whole thing every 30 minutes and then settles the live book. It never promotes a formula — that stays the sandbox's decision — so a good week at the book cannot masquerade as a discovered edge. It can *retire* one: once 150 of the promoted formula's real picks have settled, a losing record on real results sends it back to the sandbox. Synthetic results are excluded from that count, because the point of the check is the world, not the simulator.

Late availability comes from two places. The **league's own injury report** is read first, on every sync, with no key: a designation of Out, Doubtful, Questionable or Game-Time Decision for a player on the board becomes a signal, and one this side does not understand is dropped rather than guessed. **Headline search** is the optional second channel and needs a news key; it only becomes a signal when a headline names the player and states a status. Either way the projection is what moves, never the price, the shift is bounded per status, a signal older than 18 hours is recorded but not applied, and the row names the signal that changed it. A report entered by hand on `Lab → Live data` always outranks both.

The same screen holds the **high-compute lane**. Training does not happen on a phone, so the app does not pretend to try: with a Kaggle username and API key saved it lists the kernels that have run and reports the latest status, and nothing is downloaded or pushed. The key is written to private storage and never read back onto the screen.

Cost is real: the price provider charges per market per region, and its free plan is 500 credits a month. The defaults are deliberately small, the remaining credits are shown on the screen, and no request is spent with no key saved. Provider details, quotas and the exact endpoints are in [research/ODDS-FEED.md](../research/ODDS-FEED.md).

## Fantasy desk and sports wire

Two consumer surfaces sit behind the lab, and both are Premium. The free build opens the same destinations and sees a locked placeholder that names what the feature is; it reads no feed and spends no request.

**Fantasy desk** (`Lab → Open the fantasy desk`) tracks football and basketball from keyless ESPN endpoints. It asks for one sport at a time:

1. **Finished games only.** Basketball reads today's and yesterday's scoreboards; football walks back up to seven days to the most recent slate. Up to eight finals are read, one summary each.
2. **Fantasy scoring.** Basketball is PTS + 1.2 REB + 1.5 AST + 3 3PM. Football is half-PPR: 1/25 pass yard, 4 pass TD, −2 INT, 1/10 rush yard, 6 rush TD, 0.5 reception, 1/10 receiving yard, 6 receiving TD. Both formulas are printed on the screen.
3. **Form, not a forecast.** The projection is a weighted average of the games read (newest first), the trend is the last three against the three before, and volatility is the standard deviation as a share of the average. A player with fewer than three games says so on his card.
4. **Start / watch / sit / out.** The call compares a player with his own baseline. Out or Doubtful on the league report overrides form, and Questionable is a watch without a guessed designation. The screen states that no league roster, scoring setting or opponent is connected.
5. **Watchlist.** A star writes `sport:player-key` to private preferences, so tracked players come back after a refresh. Nothing leaves the phone.
6. **Injury wire.** Each game summary already carries both clubs' designations, so the wire is built from the same eight reads: newest first, with the club's short comment. An empty read is shown as empty, not as a clean bill of health. The league-wide `/injuries` route is deliberately avoided here; the NFL version is 8.7 MB in one response.
7. **Game log.** A card can open the athlete's documented `gamelog` for a longer, per-game history. It is fetched only on the first open, cached for the screen's lifetime, and scored with the same formulas. One response carries every season split, so chips switch the panel between regular season, postseason and preseason without another request; regular season is the default, and an empty split is shown as empty rather than borrowing another split's games. Each split states its game count, average and best fantasy score, counted only from rows with a recognised score, so a kicker's unscored log does not report a misleading zero. A line field under the summary reports Over and Under counts for that split only; exact pushes are shown separately and never counted as losses. ESPN's columns are joined by their canonical names rather than their position, so a quarterback's passing and rushing yards cannot be swapped and a kicker is shown without an invented fantasy total.

The NFL box score does not carry the stat-column names the NBA payload does, so the parser reads the column order ESPN has used for passing, rushing and receiving and skips a row shorter than the columns it needs. A player who appears in two categories — a quarterback who also ran — is merged once. The basketball parser is the same one the live board already uses, so there is one reading of that payload.

**League capture** (`Fantasy desk → Capture my league`) takes a photo or up to nine gallery screenshots and reads the lineup on the phone. The three modes are My lineup, Opponent and League table. The parser reads slots, names, pro teams, opponents and any projected or average points; the review list shows every row it accepted, every warning, and the raw OCR, and a bad row can be removed before it is saved.

Each week is stored separately: a player added in week 8 is not overwritten by week 9, and a league-table capture does not move the current roster forward. The snapshots keep the record and points as printed. **Every player gets a short call and a reason** built from the saved slot, the imported projection, the last ESPN games, the trend and any injury designation. A player with no projection from either source is Watch with that sentence in the body; the app does not invent a number. Importing the opponent adds the projected head-to-head edge, and the season track lists only weeks that were actually captured.

Photos are filled in by a WorkManager job after the save. Only each player's name is sent to ESPN's public search directory; the screenshot, league name, team names and projections stay on the phone. A name that is not an exact match in the right league stays as initials instead of taking another athlete's headshot.

**Sports wire** (`Lab → Open the wire`) reads NFL, NBA, MLB and NHL news in parallel from ESPN's public `news` endpoint, filters by league, and shows the headline, summary and time. Every row links out to ESPN; no article text is mirrored and no remote image is downloaded. A league that fails is named in the note instead of taking the other three down.

Neither screen is a betting signal, and both say so. Every read is one of ESPN's documented site endpoints — `news`, `scoreboard?dates=`, `summary?event=` — and none of them needs a key.

## Straight markets and parlays

The board also carries the three markets a book puts above its props: the **moneyline**, the **spread** and the **total**. They come from the same event payload as the props but from a slower clock — three hours, because each market costs a credit — and they are stored in `game_lines` as snapshots, replaced on every refresh. A spread or total is only compared within the same number (a −3.5 is never shopped against a −4.5), and a no-vig chance is only computed inside one book's pair.

Those lines are now **scored**, not just quoted. `GameModel` builds a team-strength rating from the box scores the feed already stores — every finished game is a result both teams carry — and projects a margin and a total for each side. `GameForecastBoard` turns that projection into a win chance with the league's measured spread until 40 games are stored, so a thin October board shows prices with no forecast rather than a confident number from two games. A model chance is compared with the book's no-vig chance inside the same pair, and only a side with positive expected value is published as a pick. The spread is settled on the side that was taken (the away side of a home −3.5 is +3.5) and a total pushes on the exact number, the way a book does it.

Points props use the **Kaggle empirical table** when the artifact is present: `P(points ≤ k | trailing mean bucket)` measured over 594k real player-games, shipped as `assets/champion_table.json`. It needs only the trailing mean and the line, so it fits the phone's data instead of inventing the GBM's twelve features, and it gives an integer line a real push mass instead of a normal-approximation guess. Every other market stays on the game-log normal model, and the row names which one priced it.

The **parlay lab** (`Best → Build`) turns the day's ranked picks into 3-leg, 5-leg and 10-leg tickets, a same-game ticket, and a stated budget split across them.

Every ticket reserves a game-market leg first (two on the ten-leg) so the game model actually reaches the ticket; props then fill the rest in edge order, one per game while the board allows it, then one per player, and only after that from a game already used. The chance is the product of the legs' model probabilities with each leg's push mass conditioned out; the price is the product of their decimals. That is why the numbers are honest and small: ten legs at roughly 60% each multiply to about 0.6%, and the big payout is the market pricing that. The card says how many legs share a game and how many are game markets, because those legs are correlated and the product is then an approximation.

**Line push.** A slider moves every repriced player leg 0–4 points against the bettor — an Over needs one more, an Under one fewer — and the ticket's chance and price move in opposite directions. `LinePush` rebuilds the leg's distribution (the stored game-log mean and spread, exactly the normal approximation that published the quote) and holds the book's markup where it was quoted, so zero push returns the quoted price untouched. A leg with no stored game log gets a curve inferred from its own probability and line, marked as inferred on the row: that fallback exists so synthetic fixtures still demonstrate the control, not as evidence about a player. Game markets keep their quote because the app has no team-margin distribution to reprice them. Every pushed price is labelled an estimate; no book quoted it.

**Same-game parlay.** The builder finds the game supplying the strongest three legs — two when one game supplies only that many — one per player, and prices it separately. Same-game legs move together, so positive correlation makes the true joint chance *higher* than the product and a book's fair price *lower* than the multiplied price — the reason books cap same-game payouts. `SameGame` lifts the probability and cuts the decimal by the same ratio, using a stated 10% log-odds pull (18% when two legs share a player). No stored data can fit a covariance matrix yet, so this is an assumption printed on the card, not a measurement.

**Circadian correction.** A toggle folds a capped, symmetric adjustment into each player projection: tip-off hour on the player's own team's clock, measured from a 19:30 norm, plus the zones crossed to the venue. The whole correction is capped at 1.5% of the projection and each moved leg reports its own percentage. Zones are fixed standard-time offsets, so daylight saving is ignored; the effect is capped well below that error's size.

**Budget.** A practice budget is split at the settings screen's Kelly fraction, with no ticket allowed more than a quarter of it. Whatever the edges do not justify stays unallocated and is shown as such, rather than being forced onto the longest shot.

**Formula per ticket.** Each card has its own formula picker, drawn from the sandbox's ladder: the promoted formula plus the best five by measured profit, or three rungs of the plain grid before a round has run. Switching a card rebuilds only that ticket from the same board. A scope panel names what each formula owns — edge bar, sides, price band, tilt, markets — and compares it with its neighbour, because a formula chooses legs and never a price. Nothing is stored and nothing is placed.

## Self-learning ledger

Every sync writes the model's lean on every priced row to `learning_bets` — player props and game markets alike, at the line and price that were live when the decision was made — and settles them from real box scores when the game finishes. Nothing is filtered by confidence, because a ledger of only the picks that looked good cannot tell the model whether it knows what "good" means. Each row carries its model chance, the market's no-vig chance, the gap in points, a HIGH/MEDIUM/LOW confidence label and a one-unit paper stake, so the record is a bet, not a spreadsheet row.

Once 200 settled props (or 200 settled game markets) exist, a **Platt correction** is fitted per track and stored in `calibration`. Every scored board then applies its own track's correction before publishing a chance, which is how the model refines itself from results: the slope and shift are evidence, they move only after 50 new settlements, and both Brier scores before and after are written to the audit trail. The Lab's Learning card shows the ledger totals, the two fits, and the latest decisions. A refit never promotes a formula — the promotion gate still owns that — so a good week at the book cannot masquerade as a discovered edge.

**Loss insights.** `LossInsights` reads every settled prop and looks for segments that are actually leaning: a side, a price band, an edge bucket, a market, a line shape, or the recent window against the one before it. Each finding carries its own sample, wins, losses, return on one-unit stakes and hit rate against the break-even its prices demanded; a segment below 12 decisions is never called a trend. A weakness can propose a concrete rule change — the opposite side, a higher edge floor, a narrower price band, or a market exclusion — while a paying pattern can propose a narrower new formula. Those proposals are added to the next round's candidate grid and must clear the same gate on a season they have not seen. The Lab lists the findings with the candidate's status: not tested yet, held in the sandbox, retired, or on the live book. An insight is evidence, never a promotion.

## Shipping: models, cards, Premium and updates

The consumer-facing half of the app is four screens behind the lab, and none of them is a placeholder.

**Model shop** measures the phone before it recommends anything: total RAM, free storage, cores and ABI. It suggests a parameter band, lists a curated shelf, then reads the repository's real GGUF files and sizes from the public Hugging Face API, marking each one *Fits* or *Too big* against a 55%-of-RAM budget with 600 MB kept free. A download is written as `.part` and only renamed when the byte count matches, so an interrupted file can never be loaded. The chosen file is written to `active.txt`, which `LlamaCppModel` now reads on every load — switching models needs an unload, not a reinstall. LoRA adapters are excluded from the base-model list because they cannot run alone.

**Share cards** are drawn with Android's canvas at 1080×1350, not screenshotted: four templates (bet of the day, my bet, settled, new strategy) assembled from the live Best flow and the paper ledger. Free cards carry a small mark; Premium removes it. Sharing goes through the app's `FileProvider`, which exposes only `cache/flyers` and `cache/updates`.

**Premium** is one plan at `$5`/month through a Stripe Checkout Session created by the site (`Brand.CHECKOUT`). The app keeps the entitlement locally, offers a single seven-day trial, and verifies an offline code of the form `PL5-XXXX-XXXX-XXXX` whose last four characters are an FNV-1a checksum of the first twelve. The check catches typos; it cannot prove a payment, and the manual says so. `tools/issue_licence.py` issues a code for the email a buyer used, and a licence server is the next step rather than a pretence of one.

**Updates** read `Brand.UPDATE_FEED` (`latest.json` on the site), compare its version with the installed one, download the APK into the cache while checking its SHA-256, and hand it to Android's installer. The parser also understands a GitHub release object, so a fork can point the feed at its own release API. The app cannot install silently and does not try.

The optional **account** is Supabase Auth — open source, self-hostable — using a passwordless email code. It verifies the address and keeps only that address on the phone: no strategies or ledger rows are uploaded, and the session token is not retained because nothing in this release uses it. Project URL and anon key are entered on the account screen; nothing requires an account. The optional **cloud model** is any OpenAI-compatible endpoint, used only when no local weights are installed and only while Premium is active.

## Design

Two accents, each tied to one meaning: aqua is the Over side, maple amber is the Under side. The split ring on the detail screen and the split bar on each row both mark where the no-vig market sits with a chalk tick. The gap between that tick and the end of the aqua arc is the edge. Type is Bricolage Grotesque for display and Manrope for text, both bundled as variable fonts under the SIL OFL (`FONT-LICENSE-OFL.txt`), with tabular figures switched on.

The dock sizes its five destinations by weight rather than padding, so labels never shrink the 48dp touch target on a narrow phone. The selected destination carries a drawn halo and a bar that animates into place; the board's live badge breathes; probability bars fill on first paint; parlay percentages count up to their value. `ui/components/Motion.kt` is the whole vocabulary — a glow means "this is the thing", a pulse means "this is live", and a counting number means "this changed".

The Math X prompt text is used under the MIT license declared by that project; see `MATHX-LICENSE.txt`.

## Math X and the local model

Math X's `formula` and `probability` mode prefixes and its Formula Lab structure are ported into `mathx/`. The authoritative math stays in Kotlin: the lab's 15 checks call the same `Pricing`, `Staking` and `Odds` functions the app ships. The independent SymPy reference is `research/mathx/verify_formulas.py`, which writes `research/derivations/propslab-pricing.md`.

The console is where a sentence becomes an operation. `ConsoleRouter` reads the line for a verb it knows and calls the matching tool; anything it does not claim goes to `LocalMathModel` through `DeviceAssistant`, which reports its own absence instead of inventing an answer. Each turn is stored in `console_turns` and marked `ran`, `notes`, `said` or `refused`, so the difference between the system doing something and the system talking about it stays visible. Consultations return `StrategyNotes` verbatim, because every figure in them is one the app actually uses. Tool arguments are optional where the app can derive them: *show the best pick*, *stage the best pick* and *confirm* resolve the top pick, its side, the supported book and the paper stake from stored state, and the model prompt is given the same briefing.

The only model boundary is `LocalMathModel`, and it is on-device only. No Anthropic client, no Math X server and no network call exist in the app. `LlamaCppModel` implements it with llama.cpp: `app/src/main/cpp/` builds `libpropslab_llama.so` for arm64-v8a from a `vendor/llama.cpp` checkout at the workspace root, and `llama_jni.cpp` exposes four operations — load, build the chat prompt, generate, free. The native code compiles at `-O2` even in a Debug build, because `-O0` inference is roughly an order of magnitude slower than this phone can actually do.

**Weights.** A base GGUF goes in the app's `files/models` directory and the first question loads it (mmap, a few seconds) off the main thread. A `*.lora*.gguf` in the same folder is not a model: only `qwen-mix-v2.lora.f16.gguf` is attached by default, named on the Lab's model card, and an adapter that refuses to attach is reported rather than hidden. With no weights present, the card states the exact path it looked for and the deterministic template assistant keeps explaining. `tools/copy-weights-to-app.sh` moves files between app sandboxes on a connected device, since two apps cannot read each other's private storage, and `adb logcat -s PropsLabNative` shows load timings, adapter state and per-request token counts. See `research/MATHX-INTEGRATION.md`.

## Rules the code enforces

- Probabilities must be finite, within [0,1] and sum to 1, or the prediction shows as unavailable.
- Stale quotes, stale injury data, a missing line and out-of-scope markets are rejected, not guessed.
- A paper bet stores the line and price at decision time. One bet per prediction and side.
- Settlement and bet placement run in Room transactions and write an audit event.
- A formula only trades the live board after clearing every gate check; promotion and retirement are audit events.
- The assistant never supplies a probability. `submit_live_wager` always returns Blocked: none of the three books publishes a wager API and all three prohibit automated wagering.
- Fixtures are tagged `synthetic_fixture` and never expire; imported quotes are `screenshot_import` and go stale like any real quote.
- Feed quotes are `odds_api` and carry the provider's own market timestamp, so a price that has aged out is rejected rather than re-dated.
- A prop with one side quoted is dropped, not priced from an invented second side. When two books quote the same line, the lower hold wins and both prices come from that one book.
- A prediction settles once, against a recorded box-score line. No result is ever overwritten.
- No estimate is published from fewer than 8 logged games, and the spread never falls below its floor.
- A loss insight never promotes a formula. It can only add a candidate that clears the same gate on a fresh season; a challenger needs at least one point more return on stake than a passing incumbent.
- A Premium surface is gated at the door: in the free build the fantasy desk and the wire read no feed and show a placeholder instead.
- A fantasy player is shown only from a finished game actually read. A thin sample is labelled thin, and a start/sit call never overrides an Out designation.
- ESPN news is a headline, a summary and a link out. No article is mirrored and no remote image is downloaded.
- Keys live in this app's private preferences. They are never written to the database, the audit trail or a log line.

## Tests

```
gradle :app:testDebugUnitTest
```

272 offline tests, all on the JVM. `DomainTest` covers settlement, the prediction gate and its market scope, and scorer parity. `FormulaTest` covers the pricing and staking layer across the full American-odds grid. `MathXTest` covers the formula lab, the ported prompt modes and the local-model fallback. `AgentToolsTest` covers the tool policy: every argument set that must be refused, the one paper bet a ticket may produce, and the live defaults that let a missing prediction id resolve to today's top pick without letting an explicit argument be overridden. `AutopilotStatusTest` covers easy mode's status card: a fresh phone naming the one missing key, a live formula with no pick not inventing one, and imported history becoming the fourth automatic step. `SandboxTest` covers the simulated season, determinism, flat staking, and the gate — including that a season without signal promotes nothing and that a loss-informed challenger can take the book only when it clears the gate and beats a passing incumbent on a fresh season. `LossInsightsTest` covers the losing-segment engine: the losing side, a low-edge bucket, a long-price leak, a paying market and a side-plus-price pattern, the 12-decision minimum, pushes and voids being excluded, the price-band and market-exclusion ids round-tripping into the same rule the engine bets, and the simulated bench carrying market labels so an exclusion changes what the gate tests. `HistoryGateTest` covers the historical second gate: a lesson candidate that clears the gate on imported rows, a market exclusion being respected rather than silently betting history, and a vetoed formula losing both the promotion and an incumbent's live book. `SlipParserTest` covers the OCR text shapes and the lines that must be skipped. `FeedTest` covers both provider parsers against the documented payload shapes, the name and team joins, cheapest-hold selection, the game-log model against hand arithmetic, its spread floors and push mass, and the estimate agreeing with settlement. `LeagueInjuriesTest` pins the injury-report parser to a real payload, including the designations it must drop. `ConsoleTest` pins the console's boundaries: an operation runs a tool and records the tool's own words, a strategy question returns the written note unparaphrased, an unknown line is marked `said`, a refused tool is recorded as refused, plain phrases route to the tool that already knows the pick, and the model prompt carries the live briefing. `ParlayTest` covers the parlay arithmetic: the product of chances and prices, push mass conditioned out of a leg, the reserved game leg on every ticket, and tickets that must be refused. `BetSlipTest` covers the slip: the tracker-readable pick lines, a moved line changing both price and chance, an edited leg writing the moved line into the text, the stake and return arithmetic, the club mark on every leg, every NBA and NFL code resolving to a bundled mark, and the public addresses for the books and trackers. `ParlayLabTest` covers the lab: a zero push returning the quote, a push trading chance for price on both sides, the push curve agreeing with `GameLogModel` to 1e-9, the inferred curve reproducing a row without a game log, the circadian cap, the same-game lift and its same-player variant, allocation that never exceeds the budget or the per-ticket cap, per-ticket formula selection in the composed screen state, and the capacity check that marks a formula which cannot fill a ticket ineligible and falls back with a stated reason. `GameModelTest` covers the team ratings and the game markets against hand arithmetic, the empirical table's half and integer lines, the spread mirror and the total push, and that calibration recovers a known correction. `FantasyDeskTest` covers the two scoring systems, the weighted projection, the hot and cold calls, an Out designation overriding any form, a Questionable designation staying a watch, thin samples being labelled thin, the tracked/trending split, the empty desk and the watchlist codec. `FantasyFeedTest` covers the news parser and its partial-failure note, the NFL box-score merge and column-order reading, the football scoring through that parser, the basketball mapping onto the same scoring, and the injury blocks inside a game summary. `FantasyLeagueTest` covers the captured league: basketball slots, pro teams and opponent parsing, the projection and season average in either order, league records and points, the sport-calendar week default, the start and bench calls, an injury overriding form, a missing projection staying a watch, recent ESPN form outranking an old screenshot number, a two-team projected edge, the club-code aliases, the team-logo parser, and an exact-name headshot match that refuses a same-name athlete in the wrong league. `FantasyGameLogTest` covers the athlete gamelog in both sports: canonical-name scoring, sorting, the regular-season preference, postseason staying separate from the default split, the split averages and line hit rate including pushes, the kicker fallback and malformed input. `HubTest` covers the licence token: issued codes verify, case and punctuation are ignored, one wrong character is refused, and version tags compare in both directions.

Four more tests are live smoke checks, not part of the offline count. `EspnLiveTest` fetches the documented scoreboard, summary, athlete gamelog and news endpoints and runs the same parsers, so a payload change is caught against the service rather than only against a fixture. They skip unless the environment variable is set:

```
PROPSLAB_LIVE_ESPN=1 gradle :app:testDebugUnitTest --tests com.propslab.EspnLiveTest
```

## The manual and the website

`site/manual.html` is the user manual, with ten screenshots taken from this build. It is shipped in the APK as `assets/manual.html` (images in `assets/img/`) and opened offline by `ManualActivity` with JavaScript disabled, so it works with no network. `site/index.html` is the storefront, using the same palette, the bundled fonts and the `logo.svg` mark; it is static and needs no build step. Re-copy the manual and images into the APK assets after editing the page.

Measured results and the independent Python bench are in `research/FORMULA-TESTS.md`.

## Next steps

1. Stand up the licence server so a Stripe webhook issues a signed code automatically; the token format can stay as it is.
2. Let the ledger reach 100 settlements and check the refit's Brier against the pre-fit number; a correction that does not improve it should stay at identity.
2. Give the sandbox a second axis to search: market filters, per-player shrinkage, and a stake-size parameter, with the same gate on every candidate.
3. Run one sync with real keys and reconcile what the providers actually send against `FeedTest`'s documented shapes — the parsers are tested against the docs, not against today's payloads.
4. Implement `LocalMathModel` with llama.cpp or LiteRT-LM, measure load time and throughput on the S25 Ultra, and keep the template fallback for when weights are absent.
5. Add an instrumentation test that opens a v2 database, applies the 2→3 migration and reads the feed tables back.
