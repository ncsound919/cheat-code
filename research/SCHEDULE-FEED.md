# The schedule feed — games and rosters before prices

Written 2026-10-29. Adds **TheSportsDB** as a third read-only provider. The odds and
box-score feeds are unchanged and documented in [ODDS-FEED.md](ODDS-FEED.md); this file
covers only what came in with TheSportsDB and the free-tier behaviour that shaped it.

## The decision

| Need | Provider | Why that one |
| --- | --- | --- |
| Game schedule and team rosters | **TheSportsDB v1** | No key required (published free key), returns start times as UTC stamps, and answers a roster by team id |

A prediction row needs a game row and a player row before a price can be attached to
either. Until now those rows existed only as invented fixtures or as by-products of an
odds sync, so the schedule runs **before the keyed steps and even when no key is saved**:
the difference between a board waiting for a key and a board with nothing to attach a
key to.

## Endpoints (verified live 2026-10-29)

```
GET /api/v1/json/123/eventsday.php?d=2026-10-29&s=Basketball
GET /api/v1/json/123/eventsseason.php?id=4387&s=2026-2027
GET /api/v1/json/123/eventsnext.php?id=134867
GET /api/v1/json/123/lookup_all_players.php?id=134867
GET /api/v1/json/123/searchteams.php?t=Los+Angeles+Lakers
```

- `s=` is the **sport** name (`Basketball`), not the league; the NBA is filtered client
  side from `strLeague == "NBA"` (league id `4387`).
- `strTimestamp` is UTC with no marker: a 7:30pm tip in Los Angeles arrives as the next
  day's `02:30`. Read as UTC; the US-Eastern date conversion stays in `NbaCalendar`.
  A row with no readable time yields `0` and is dropped from every window rather than
  landing at the epoch.
- Envelopes differ per endpoint: `eventsday`/`eventsnext` answer under `events`,
  `eventslast` under `results`, rosters under `player`. An empty day answers
  `"events": null`, which is read as zero games, not an error.
- The free key is a published constant (`123`), not a credential: it is code, not a
  preference, and no URL this client builds reaches a log line — socket exceptions are
  replaced with a sentence before a caller can echo them.

## What the free tier actually returns (measured, not assumed)

| Read | Observed 2026-10-29 |
| --- | --- |
| `eventsday`, three different dates | exactly 3 basketball events each, **no NBA** |
| `eventsseason` (NBA, 2026-2027) | 15 events, all 2026-10-03 → 10-08 (already finished) |
| `eventsnext` (Lakers) | the real next game, in the window |
| `lookup_all_players` (Lakers) | 10 players |
| `searchteams` (Arsenal / Los Angeles Lakers) | correct team, one row |

Because each call counts against a quota, `FeedSync.syncSchedule` reads in stages and
stops once the window has games:

1. **today + tomorrow** — the board's own window;
2. **the season page** — because both day queries came back with no NBA game;
3. **the per-team schedule** for up to 4 candidate teams — because on the day this was
   built, the season page held only early-October games while four `eventsnext` probes
   found the games actually being played that night.

Candidate team ids come from every NBA event read along the way, even out-of-window ones.
Reads are paced 1s apart, and a 429 is reported as a rate limit rather than retried.

## What a populate writes

- `games` — `tsdb:<idEvent>`, real `startUtc`, both full team names, an abbreviation from
  `Teams.abbreviation`, status `FINAL`/`SCHEDULED`.
- `players` — `Names.playerId(name)` (the same key the odds feed derives), so a roster
  import and a later price attach to the same person.
- One audit row, kind `schedule_populated`, and a note shown on the Live data screen.

Writes are insert-if-absent: a game the odds feed already created keeps its own id, and a
roster row never overwrites a player the box scores filed.

**What it does not do:** it carries no prices and no stats. A game with no quote has no
prediction, so the board still needs the odds key to show anything new — the schedule is
the substrate, not the board.

## Verification

- `gradle :app:testDebugUnitTest` — **109 tests, 0 failures**, 12 of them in
  `SportsDbTest`: both day and season parsing against the payloads above, the UTC stamp
  (pinned to the exact epoch), the date/time fallback, a row with no readable time staying
  out of every window, both envelope keys, `events: null`, roster parsing, URL shapes and
  the season label crossing the calendar year.
- `gradle :app:assembleDebug` — APK builds.
- **Live:** the staged read sequence was replayed against the real API from this machine —
  day reads found 0 NBA games, the season fallback 0 in-window, and the four `eventsnext`
  probes found **4 real NBA games (2 finished, 2 scheduled) → 40 players across 4
  rosters**, which is exactly what `syncSchedule` writes.

**Not verified:** the sync has not run on the device (the phone was off adb), so the Room
writes themselves are exercised only by the compiled path and the parser tests above.
