# Betting integration — DraftKings, PrizePicks, Fanatics

Researched 2026-10-29. Three independent reviews (one per operator) read the operators'
own terms and documentation; the claims below carry their sources.

## The headline

**None of the three publishes an API that can place a wager, all three prohibit
automating it, and none documents a deep link that prefills a bet slip.** So PropsLab
has no bet-placing code path — it has a *proposal* path that ends with a human.

| Question | DraftKings | PrizePicks | Fanatics |
| --- | --- | --- | --- |
| Public API to place a wager | No | No | No |
| Official odds/props API for third parties | No (private B2B only) | No | No |
| Terms allow bots / scripts / automation | No — terms prohibit automated means (bots, scripts, parsers, screen scrapers) | No — "unauthorized scripts or other automated means" is an explicit violation | No — "any automated use of any system", incl. a "bot to engage in play" |
| Documented deep link that prefills a slip | No | No | No |
| Geolocation required at bet time | Yes (state-by-state, GeoComply-class checks) | Yes | Yes |

## What does exist

**DraftKings**
- No public Sportsbook developer portal or wager endpoint. The documented flow is
  manual: pick → bet slip → stake → confirm.
  [DK support: how to place a bet](https://support.draftkings.com/dk/en-us/how-do-i-place-a-bet-on-draftkings-sports-betting?id=kb_article_view&sysparm_article=KB0010423)
- The well-known `draftkings` endpoints are **DFS** (contests, draft groups,
  salaries), undocumented and unsupported; the PyPI client
  [`draft-kings`](https://pypi.org/project/draft-kings/) is read-only DFS.
  [Unofficial endpoint notes](https://github.com/SeanDrum/Draft-Kings-API-Documentation)
- The "Marketplace API" is NFT collectibles, not betting. Supplier deals (e.g.
  [Genius Sports](https://www.geniussports.com/newsroom/draftkings-and-genius-sign-nfl-official-sports-data-single-game-parlay-fan-engagement-agreement/))
  are B2B contracts, not public interfaces.
- [US terms](https://sportsbook.draftkings.com/legal/us-terms-of-use) ·
  [NJ terms](https://sportsbook.draftkings.com/legal/nj-terms-of-use) *(these pages
  returned 403 to the fetch, so the clause text was not read verbatim — treat the
  terms as controlling, not this summary)*

**PrizePicks**
- No public developer API and no API documentation.
  [Independent profile](https://github.com/api-evangelist/prizepicks)
- The [partner application](https://www.prizepicks.com/prizepicks-partner-application)
  is an affiliate programme, not a developer integration.
- [Terms of Service](https://www.prizepicks.com/help-center/terms-of-service) name
  "unauthorized scripts or other automated means" as improper conduct, with
  disqualification, prize forfeiture and account termination.
- The Feed feature shares *submitted lineups and profiles* — not a link that
  preselects a player, stat and side.
  [Feed announcement](https://www.prizepicks.com/press-news/prizepicks-unveils-new-social-features-with-launch-of-the-feed)

**Fanatics Sportsbook**
- No public wager API; the acquired PointsBet US platform and Banach technology are
  operator-side, with no public partner docs.
  [Acquisition announcement](https://www.fanaticsinc.com/press-releases/fanatics-betting-and-gaming-closes-its-acquisition-of-the-us-businesses-of-pointsbet)
- [Missouri terms](https://sportsbook.fanatics.com/legal/mo/terms-of-service) prohibit
  "any automated use of any system", a "bot to engage in play", proxy wagering, and
  require one individual's personal use plus verified in-state location.
- "Copy Bet" shares a slip **inside** Fanatics; there is no partner link format.
  [Product page](https://betfanatics.com/sportsbook)

**Read-only odds data (the legal way to show these books' lines)**
- [The Odds API](https://the-odds-api.com/) lists DraftKings and PrizePicks as
  bookmakers, and its [terms](https://the-odds-api.com/terms-and-conditions.html)
  permit display in websites and mobile apps (no raw resale). Its
  [deep-link field](https://the-odds-api.com/releases/deep-links.html) is
  "if available" — the worked example is FanDuel, not any of these three.
- [OpticOdds](https://opticodds.com/sportsbooks/draftkings-api),
  [SportsGameOdds](https://sportsgameodds.com/bookmakers/prizepicks-odds-api),
  [SharpAPI](https://sharpapi.io/sportsbooks/prizepicks-odds-api),
  [BettingPros](https://www.bettingpros.com/api-data/) also sell *about*-the-book data.
  These are vendor products, not operator APIs.
- [Sportradar](https://developer.sportradar.com/getting-started/docs/get-started) and
  [Genius Sports](https://www.geniussports.com/bet/odds-feeds-api/) are licensed B2B
  feeds requiring a contract.

A Gravity Index search for an odds provider returned nothing — the category is not in
its catalog, so the list above comes from the web research, not from a recommendation.

## What PropsLab ships instead

Six tools in `com.propslab.agent`, reachable through `ToolRegistry` and shown on the
Lab tab. The enforcement lives in the tools, not in the prompt.

| Tool | Capability | Behaviour |
| --- | --- | --- |
| `list_props` | read | Local slate, filtered to what the gate accepted |
| `get_edge` | read | No-vig price, hold, edge, EV and Kelly from `domain/` |
| `stage_ticket` | stage | Writes a proposal (in-memory) + `ticket_staged` audit event. Stake capped at 10 units |
| `confirm_ticket` | stage | Human confirms → one persisted paper bet + `ticket_confirmed` audit event. Consumed on first use |
| `open_book` | handoff | Returns a URL from a three-entry allowlist. **There is no url parameter**, so a prompt-injected link cannot travel through it |
| `submit_live_wager` | submit-live | Always `Blocked`, for any arguments, with the reason and an audit event |

Key files: [agent/Tools.kt](../PropsLab/PropsLab/app/src/main/java/com/propslab/agent/Tools.kt),
[agent/Sportsbook.kt](../PropsLab/PropsLab/app/src/main/java/com/propslab/agent/Sportsbook.kt),
[agent/AgentTools.kt](../PropsLab/PropsLab/app/src/main/java/com/propslab/agent/AgentTools.kt),
[AgentToolsTest.kt](../PropsLab/PropsLab/app/src/test/java/com/propslab/AgentToolsTest.kt).
`PropsRepository` implements the `AgentData` port, `PropsLabApp` builds the registry,
and the Lab tab renders it.

## Why there is no live-submission code

Not a missing feature — an absent interface. There is no endpoint to call, the terms
forbid the automation, and a wager must be placed from a geolocated device inside a
licensed state. An unofficial bot would be a ToS breach that can void wagers and close
accounts, and it could not be verified from here. If real placement is ever wanted, the
route is a commercial arrangement with the operator, not code.

## Verification

- `gradle :app:testDebugUnitTest` — **40 tests, 0 failures** (10 in `AgentToolsTest`).
- `gradle :app:assembleDebug` — APK builds.
- Not verified on a device: the Lab tab's "Agent tools" card has not been run on the
  S25 Ultra.

**Update 2026-10-29 (odds feed):** a read-only odds and box-score feed is now wired
up and the manifest carries `INTERNET` for it. That changes nothing in this
section: both feeds are GETs, neither provider accepts a wager, and
`submit_live_wager` still returns Blocked for every argument set. See
[ODDS-FEED.md](ODDS-FEED.md).

## Credential hygiene

The keys in `D:\docs\opencode api.txt` were pasted into chat for the Kaggle work. They
are **not** written into this repository — the inventory script takes them from
environment variables. Anything in that file you care about (the Stripe live keys, the
GitHub PAT, the AWS access key, the Supabase service-role key, the three Kaggle tokens)
should be treated as exposed and rotated.
