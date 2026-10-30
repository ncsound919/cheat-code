# The odds feed — real prices, real box scores

Written 2026-10-29. Replaces the "settle against the sandbox oracle" stage of the loop with
two read-only providers. The wager problem is unchanged and documented separately in
[BETTING-INTEGRATION.md](BETTING-INTEGRATION.md): none of the three books accepts an
automated wager, so nothing here places one.

## The decision

| Need | Provider | Why that one |
| --- | --- | --- |
| Player prop lines and prices | **The Odds API** | Publishes DraftKings, PrizePicks and Fanatics prop prices; its terms permit display in a mobile app; free plan of 500 credits/month |
| Per-player results | **BALLDONTLIE** | Per-game box scores with `pts`, `reb`, `ast`, `fg3m`, keyed by player id |

Both were verified against their live documentation on 2026-10-29; the endpoint shapes below
are what the parsers in `feed/` are written against, and `FeedTest` feeds each parser the
documented shape.

## The Odds API

```
GET https://api.the-odds-api.com/v4/sports/basketball_nba/events?apiKey=…
GET https://api.the-odds-api.com/v4/sports/basketball_nba/events/{eventId}/odds
      ?apiKey=…&regions=us&oddsFormat=american&dateFormat=iso
      &markets=player_points,player_rebounds,player_assists
      &bookmakers=draftkings,prizepicks
```

- Player props are served **only by the single-event endpoint**, so a sync is one request
  per game. The app caps a sync at 8 games by default (`FeedConfig.maxEvents`).
- Cost is *markets returned × regions*, so asking for three markets on one game is three
  credits. The free plan is 500 credits a month.
- Bookmaker keys: `draftkings` (region `us`), `prizepicks` (region `us_dfs`), `fanatics`
  (`us`, **paid plans only** — the screen says so next to the chip).
- `description` is the player, `name` is `Over`/`Under`, `point` is the line, `price` is
  American odds. `last_update` is per market, and that timestamp is what the app stores as
  the quote time — not the time of the request.
- `x-requests-remaining` is read from the response headers and shown on the Live data screen.
- The `/scores/` endpoint returns **team** scores only, which is why a second provider is
  needed for player props.

## BALLDONTLIE

```
GET https://api.balldontlie.io/v1/games?dates[]=2026-10-28&per_page=100
GET https://api.balldontlie.io/v1/stats?game_ids[]=15907925&per_page=100
```

- Authentication is the raw key in `Authorization:` (no `Bearer`).
- The docs and the OpenAPI spec disagree about the prefix (`/v1` versus `/nba/v1`), so the
  client tries the remembered one first, treats a **404 as "wrong path"**, tries the other,
  and remembers whichever answered. A 401/403 is *not* retried: that is an entitlement
  problem, and the screen says the key was rejected.
- `/stats` (flat per-player rows) needs a paid tier on their side — ALL-STAR or above.
  Box Scores is a more expensive tier still. A free key answers with an error rather than
  empty data, which is why the note on screen distinguishes the two.
- Rate limits are per tier (free is 5 requests/minute), and a sync reads at most 10 finished
  games by default (`FeedConfig.maxResultGames`).

## Joining two feeds that share no id

The odds feed names a player in free text; the box score carries `first_name`/`last_name`.
There is no shared id, so `Names.key` normalises both sides — accents folded, punctuation
dropped, suffixes (`Jr.`, `III`) removed, case and spacing flattened — and that key is the
join. Games are joined on the **US Eastern** date plus normalised team names, because a 10pm
tip-off is already the next day in UTC; `NbaCalendar` is the only place that conversion
happens.

A name that does not survive normalisation stays unmatched. Nothing is fuzzy-matched on a
first initial, because the cost of settling a prop against the wrong player is worse than
leaving a row unsettled.

## What prices a real prop

`GameLogModel` is a normal approximation of the player's own last 20 games:

```
mean, sd  = the player's logged values for that market
sd        = max(sd, 0.30 × mean, 1.0)          // the spread never collapses
half line: over = 1 − Φ((line − mean)/σ)        // no push mass
whole line: over = 1 − Φ((line + 0.5 − mean)/σ)
            push = Φ((line + 0.5 − mean)/σ) − Φ((line − 0.5 − mean)/σ)
            under = Φ((line − 0.5 − mean)/σ)
```

- Fewer than **8 logged games** and no estimate is published. The row keeps its real line
  and price and says why (`INSUFFICIENT_SAMPLE`).
- The floors matter: without them twelve quiet games would price at 99% and the sandbox
  would happily promote a formula that reads noise. A test pins the estimate below 90% on a
  near-degenerate log.
- Integer lines get a real push mass rather than being thrown away or treated as half lines.
- Markets covered: points, rebounds, assists, threes and PRA — every one is a column of the
  box score or a sum of them. Nothing else is claimed.

This is a *baseline*, and it is not evidence of an edge. The estimate is compared to the
book's own no-vig price, exactly like any other prediction, and the promotion gate — not this
file — decides whether the disagreement is worth money.

## What a sync does

1. **Box scores first.** Read the last two days of finished games, store each player's line,
   then settle every open prediction whose game now has a box score. A prediction settles
   once; the row records which stat line it settled from.
2. **Prices second.** Read upcoming games, fetch props for the selected books and markets,
   keep one two-way pair per game/player/market/line — the pair with the **lowest hold**,
   from a single book — and score each against the game log.
3. **Retire the fixtures.** The first sync that writes real quotes marks every synthetic
   prediction unavailable and closes its game. They stay in the ledger with their results;
   they can no longer be traded.
4. **Move the live book.** `refreshLiveBook` settles what finished and lets the promoted
   formula take picks on the new board. It never promotes a formula; that stays the
   sandbox's job, which is what stops a good week at the book from looking like an edge.
5. **Review the live record.** The promotion gate only ever sees a simulated season, so a
   formula can hold a book it has not earned in reality. After **150 settled real picks**
   (synthetic results excluded) the promoted formula is scored on `PromotionGate.review`
   and retired if it is losing. Below 150 a bad run says more about variance than about the
   formula, so nothing moves.

`FeedWorker` runs this every 30 minutes. There is no sync when no keys are saved: the state
row says so and the schedule keeps running.

## Cost of leaving it on

A 30-minute schedule is 48 syncs a day. With the defaults (8 games × 3 markets) that is 24
credits per sync, or ~1,150 a day — far past the free 500 a month, so the defaults are
deliberately *not* a 24/7 configuration. Practical settings for a free key: one market, two
or three games, and a sync on demand. The screen reports the remaining credits after every
call so this cannot be a surprise. Box-score reads are bounded the same way by
`maxResultGames`.

## Verification

- `gradle :app:testDebugUnitTest` — **86 tests, 0 failures**, 21 of them in `FeedTest`:
  name and team joining, both parsers against the documented payload shapes, one-sided props
  and unpriced markets dropped, cheapest-hold selection, the model's probabilities against
  hand arithmetic, the spread floors, push mass on integer lines, and the estimate agreeing
  with `Settlement`.
- `gradle :app:assembleDebug` — APK builds (68 MB).
- The Room 3 migration SQL was compared column for column against the Room-generated
  `app/schemas/com.propslab.data.PropsDatabase/3.json`, including the `DEFAULT ''` the
  migration adds to the two new `games` columns.

**Not verified:** no live key was used from here, so no real payload has been fetched, no
sync has been run end to end against the providers, the 2→3 migration has not been exercised
on a device holding a v2 database, and the Live data screen has not been run on the S25
Ultra. The parsers are tested against the documented shapes, which is not the same as tested
against what the providers send today.
