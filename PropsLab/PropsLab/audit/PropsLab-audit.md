# Android Audit Report — PropsLab

- Snapshot: #14
- Upload: PropsLab-source-final.zip
- Kind: Source ZIP
- Version: audit-final
- Scanned: 2026-10-30T01:24:50-04:00
- SHA-256: `6bd184c262af25e6b0774062cc7ea35fc409fe468ec68b36b32571ea1e392c95`
- Heuristic score: 74/100

> Score is a heuristic, not a security certification. ‘No finding detected’ is not proof of safety.

## Deterministic findings

### [MEDIUM] DEBT-002 — Oversized unit: ParlayCandidate
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:1
- Message: 699 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: ParlaySeed
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1
- Message: 1990 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: FantasyLine
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:1
- Message: 503 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: NewsItem
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:1
- Message: 450 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: FeedSync
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:1
- Message: 996 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: LossObservation
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:1
- Message: 487 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: BestViewModel
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:1
- Message: 580 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: DetailViewModel
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:1
- Message: 417 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: PlayerLogUi
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:1
- Message: 853 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: FeedScreenState
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:1
- Message: 615 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: LabViewModel
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:1
- Message: 885 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: LensesViewModel
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:1
- Message: 516 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: ModelShopScreen
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:1
- Message: 491 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: ParlayViewModel
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:1
- Message: 622 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: SandboxScreenState
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:1
- Message: 499 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: SlateState
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:1
- Message: 430 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-002 — Oversized unit: ParlayLabTest
- Location: `app/src/test/java/com/propslab/ParlayLabTest.kt`:1
- Message: 493 lines in one file; the unit likely owns several responsibilities.
- Suggested action: Split by responsibility (data access, policy, presentation) before adding behaviour.
- Source: brooks-debt

### [MEDIUM] DEBT-003 — Deprecated API: onBackPressed
- Location: `app/src/main/java/com/propslab/ui/screens/ManualActivity.kt`:50
- Message: onBackPressed is deprecated and will not survive the next platform bump.
- Suggested action: Use OnBackPressedDispatcher.
- Source: brooks-debt

### [MEDIUM] DEBT-004 — Mutable singleton state: Licensing
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:164
- Message: Licensing is an object holding mutable vars — global state with no owner.
- Suggested action: Move state behind an injected repository/state holder.
- Source: brooks-debt

### [MEDIUM] DEBT-004 — Mutable singleton state: ParlayStaking
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:119
- Message: ParlayStaking is an object holding mutable vars — global state with no owner.
- Suggested action: Move state behind an injected repository/state holder.
- Source: brooks-debt

### [MEDIUM] DEBT-004 — Mutable singleton state: SandboxHash
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:219
- Message: SandboxHash is an object holding mutable vars — global state with no owner.
- Suggested action: Move state behind an injected repository/state holder.
- Source: brooks-debt

### [MEDIUM] DEBT-005 — Talk-to-many class: ParlaySeed
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:67
- Message: ParlaySeed exposes 19 public functions.
- Suggested action: Split by caller: each public surface should belong to one responsibility.
- Source: brooks-debt

### [MEDIUM] DEBT-005 — Talk-to-many class: PropsRepository
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:76
- Message: PropsRepository exposes 19 public functions.
- Suggested action: Split by caller: each public surface should belong to one responsibility.
- Source: brooks-debt

### [MEDIUM] DEBT-005 — Talk-to-many class: FeedScreenState
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:53
- Message: FeedScreenState exposes 17 public functions.
- Suggested action: Split by caller: each public surface should belong to one responsibility.
- Source: brooks-debt

### [MEDIUM] DEBT-005 — Talk-to-many class: FeedViewModel
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:74
- Message: FeedViewModel exposes 17 public functions.
- Suggested action: Split by caller: each public surface should belong to one responsibility.
- Source: brooks-debt

### [MEDIUM] DEBT-005 — Talk-to-many class: FeedTest
- Location: `app/src/test/java/com/propslab/FeedTest.kt`:30
- Message: FeedTest exposes 20 public functions.
- Suggested action: Split by caller: each public surface should belong to one responsibility.
- Source: brooks-debt

### [MEDIUM] DEBT-005 — Talk-to-many class: SandboxTest
- Location: `app/src/test/java/com/propslab/SandboxTest.kt`:28
- Message: SandboxTest exposes 16 public functions.
- Suggested action: Split by caller: each public surface should belong to one responsibility.
- Source: brooks-debt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:62
- Message: This condition is too complex (8). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:106
- Message: This condition is too complex (5). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:112
- Message: This condition is too complex (5). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:216
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:410
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:847
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1905
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:192
- Message: This condition is too complex (6). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:116
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:92
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:72
- Message: This condition is too complex (12). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.ComplexCondition — detekt: detekt.complexity.ComplexCondition
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:232
- Message: This condition is too complex (4). Defined complexity threshold for conditions is set to '4'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:184
- Message: The function run appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:44
- Message: The function route appears to be too complex based on Cyclomatic Complexity (complexity: 43). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:61
- Message: The function parse appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:56
- Message: The function compile appears to be too complex based on Cyclomatic Complexity (complexity: 27). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:40
- Message: The function build appears to be too complex based on Cyclomatic Complexity (complexity: 30). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:75
- Message: The function build appears to be too complex based on Cyclomatic Complexity (complexity: 18). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:150
- Message: The function pickOf appears to be too complex based on Cyclomatic Complexity (complexity: 23). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/LearningSettlement.kt`:22
- Message: The function game appears to be too complex based on Cyclomatic Complexity (complexity: 18). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:259
- Message: The function build appears to be too complex based on Cyclomatic Complexity (complexity: 30). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:403
- Message: The function priceLeg appears to be too complex based on Cyclomatic Complexity (complexity: 24). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:450
- Message: The function explain appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:530
- Message: The function describe appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:584
- Message: The function compose appears to be too complex based on Cyclomatic Complexity (complexity: 30). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:366
- Message: The function setFormula appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:600
- Message: The function runDeepTest appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:725
- Message: The function refreshLiveBook appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1450
- Message: The function runSandboxRound appears to be too complex based on Cyclomatic Complexity (complexity: 54). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:59
- Message: The function fit appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:83
- Message: The function reprice appears to be too complex based on Cyclomatic Complexity (complexity: 26). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:185
- Message: The function curveOf appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:334
- Message: The function advise appears to be too complex based on Cyclomatic Complexity (complexity: 25). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:273
- Message: The function parseInjuries appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:326
- Message: The function parseNflBoxScore appears to be too complex based on Cyclomatic Complexity (complexity: 31). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:96
- Message: The function readSeason appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:204
- Message: The function statLine appears to be too complex based on Cyclomatic Complexity (complexity: 20). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:25
- Message: The function parseGames appears to be too complex based on Cyclomatic Complexity (complexity: 20). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:69
- Message: The function parseBoxScore appears to be too complex based on Cyclomatic Complexity (complexity: 38). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:180
- Message: The function syncSchedule appears to be too complex based on Cyclomatic Complexity (complexity: 25). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:281
- Message: The function syncResults appears to be too complex based on Cyclomatic Complexity (complexity: 19). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:348
- Message: The function syncEspnResults appears to be too complex based on Cyclomatic Complexity (complexity: 20). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:511
- Message: The function syncSignals appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:667
- Message: The function syncProps appears to be too complex based on Cyclomatic Complexity (complexity: 26). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:808
- Message: The function buildBoard appears to be too complex based on Cyclomatic Complexity (complexity: 20). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:129
- Message: The function parseGameLines appears to be too complex based on Cyclomatic Complexity (complexity: 29). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:166
- Message: The function parseEventOdds appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:188
- Message: The function pairOutcomes appears to be too complex based on Cyclomatic Complexity (complexity: 23). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:79
- Message: The function parseStats appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:191
- Message: The function download appears to be too complex based on Cyclomatic Complexity (complexity: 28). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:129
- Message: The function download appears to be too complex based on Cyclomatic Complexity (complexity: 35). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:122
- Message: The function analyze appears to be too complex based on Cyclomatic Complexity (complexity: 59). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/sim/SandboxEngine.kt`:34
- Message: The function decide appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/sim/SandboxEngine.kt`:62
- Message: The function run appears to be too complex based on Cyclomatic Complexity (complexity: 18). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:46
- Message: The function parse appears to be too complex based on Cyclomatic Complexity (complexity: 27). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/components/AutopilotCard.kt`:35
- Message: The function AutopilotCard appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:36
- Message: The function AccountScreen appears to be too complex based on Cyclomatic Complexity (complexity: 18). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:252
- Message: The function BestRow appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:604
- Message: The function GameLogPanel appears to be too complex based on Cyclomatic Complexity (complexity: 31). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:176
- Message: The function buildFlyer appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:111
- Message: The function ImportScreen appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:534
- Message: The function LearningCard appears to be too complex based on Cyclomatic Complexity (complexity: 30). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:221
- Message: The function ClaimCard appears to be too complex based on Cyclomatic Complexity (complexity: 23). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:50
- Message: The function ModelShopScreen appears to be too complex based on Cyclomatic Complexity (complexity: 24). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:91
- Message: The function NewsScreen appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:41
- Message: The function PremiumScreen appears to be too complex based on Cyclomatic Complexity (complexity: 18). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:346
- Message: The function FormulaRow appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.CyclomaticComplexMethod — detekt: detekt.complexity.CyclomaticComplexMethod
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:42
- Message: The function UpdatesScreen appears to be too complex based on Cyclomatic Complexity (complexity: 20). Defined complexity threshold for methods is set to '15'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LargeClass — detekt: detekt.complexity.LargeClass
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:76
- Message: Class PropsRepository is too large. Consider splitting it into smaller pieces.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LargeClass — detekt: detekt.complexity.LargeClass
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:62
- Message: Class FeedSync is too large. Consider splitting it into smaller pieces.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:44
- Message: The function route is too long (68). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:56
- Message: The function compile is too long (74). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:40
- Message: The function build is too long (109). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:33
- Message: The function seed is too long (94). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:259
- Message: The function build is too long (64). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:584
- Message: The function compose is too long (74). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:41
- Message: The function migrate is too long (69). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:506
- Message: The function createClaim is too long (83). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:600
- Message: The function runDeepTest is too long (61). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:903
- Message: The function importSlip is too long (80). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1018
- Message: The function learning is too long (69). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1188
- Message: The function logLearningBoard is too long (65). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1450
- Message: The function runSandboxRound is too long (171). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:334
- Message: The function advise is too long (75). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:158
- Message: The function fetch is too long (61). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:326
- Message: The function parseNflBoxScore is too long (73). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:96
- Message: The function readSeason is too long (69). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:180
- Message: The function syncSchedule is too long (75). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:511
- Message: The function syncSignals is too long (82). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:667
- Message: The function syncProps is too long (108). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:808
- Message: The function buildBoard is too long (123). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:62
- Message: The function render is too long (81). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:191
- Message: The function download is too long (62). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:129
- Message: The function download is too long (81). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:122
- Message: The function analyze is too long (202). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/sim/SandboxEngine.kt`:62
- Message: The function run is too long (73). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:70
- Message: The function AppNav is too long (124). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:36
- Message: The function AccountScreen is too long (149). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:92
- Message: The function BestScreen is too long (71). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:252
- Message: The function BestRow is too long (100). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:367
- Message: The function BetToPlace is too long (60). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:244
- Message: The function FantasyScreen is too long (185). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:495
- Message: The function PlayerCard is too long (95). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:604
- Message: The function GameLogPanel is too long (108). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:266
- Message: The function FeedScreen is too long (224). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:53
- Message: The function FlyerScreen is too long (118). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:176
- Message: The function buildFlyer is too long (72). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:111
- Message: The function ImportScreen is too long (148). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:150
- Message: The function LabScreen is too long (91). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:324
- Message: The function StudioCard is too long (90). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:534
- Message: The function LearningCard is too long (122). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:221
- Message: The function ClaimCard is too long (127). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:350
- Message: The function SystemCard is too long (76). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:50
- Message: The function ModelShopScreen is too long (248). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:405
- Message: The function CloudCard is too long (68). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:91
- Message: The function NewsScreen is too long (131). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:258
- Message: The function PushPanel is too long (68). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:328
- Message: The function ParlayCard is too long (114). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:230
- Message: The function BetSlip is too long (68). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:41
- Message: The function PremiumScreen is too long (194). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:346
- Message: The function FormulaRow is too long (61). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:44
- Message: The function SettingsScreen is too long (224). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:34
- Message: The function SignalsCard is too long (80). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongMethod — detekt: detekt.complexity.LongMethod
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:42
- Message: The function UpdatesScreen is too long (181). The maximum length is 60.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/agent/Tools.kt`:175
- Message: The function stage(predictionId: String, playerName: String, book: Sportsbook, side: Side, line: Double, odds: Int, stakeUnits: Double, edgePts: Double?) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:450
- Message: The function explain(size: Int, legs: List<ParlayLegUi>, games: Int, probability: Double, decimal: Double, anchor: ParlayCandidate, propLegs: Int, gameLegs: Int, options: ParlayOptions) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1767
- Message: The function measure(prop: PropUi, side: Side, odds: Int, edgePts: Double, live: Boolean, shadow: Boolean) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:83
- Message: The function reprice(quotedOdds: Int, line: Double, side: Side, points: Double, overProb: Double, pushProb: Double, mean: Double?, spread: Double?, meanMultiplier: Double) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:106
- Message: The function evaluate(line: Double?, quoteTimeUtc: Long, featureCutoffUtc: Long, nowUtc: Long, playerMatched: Boolean, market: String, maxQuoteAgeMs: Long, maxFeatureAgeMs: Long, validatedMarkets: Set<String>) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:247
- Message: The function footballScore(passYards: Int, passTouchdowns: Int, interceptions: Int, rushYards: Int, rushTouchdowns: Int, receptions: Int, receivingYards: Int, receivingTouchdowns: Int, fumblesLost: Int) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:305
- Message: The function build(sport: FantasySport, lines: List<FantasyLine>, injuries: List<FantasyInjury>, watchlist: Set<String>, fetchedUtc: Long, note: String) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:204
- Message: The function statLine(sport: FantasySport, value: (String) -> String, has: (String) -> Boolean, names: List<String>, display: List<String>, stats: JSONArray) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:62
- Message: The constructor(db: PropsDatabase, settings: FeedSettings, assistant: LocalAssistant, fetcher: JsonFetcher, sportsDb: SportsDb, news: InjuryNews, league: LeagueInjuries, champion: ChampionTable?, clock: () -> Long) has too many parameters. The current threshold is set to 7.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:945
- Message: The function writeState(props: Step, results: Step, signals: Step, now: Long, credits: Int?, quotes: Int, scored: Int, settled: Int, fixturesRetired: Boolean?) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:81
- Message: The function score(values: List<Double>, line: Double, availability: Double, market: String, table: ChampionTable?, calibration: PlattFit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:188
- Message: The function pairOutcomes(eventId: String, market: String, bookKey: String, bookTitle: String, updatedUtc: Long, outcomes: JSONArray) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:173
- Message: The function drawWrapped(canvas: Canvas, value: String, x: Float, top: Float, width: Float, size: Float, lineHeight: Float, color: Int, face: Typeface, bold: Boolean) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:206
- Message: The function text(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, face: Typeface, align: Paint.Align, letterSpacing: Float, bold: Boolean) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:23
- Message: The constructor(bucketWidth: Double, kmax: Int, cdf: List<DoubleArray>, fitRows: Int, fitThrough: String, sourceDataset: String, sha256: String) has too many parameters. The current threshold is set to 7.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/inference/Scorer.kt`:20
- Message: The constructor(schemaVersion: Int, features: List<String>, mean: DoubleArray, scale: DoubleArray, coef: DoubleArray, intercept: Double, plattA: Double, plattB: Double) has too many parameters. The current threshold is set to 7.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/mathx/LlamaCppModel.kt`:175
- Message: The function generate(handle: Long, prompt: String, maxTokens: Int, temperature: Float, topK: Int, topP: Float, seed: Int) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:63
- Message: The function plan(season: List<SimProp>, incumbent: String?, thresholds: GateThresholds, grid: List<StrategyParams>, challengers: Set<String>, vetoed: Set<String>) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:438
- Message: The function finding(id: String, dimension: String, kind: InsightKind, title: String, stats: Stats, advice: String, proposal: StrategyParams?) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:49
- Message: The function SplitRing(over: Float, push: Float, market: Float?, modifier: Modifier, strokeWidth: Dp, content: @Composable () -> Unit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:92
- Message: The function BestScreen(onOpen: (String) -> Unit, onOpenSandbox: () -> Unit, onOpenPlain: () -> Unit, onOpenLenses: () -> Unit, onOpenParlay: () -> Unit, onOpenFeed: () -> Unit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:495
- Message: The function PlayerCard(player: FantasyPlayer, tracked: Boolean, onToggle: () -> Unit, log: PlayerLogUi?, onToggleLog: () -> Unit, onSelectLogSeason: (String) -> Unit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:150
- Message: The function LabScreen(onOpenImport: () -> Unit, onOpenFeed: () -> Unit, onOpenConsole: () -> Unit, onOpenSettings: () -> Unit, onOpenPlain: () -> Unit, onOpenLenses: () -> Unit, onOpenModels: () -> Unit, onOpenPremium: () -> Unit, onOpenUpdates: () -> Unit, onOpenFlyer: () -> Unit, onOpenFantasy: () -> Unit, onOpenNews: () -> Unit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:257
- Message: The function LabHeader(passed: Int, total: Int, formulas: Int, easy: Boolean, onOpenSettings: () -> Unit, onOpenPlain: () -> Unit, onOpenLenses: () -> Unit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:534
- Message: The function LearningCard(learning: LearningUi, busy: Boolean, message: String?, provenance: String?, onLearn: () -> Unit, easy: Boolean) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:335
- Message: The function Stepper(label: String, value: Int, unit: String, range: IntRange, step: Int, onChange: (Int) -> Unit) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/test/java/com/propslab/AgentToolsTest.kt`:50
- Message: The function prop(id: String, playerName: String, over: Int?, under: Int?, pOver: Double?, pPush: Double?, pUnder: Double?, line: Double?, fixture: Boolean) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/test/java/com/propslab/GameModelTest.kt`:19
- Message: The function game(id: String, team: String, opponent: String, scored: Int, allowed: Int, at: Long) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/test/java/com/propslab/LossInsightsTest.kt`:31
- Message: The function observation(side: Side, outcome: Outcome, market: String, odds: Int, edge: Double?, line: Double) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.LongParameterList — detekt: detekt.complexity.LongParameterList
- Location: `app/src/test/java/com/propslab/ParlayLabTest.kt`:423
- Message: The function candidate(key: String, game: String, subject: String, odds: Int, pWin: Double, edge: Double) has too many parameters. The current threshold is set to 6.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:326
- Message: Function parseNflBoxScore is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:69
- Message: Function parseBoxScore is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:140
- Message: Function namesIn is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:129
- Message: Function parseGameLines is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:129
- Message: Function fromJson is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:46
- Message: Function parse is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.NestedBlockDepth — detekt: detekt.complexity.NestedBlockDepth
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:145
- Message: Function kellyIsZeroAtFairAndNegativeBelow is nested too deeply.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:26
- Message: Class 'KaggleClient' with '17' functions detected. Defined threshold inside classes is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/Daos.kt`:83
- Message: Interface 'SlateDao' with '13' functions detected. Defined threshold inside interfaces is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/FeedDao.kt`:32
- Message: Interface 'FeedDao' with '32' functions detected. Defined threshold inside interfaces is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/LearningDao.kt`:19
- Message: Interface 'LearningDao' with '12' functions detected. Defined threshold inside interfaces is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:178
- Message: Object 'ParlayTickets' with '14' functions detected. Defined threshold inside objects is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\data\PropsRepository.kt' with '12' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:76
- Message: Class 'PropsRepository' with '65' functions detected. Defined threshold inside classes is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/SandboxDao.kt`:10
- Message: Interface 'SandboxDao' with '15' functions detected. Defined threshold inside interfaces is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/data/SignalDao.kt`:10
- Message: Interface 'SignalDao' with '11' functions detected. Defined threshold inside interfaces is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:62
- Message: Class 'FeedSync' with '16' functions detected. Defined threshold inside classes is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:59
- Message: Class 'SportsDb' with '15' functions detected. Defined threshold inside classes is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:51
- Message: Object 'ModelHub' with '14' functions detected. Defined threshold inside objects is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:76
- Message: Object 'LossInsights' with '15' functions detected. Defined threshold inside objects is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\components\Components.kt' with '15' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\BestScreen.kt' with '11' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\DetailScreen.kt' with '18' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\FantasyScreen.kt' with '12' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:74
- Message: Class 'FeedViewModel' with '19' functions detected. Defined threshold inside classes is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\LabScreen.kt' with '18' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\LensesScreen.kt' with '14' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\ParlayScreen.kt' with '12' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.complexity.TooManyFunctions — detekt: detekt.complexity.TooManyFunctions
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:1
- Message: File 'C:\Users\tap45\AppData\Local\Temp\android-audit-desk-e3hw8zei\source\app\src\main\java\com\propslab\ui\screens\SlateScreen.kt' with '14' functions detected. Defined threshold inside files is set to '11'
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.empty-blocks.EmptyElseBlock — detekt: detekt.empty-blocks.EmptyElseBlock
- Location: `app/src/main/java/com/propslab/agent/Tools.kt`:69
- Message: This else block is empty and can be removed.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/account/Account.kt`:98
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/account/Account.kt`:131
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:257
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:209
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:109
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/ocr/ScreenshotOcr.kt`:29
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/work/FeedWorker.kt`:36
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/work/FreshnessWorker.kt`:19
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/work/LearningWorker.kt`:27
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.exceptions.TooGenericExceptionCaught — detekt: detekt.exceptions.TooGenericExceptionCaught
- Location: `app/src/main/java/com/propslab/work/SandboxWorker.kt`:29
- Message: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:70
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:201
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/AutopilotCard.kt`:35
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/AutopilotCard.kt`:98
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:49
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:94
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:123
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:139
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:162
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:214
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:228
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:242
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:261
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:286
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:296
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:322
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:341
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:358
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:86
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:103
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:123
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:143
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/components/PremiumGate.kt`:26
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:36
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:193
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:53
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:84
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:92
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:174
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:206
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:252
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:367
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:443
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:469
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:528
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:545
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:94
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:152
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:178
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:200
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:210
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:265
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:285
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:83
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:116
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:125
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:137
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:174
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:182
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:187
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:207
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:215
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:230
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:278
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:302
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:317
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:346
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:364
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:378
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:403
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:244
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:442
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:486
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:495
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:604
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:719
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:776
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:793
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:827
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:266
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:507
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:551
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:596
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:53
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:111
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:267
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:150
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:257
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:305
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:324
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:416
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:441
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:471
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:500
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:534
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:676
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:701
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:744
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:782
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:804
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:848
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:857
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:871
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:99
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:149
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:162
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:177
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:186
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:205
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:221
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:350
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:428
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:437
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:447
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:498
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:50
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:332
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:359
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:405
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:476
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:91
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:232
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:239
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:279
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:108
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:152
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:187
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:225
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:258
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:328
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:452
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:512
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:560
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:580
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:602
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt`:296
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt`:345
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:84
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:136
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:189
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:205
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:230
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:300
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:315
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:328
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:41
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:244
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:94
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:156
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:214
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:234
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:292
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:346
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:409
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:435
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:470
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:44
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:286
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:295
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:314
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:325
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:335
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:361
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:372
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:34
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:122
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:105
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:142
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:206
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:254
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:264
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:269
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:287
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:294
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:310
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:352
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:388
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:416
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:42
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:231
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.FunctionNaming — detekt: detekt.naming.FunctionNaming
- Location: `app/src/main/java/com/propslab/ui/theme/Theme.kt`:18
- Message: Function names should match the pattern: [a-z][a-zA-Z0-9]*
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:23
- Message: The file name 'GameLog' does not match the name of the single top-level declaration 'FantasyGameLogApi'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:30
- Message: The file name 'AuditScreen' does not match the name of the single top-level declaration 'AuditViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:61
- Message: The file name 'BestScreen' does not match the name of the single top-level declaration 'BestViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:51
- Message: The file name 'ConsoleScreen' does not match the name of the single top-level declaration 'ConsoleViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:63
- Message: The file name 'DetailScreen' does not match the name of the single top-level declaration 'DetailViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:69
- Message: The file name 'LabScreen' does not match the name of the single top-level declaration 'LabViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:50
- Message: The file name 'LensesScreen' does not match the name of the single top-level declaration 'LensesViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:44
- Message: The file name 'NewsScreen' does not match the name of the single top-level declaration 'NewsViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:66
- Message: The file name 'ParlayScreen' does not match the name of the single top-level declaration 'ParlayViewModel'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.naming.MatchingDeclarationName — detekt: detekt.naming.MatchingDeclarationName
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:9
- Message: The file name 'Color' does not match the name of the single top-level declaration 'Palette'.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:78
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:99
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:65
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:69
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:157
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:189
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:276
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:286
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:875
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1263
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1297
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1703
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:71
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:277
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:281
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:331
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:335
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:339
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:190
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:29
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:74
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:78
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:84
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:313
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:356
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:378
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:415
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:429
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:463
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:542
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:701
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:110
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:135
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:140
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:146
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:171
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:176
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:198
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:216
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:60
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:82
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:85
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:112
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:140
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:164
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:99
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:302
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.LoopWithTooManyJumpStatements — detekt: detekt.style.LoopWithTooManyJumpStatements
- Location: `app/src/main/java/com/propslab/sim/SandboxEngine.kt`:82
- Message: The loop contains more than one break or continue statement. The code should be refactored to increase readability.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/account/Account.kt`:91
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/account/Account.kt`:110
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/account/Account.kt`:119
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/account/Account.kt`:144
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/account/Account.kt`:145
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/account/Account.kt`:154
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:82
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:90
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:140
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:141
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:142
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:143
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:144
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:145
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:150
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:115
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:138
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/Availability.kt`:75
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/Availability.kt`:76
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/Availability.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/Availability.kt`:88
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:39
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:51
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:52
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:25
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:26
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:27
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:28
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:29
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:30
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:31
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:32
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:33
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:34
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:35
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:38
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:71
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:87
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:51
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:52
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:75
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:105
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:193
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:201
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:211
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimBench.kt`:61
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimBench.kt`:63
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimBench.kt`:118
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimBench.kt`:124
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:195
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:198
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimModels.kt`:56
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/ClaimModels.kt`:57
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:34
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:69
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:99
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:57
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:207
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:208
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:209
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:118
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:21
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:35
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:36
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:37
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:38
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:41
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:44
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:46
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:49
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:51
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:54
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:55
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:58
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:59
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:60
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:96
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:105
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:106
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:107
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:112
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:114
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:127
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:142
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:143
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:158
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:237
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:180
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:272
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:372
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:410
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:428
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:434
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:490
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:509
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:512
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:515
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:516
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:517
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:544
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:602
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:637
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:638
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:639
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:655
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:117
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:181
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:228
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:246
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:284
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:310
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:366
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:95
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:104
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:119
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:472
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:478
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:508
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:577
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:645
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:646
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:709
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:911
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1053
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1301
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1304
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1305
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1354
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1359
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1395
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1413
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1414
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1725
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1967
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1968
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/SandboxUi.kt`:95
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/data/SandboxUi.kt`:144
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/BetNarrative.kt`:64
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/BetNarrative.kt`:86
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:64
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:76
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:85
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:97
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:102
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:125
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:126
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:136
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:49
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:50
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:51
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:52
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:53
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:54
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:55
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:63
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:64
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:78
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:82
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:94
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:100
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Formulas.kt`:28
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Formulas.kt`:51
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Formulas.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Formulas.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:69
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:70
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:89
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:153
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:154
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:155
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:196
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:202
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:207
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:208
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:218
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:221
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:222
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:244
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:249
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:255
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:256
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:103
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:120
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:151
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:152
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:153
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:175
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:176
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:178
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:179
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:180
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:195
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:196
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:197
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:201
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:202
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:203
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:209
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:210
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:211
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:213
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:219
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:223
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:227
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:228
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:229
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:230
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:240
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:241
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:8
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:9
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:36
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:40
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:113
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:114
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:90
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:141
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:151
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:152
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:158
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:168
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:66
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:79
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:80
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:82
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:92
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:93
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:110
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:111
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:245
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:258
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:259
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:261
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:262
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:263
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:264
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:265
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:274
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:296
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:319
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:322
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:335
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:380
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:381
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:402
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:407
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:408
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:426
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:434
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:65
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:116
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:246
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:349
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:351
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:352
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:355
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:357
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:360
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:363
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:434
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:212
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:239
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:242
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedBoard.kt`:20
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedConfig.kt`:132
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedConfig.kt`:133
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedConfig.kt`:137
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedConfig.kt`:138
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedConfig.kt`:139
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedConfig.kt`:140
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedHttp.kt`:10
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedHttp.kt`:58
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedHttp.kt`:79
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedHttp.kt`:80
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedHttp.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:182
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:187
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:188
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:189
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:285
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:351
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:414
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:437
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:513
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:540
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:550
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:580
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:622
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:684
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:688
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:116
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:117
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:161
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:166
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:167
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/Names.kt`:59
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:244
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:94
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:113
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:204
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:82
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:85
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:86
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:92
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:95
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:96
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:102
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:103
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:104
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:106
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:107
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:108
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:109
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:112
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:113
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:115
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:119
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:124
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:125
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:126
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:127
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:128
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:129
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:133
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:134
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:135
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:136
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:139
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:140
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:141
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:144
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:145
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:147
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:151
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:161
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:166
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:168
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:170
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:182
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:223
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:260
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:271
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:34
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:35
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:36
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:49
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:50
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:51
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:67
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:70
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:93
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:94
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:95
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/DeviceProfile.kt`:96
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:33
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:136
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:159
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:216
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:220
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:227
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:237
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:301
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:314
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:315
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:326
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:340
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:341
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:82
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:112
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:114
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:152
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:153
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:162
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:165
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:173
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:176
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:251
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:255
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:256
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:262
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:38
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:46
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:47
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:71
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:109
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:36
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:72
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:93
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/CloudModel.kt`:95
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/DeviceAssistant.kt`:20
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:60
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/LocalAssistant.kt`:25
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/LocalAssistant.kt`:29
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/Scorer.kt`:37
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/inference/Scorer.kt`:46
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:45
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:46
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:48
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:49
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:89
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:94
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:98
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:124
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:127
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:135
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:140
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:143
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:144
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:164
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:170
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:171
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:172
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:173
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:174
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:175
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:185
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:186
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:192
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:196
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:197
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:199
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:207
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:224
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:225
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:229
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:230
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:241
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:242
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:243
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:244
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/LlamaCppModel.kt`:109
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:44
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:66
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:69
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:70
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:71
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:72
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:123
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:127
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:156
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:160
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:64
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:65
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:66
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:67
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:69
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:106
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:107
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:108
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/settings/AppSettings.kt`:109
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:23
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:151
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:152
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:154
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:181
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:89
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:90
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:91
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:100
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:101
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:258
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:263
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:287
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:331
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:334
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:362
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:363
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:383
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:384
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:388
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:389
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:391
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:392
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:396
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:403
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:404
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:483
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/PromotionGate.kt`:99
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/PromotionGate.kt`:107
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxEngine.kt`:47
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:93
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:104
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:126
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:147
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:148
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:233
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:234
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:235
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:239
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:244
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxTruth.kt`:39
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxTruth.kt`:43
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxTruth.kt`:56
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxTruth.kt`:70
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/SandboxTruth.kt`:74
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:37
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:38
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:39
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:40
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:41
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:42
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:43
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:44
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:45
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:61
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:75
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:85
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:91
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:92
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:93
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/sim/Season.kt`:95
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:64
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:65
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:66
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:175
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:177
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Format.kt`:31
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Format.kt`:45
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Format.kt`:48
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Format.kt`:49
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:80
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:82
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:227
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:230
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:58
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:59
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:61
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:62
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:70
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:72
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:73
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:75
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:77
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:79
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:81
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:96
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:97
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:102
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:124
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:125
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:126
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:130
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:172
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:343
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:346
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:347
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:360
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:361
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:52
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:55
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:68
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:92
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:131
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:151
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:162
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:163
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:164
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:32
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:63
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:66
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:256
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:278
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:318
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:521
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:522
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:523
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:574
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:64
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:243
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:244
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:245
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:247
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:280
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:285
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:286
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:288
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:289
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:325
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:326
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:331
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:332
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:335
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:383
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:125
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:546
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:755
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:84
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:138
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:400
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:413
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:181
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:182
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:198
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:199
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:218
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:238
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:257
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:261
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:264
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:254
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:80
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:121
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:128
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:587
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:635
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:52
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:54
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:462
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:463
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:479
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:484
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:73
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:256
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:383
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:74
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:76
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:471
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:618
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:619
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:620
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt`:353
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:76
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:139
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:140
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:158
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:165
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:209
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:69
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:238
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:239
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:264
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:311
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:331
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:364
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:426
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:111
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:114
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:117
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:120
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:123
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:126
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:160
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:163
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:166
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:173
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:202
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:216
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:223
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:230
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:233
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:243
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:244
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:58
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:83
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:96
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:101
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:314
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:107
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:10
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:11
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:12
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:13
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:14
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:15
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:16
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:18
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:19
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:21
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:22
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Color.kt`:23
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:18
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:21
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:34
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:35
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:36
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:37
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:38
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:39
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:40
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:41
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:42
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:43
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:44
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:45
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:46
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/work/FeedWorker.kt`:53
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/work/FreshnessWorker.kt`:32
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/work/LearningWorker.kt`:42
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MagicNumber — detekt: detekt.style.MagicNumber
- Location: `app/src/main/java/com/propslab/work/SandboxWorker.kt`:45
- Message: This expression contains a magic number. Consider defining it to a well named constant.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/PropsLabApp.kt`:48
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:67
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:104
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:119
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:167
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:31
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:35
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:57
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:82
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:101
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/SystemPort.kt`:55
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/SystemTools.kt`:58
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/SystemTools.kt`:112
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/SystemTools.kt`:123
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/agent/Tools.kt`:69
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:38
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:94
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:114
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/claim/ClaimCompiler.kt`:116
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/claim/ClaimModels.kt`:81
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:71
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/claim/DeepTest.kt`:95
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:135
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:85
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:103
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:108
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/AutopilotUi.kt`:153
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/Daos.kt`:130
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FeedDao.kt`:148
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:58
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:59
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:60
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:64
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:69
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:70
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:77
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:92
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:105
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:106
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:107
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:114
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:116
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:120
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:122
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/FixtureSeeder.kt`:127
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:123
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:126
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:213
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:215
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:218
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameLines.kt`:106
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/GameLines.kt`:107
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/LearningUi.kt`:74
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/LearningUi.kt`:76
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:473
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:485
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:670
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:88
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:89
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:142
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:156
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:200
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:201
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsDatabase.kt`:222
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:604
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:659
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:681
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:782
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:847
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:854
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:868
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:980
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1050
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1153
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1314
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1336
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1490
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1629
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1654
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1921
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1922
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/data/SignalEntities.kt`:39
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/domain/BetNarrative.kt`:80
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/domain/BetNarrative.kt`:81
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:100
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:138
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:142
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:35
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedHttp.kt`:41
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:330
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:331
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:399
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:529
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:580
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:790
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:793
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:844
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:876
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:163
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:104
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:109
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:128
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:134
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:151
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:286
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:192
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:58
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/inference/Scorer.kt`:34
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/mathx/FormulaLab.kt`:216
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/mathx/MathXPrompts.kt`:51
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:61
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:62
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:467
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/sim/PromotionGate.kt`:75
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:106
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:41
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:52
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:170
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/Format.kt`:54
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:166
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:167
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:75
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:77
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:79
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:83
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:84
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:112
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:113
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:117
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:145
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:64
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:74
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:118
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:155
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:60
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:97
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:177
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:198
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:226
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:227
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:228
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:328
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:484
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:521
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:162
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:266
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:328
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:85
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:95
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:131
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:149
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:168
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:200
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:208
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:238
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:309
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:320
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:325
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:331
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:332
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:351
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:379
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:396
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:397
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:407
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:411
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:132
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:159
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:463
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:512
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:142
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:146
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:211
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:214
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:269
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:487
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:703
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:756
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:880
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:150
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:441
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:447
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:466
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:486
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:201
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:209
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:211
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:230
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:281
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:288
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:367
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:382
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:383
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:429
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:155
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:269
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:481
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:75
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:92
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:114
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:145
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:182
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:290
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:301
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:338
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:348
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:100
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:103
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:138
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:148
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:167
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:213
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:217
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:140
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:165
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:171
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:252
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:272
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:311
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:426
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:443
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:451
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:129
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:173
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:255
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:105
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:76
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:97
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:254
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:258
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:259
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:282
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:320
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:325
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:340
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:70
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:80
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:90
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:102
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:123
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:133
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:143
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:182
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:277
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:18
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/main/java/com/propslab/ui/theme/Type.kt`:21
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/AgentToolsTest.kt`:78
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/AgentToolsTest.kt`:225
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/AgentToolsTest.kt`:265
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/ClaimTest.kt`:96
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/ClaimTest.kt`:280
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/ClaimTest.kt`:282
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/DomainTest.kt`:38
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/DomainTest.kt`:68
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FantasyFeedTest.kt`:58
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:24
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:35
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:46
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:131
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:216
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:219
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:222
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/FormulaTest.kt`:225
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/LeagueInjuriesTest.kt`:70
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/MathXTest.kt`:58
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/MathXTest.kt`:60
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/SandboxTest.kt`:114
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/SandboxTest.kt`:252
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/SandboxTest.kt`:285
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MaxLineLength — detekt: detekt.style.MaxLineLength
- Location: `app/src/test/java/com/propslab/SportsDbTest.kt`:256
- Message: Line detected, which is longer than the defined maximum line length in the code style.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MayBeConst — detekt: detekt.style.MayBeConst
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:221
- Message: OFFSET can be a `const val`.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MayBeConst — detekt: detekt.style.MayBeConst
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:222
- Message: PRIME can be a `const val`.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MayBeConst — detekt: detekt.style.MayBeConst
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:223
- Message: GAMMA can be a `const val`.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MayBeConst — detekt: detekt.style.MayBeConst
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:224
- Message: MIX1 can be a `const val`.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.MayBeConst — detekt: detekt.style.MayBeConst
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:225
- Message: MIX2 can be a `const val`.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:51
- Message: Function probabilitiesOrNull has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:74
- Message: Function run has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:115
- Message: Function run has 9 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:184
- Message: Function run has 13 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:230
- Message: Function edgeOf has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:256
- Message: Function run has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:303
- Message: Function run has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:44
- Message: Function route has 16 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/ConsoleRouter.kt`:130
- Message: Function injury has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/agent/SystemTools.kt`:98
- Message: Function run has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:44
- Message: Function search has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/availability/InjuryNews.kt`:66
- Message: Function parse has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:62
- Message: Function parse has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/availability/InjuryParser.kt`:80
- Message: Function mentionsPlayer has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:45
- Message: Function fetch has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/availability/LeagueInjuries.kt`:61
- Message: Function parse has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:111
- Message: Function unlock has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:175
- Message: Function verify has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:76
- Message: Function recent has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:86
- Message: Function sessionStatus has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:98
- Message: Function push has 7 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:140
- Message: Function output has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:148
- Message: Function download has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/compute/KaggleClient.kt`:185
- Message: Function parseOutput has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/GameForecasts.kt`:150
- Message: Function pickOf has 7 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/GameLines.kt`:85
- Message: Function spread has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/GameLines.kt`:100
- Message: Function total has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/GameLines.kt`:113
- Message: Function market has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/LearningSettlement.kt`:22
- Message: Function game has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:259
- Message: Function build has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:344
- Message: Function buildSameGame has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:229
- Message: Function recordSignal has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:366
- Message: Function setFormula has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:417
- Message: Function noteSignal has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:460
- Message: Function kaggleExperiment has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:600
- Message: Function runDeepTest has 9 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:672
- Message: Function resumeDeepTest has 9 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:688
- Message: Function recordDeepResult has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:764
- Message: Function reviewLiveRecord has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1280
- Message: Function settleGame has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1334
- Message: Function paramsOf has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1751
- Message: Function pickFrom has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1757
- Message: Function pickUnfiltered has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1767
- Message: Function measure has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1864
- Message: Function toSim has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1937
- Message: Function toLossObservation has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Calibration.kt`:59
- Message: Function fit has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Circadian.kt`:71
- Message: Function impact has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Formulas.kt`:77
- Message: Function kellyFraction has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/GameModel.kt`:171
- Message: Function forecast has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:83
- Message: Function reprice has 14 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:147
- Message: Function probabilities has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:185
- Message: Function curveOf has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:43
- Message: Function build has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:83
- Message: Function adjust has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:124
- Message: Function kelly has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/domain/Parlay.kt`:131
- Message: Function allocate has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:334
- Message: Function advise has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:67
- Message: Function parse has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:233
- Message: Function recentFinalGames has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:254
- Message: Function scoreboard has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:273
- Message: Function parseInjuries has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:326
- Message: Function parseNflBoxScore has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:31
- Message: Function fetch has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/fantasy/GameLog.kt`:49
- Message: Function parse has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:25
- Message: Function parseGames has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:69
- Message: Function parseBoxScore has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:126
- Message: Function headerDate has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/EspnApi.kt`:140
- Message: Function namesIn has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedBoard.kt`:25
- Message: Function acceptable has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:281
- Message: Function syncResults has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:425
- Message: Function loadDay has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:454
- Message: Function settleOutstanding has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:488
- Message: Function matchGame has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:511
- Message: Function syncSignals has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:605
- Message: Function recordLeague has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:667
- Message: Function syncProps has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:81
- Message: Function score has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:129
- Message: Function parseGameLines has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:166
- Message: Function parseEventOdds has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:237
- Message: Function priceOf has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:248
- Message: Function isoUtc has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:80
- Message: Function parseEvents has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:108
- Message: Function parsePlayers has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:135
- Message: Function players has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:143
- Message: Function events has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/feed/SportsDb.kt`:160
- Message: Function utcMillis has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:229
- Message: Function share has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/hub/ModelHub.kt`:268
- Message: Function finish has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:74
- Message: Function parseFeed has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/hub/Updater.kt`:238
- Message: Function isNewer has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:44
- Message: Function probabilities has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:76
- Message: Function cdfAt has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/ChampionTable.kt`:91
- Message: Function fromJson has 10 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:55
- Message: Function score has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:88
- Message: Function walk has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/GbmBundleScorer.kt`:106
- Message: Function interp has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/inference/Scorer.kt`:32
- Message: Function score has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/mathx/LlamaCppModel.kt`:63
- Message: Function status has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/mathx/MathXAssistant.kt`:23
- Message: Function explain has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:101
- Message: Function parseLine has 7 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:146
- Message: Function nameCandidate has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ocr/SlipParser.kt`:154
- Message: Function looksLikeName has 5 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:63
- Message: Function plan has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:360
- Message: Function verdict has 4 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:419
- Message: Function marketProposal has 6 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/sim/SandboxEngine.kt`:34
- Message: Function decide has 9 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:257
- Message: Function edgeOf has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/sim/SandboxTruth.kt`:38
- Message: Function truthOver has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/theory/TheoryLab.kt`:46
- Message: Function parse has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:230
- Message: Function MarketCrossCheck has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:163
- Message: Function toggleLog has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:758
- Message: Function hitRateText has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.ReturnCount — detekt: detekt.style.ReturnCount
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:379
- Message: Function sideHandicap has 3 return statements which exceeds the limit of 2.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.UnusedParameter — detekt: detekt.style.UnusedParameter
- Location: `app/src/main/java/com/propslab/fantasy/FantasyFeed.kt`:326
- Message: Function parameter `gameId` is unused.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.UnusedParameter — detekt: detekt.style.UnusedParameter
- Location: `app/src/main/java/com/propslab/feed/ResultsApi.kt`:79
- Message: Function parameter `fetchedUtc` is unused.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.UnusedPrivateProperty — detekt: detekt.style.UnusedPrivateProperty
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:645
- Message: Private property `attempt` is unused.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/Nav.kt`:10
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:10
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:5
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:11
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/AuditScreen.kt`:4
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/BestScreen.kt`:6
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt`:7
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:7
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:20
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:8
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:4
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:6
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:11
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:9
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:6
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:5
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:5
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:13
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/NewsScreen.kt`:8
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:7
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt`:6
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:7
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:19
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:7
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:13
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:4
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:4
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:9
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:4
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:6
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:9
- Message: androidx.compose.foundation.layout.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:15
- Message: androidx.compose.runtime.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/DomainTest.kt`:3
- Message: com.propslab.domain.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/DomainTest.kt`:4
- Message: com.propslab.inference.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/DomainTest.kt`:5
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/FantasyDeskTest.kt`:11
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/FantasyFeedTest.kt`:9
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/FantasyGameLogTest.kt`:5
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/GameModelTest.kt`:11
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/ParlayLabTest.kt`:26
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [MEDIUM] DETEKT-detekt.style.WildcardImport — detekt: detekt.style.WildcardImport
- Location: `app/src/test/java/com/propslab/ParlayTest.kt`:7
- Message: org.junit.Assert.* is a wildcard import. Replace it with fully qualified imports.
- Suggested action: See the detekt rule documentation for the fix.
- Source: detekt

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/AccountScreen.kt`:61
- Message: Hardcoded user-visible strings in Text() (5 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/DetailScreen.kt`:150
- Message: Hardcoded user-visible strings in Text() (8 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:450
- Message: Hardcoded user-visible strings in Text() (5 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/FeedScreen.kt`:295
- Message: Hardcoded user-visible strings in Text() (12 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/ImportScreen.kt`:133
- Message: Hardcoded user-visible strings in Text() (7 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:267
- Message: Hardcoded user-visible strings in Text() (27 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt`:114
- Message: Hardcoded user-visible strings in Text() (10 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt`:155
- Message: Hardcoded user-visible strings in Text() (9 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt`:145
- Message: Hardcoded user-visible strings in Text() (10 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt`:64
- Message: Hardcoded user-visible strings in Text() (7 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt`:217
- Message: Hardcoded user-visible strings in Text() (5 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/SlateScreen.kt`:151
- Message: Hardcoded user-visible strings in Text() (6 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] COMP-002 — Hardcoded user-visible strings in Text()
- Location: `app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt`:67
- Message: Hardcoded user-visible strings in Text() (7 occurrences)
- Suggested action: Move to strings.xml (localization, accessibility, string-based testing).
- Source: deterministic

### [LOW] DEBT-001 — Long parameter list in stage()
- Location: `app/src/main/java/com/propslab/agent/Tools.kt`:175
- Message: stage() takes 8 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in explain()
- Location: `app/src/main/java/com/propslab/data/ParlayTickets.kt`:450
- Message: explain() takes 9 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in measure()
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1767
- Message: measure() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in reprice()
- Location: `app/src/main/java/com/propslab/domain/LinePush.kt`:83
- Message: reprice() takes 9 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in evaluate()
- Location: `app/src/main/java/com/propslab/domain/Odds.kt`:106
- Message: evaluate() takes 9 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in footballScore()
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:247
- Message: footballScore() takes 9 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in build()
- Location: `app/src/main/java/com/propslab/fantasy/FantasyDesk.kt`:305
- Message: build() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in buildBoard()
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:808
- Message: buildBoard() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in writeState()
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:945
- Message: writeState() takes 9 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in score()
- Location: `app/src/main/java/com/propslab/feed/GameLogModel.kt`:81
- Message: score() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in pairOutcomes()
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:188
- Message: pairOutcomes() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in drawWrapped()
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:173
- Message: drawWrapped() takes 10 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in text()
- Location: `app/src/main/java/com/propslab/flyer/Flyer.kt`:206
- Message: text() takes 10 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in plan()
- Location: `app/src/main/java/com/propslab/sim/Autopilot.kt`:63
- Message: plan() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in finding()
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:438
- Message: finding() takes 7 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in SplitRing()
- Location: `app/src/main/java/com/propslab/ui/components/Components.kt`:48
- Message: SplitRing() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in Stepper()
- Location: `app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt`:334
- Message: Stepper() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in prop()
- Location: `app/src/test/java/com/propslab/AgentToolsTest.kt`:50
- Message: prop() takes 9 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in game()
- Location: `app/src/test/java/com/propslab/GameModelTest.kt`:19
- Message: game() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in observation()
- Location: `app/src/test/java/com/propslab/LossInsightsTest.kt`:31
- Message: observation() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-001 — Long parameter list in candidate()
- Location: `app/src/test/java/com/propslab/ParlayLabTest.kt`:423
- Message: candidate() takes 6 parameters.
- Suggested action: Group the parameters into a named type; call sites become readable and defaultable.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/account/Account.kt`:1
- Message: 2 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/agent/AgentTools.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/feed/OddsApi.kt`:1
- Message: 2 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/sim/LossInsights.kt`:1
- Message: 4 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/ui/components/Motion.kt`:1
- Message: 5 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/ui/screens/LabScreen.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/ui/screens/LensesScreen.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/main/java/com/propslab/ui/screens/SignalsCard.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] DEBT-007 — Repeated blocks within a file
- Location: `app/src/test/java/com/propslab/ParlayLabTest.kt`:1
- Message: 3 six-line block(s) repeat inside this file.
- Suggested action: Extract the repeated block once; two copies already drift apart.
- Source: brooks-debt

### [LOW] REV-006 — String.format without an explicit Locale
- Location: `app/src/main/java/com/propslab/data/PropsRepository.kt`:1857
- Message: Implicit default locale makes formatting device-dependent.
- Suggested action: Pass Locale.US/ROOT explicitly, or use a formatter.
- Source: review

### [LOW] REV-006 — String.format without an explicit Locale
- Location: `app/src/main/java/com/propslab/feed/FeedSync.kt`:978
- Message: Implicit default locale makes formatting device-dependent.
- Suggested action: Pass Locale.US/ROOT explicitly, or use a formatter.
- Source: review

### [LOW] REV-006 — String.format without an explicit Locale
- Location: `app/src/main/java/com/propslab/sim/SandboxModels.kt`:128
- Message: Implicit default locale makes formatting device-dependent.
- Suggested action: Pass Locale.US/ROOT explicitly, or use a formatter.
- Source: review

### [LOW] REV-007 — Wall-clock time used as a duration
- Location: `app/src/main/java/com/propslab/billing/Tiers.kt`:66
- Message: currentTimeMillis is wall-clock and can jump; elapsed time should use elapsedRealtime.
- Suggested action: Use SystemClock.elapsedRealtime() or a monotonic clock.
- Source: review

### [LOW] REV-007 — Wall-clock time used as a duration
- Location: `app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt`:125
- Message: currentTimeMillis is wall-clock and can jump; elapsed time should use elapsedRealtime.
- Suggested action: Use SystemClock.elapsedRealtime() or a monotonic clock.
- Source: review

## Version diff

Compared against snapshot #13 (2026-10-30T01:13:19-04:00).

### New (9)
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt

### Resolved (12)
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.style.NewLineAtEndOfFile` detekt: detekt.style.NewLineAtEndOfFile — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.style.UnusedParameter` detekt: detekt.style.UnusedParameter — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.UnusedParameter` detekt: detekt.style.UnusedParameter — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/PropsRepository.kt

### Persistent (637)
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/billing/Tiers.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/inference/ChampionTable.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/Nav.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/DomainTest.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/feed/OddsApi.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DEBT-001` Long parameter list in reprice() — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/components/Motion.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/claim/ClaimCompiler.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/theory/TheoryLab.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/PropsLabApp.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/hub/DeviceProfile.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/flyer/Flyer.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/sim/SandboxEngine.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/Names.kt
- `DEBT-001` Long parameter list in build() — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/NewsScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/inference/Scorer.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/claim/ClaimCompiler.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/mathx/FormulaLab.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/domain/GameModel.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DEBT-001` Long parameter list in finding() — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/mathx/MathXAssistant.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/inference/CloudModel.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/NewsScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DEBT-001` Long parameter list in evaluate() — app/src/main/java/com/propslab/domain/Odds.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/claim/ClaimBench.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/theme/Color.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/theme/Color.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/components/PremiumGate.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/flyer/Flyer.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/theme/Type.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/mathx/LlamaCppModel.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/SystemTools.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/agent/Tools.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/FeedDao.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.UnusedPrivateProperty` detekt: detekt.style.UnusedPrivateProperty — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/sim/SandboxEngine.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/settings/AppSettings.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/Nav.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/availability/InjuryNews.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/ParlayTest.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/LearningSettlement.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/claim/ClaimCompiler.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/domain/Circadian.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DEBT-001` Long parameter list in text() — app/src/main/java/com/propslab/flyer/Flyer.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/ResultsApi.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/GameForecasts.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/mathx/MathXPrompts.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/domain/Calibration.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DEBT-002` Oversized unit: NewsItem — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/availability/InjuryParser.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/claim/ClaimCompiler.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/PropsDatabase.kt
- `DEBT-001` Long parameter list in footballScore() — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/SandboxEngine.kt
- `DETEKT-detekt.style.MayBeConst` detekt: detekt.style.MayBeConst — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/FixtureSeeder.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/FantasyFeedTest.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/flyer/Flyer.kt
- `REV-006` String.format without an explicit Locale — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/claim/DeepTest.kt
- `DEBT-002` Oversized unit: FeedScreenState — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/domain/Odds.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/FeedDao.kt
- `DEBT-001` Long parameter list in explain() — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/theory/TheoryLab.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/account/Account.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/ResultsApi.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsDatabase.kt
- `DETEKT-detekt.style.UnusedParameter` detekt: detekt.style.UnusedParameter — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/domain/LinePush.kt
- `DEBT-002` Oversized unit: LabViewModel — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/FixtureSeeder.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DEBT-001` Long parameter list in buildBoard() — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DEBT-002` Oversized unit: ParlayLabTest — app/src/test/java/com/propslab/ParlayLabTest.kt
- `DEBT-001` Long parameter list in prop() — app/src/test/java/com/propslab/AgentToolsTest.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/availability/InjuryNews.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/Calibration.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/data/PropsRepository.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/ClaimTest.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/ParlayLabTest.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/work/LearningWorker.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/work/SandboxWorker.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/components/AutopilotCard.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/FantasyFeedTest.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/test/java/com/propslab/ParlayLabTest.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/MathXTest.kt
- `DEBT-002` Oversized unit: ParlayCandidate — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/ChampionTable.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/inference/Scorer.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/AutopilotUi.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/availability/LeagueInjuries.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/components/Motion.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/LeagueInjuriesTest.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/Nav.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/test/java/com/propslab/GameModelTest.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DEBT-002` Oversized unit: ModelShopScreen — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DEBT-007` Repeated blocks within a file — app/src/test/java/com/propslab/ParlayLabTest.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/SandboxTest.kt
- `DEBT-002` Oversized unit: LensesViewModel — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/GameLogModel.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/test/java/com/propslab/FormulaTest.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ocr/SlipParser.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/main/java/com/propslab/theory/TheoryLab.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/inference/ChampionTable.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/OddsApi.kt
- `REV-006` String.format without an explicit Locale — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/LearningSettlement.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DEBT-002` Oversized unit: DetailViewModel — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/sim/SandboxEngine.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/FantasyGameLogTest.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/Odds.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/feed/FeedHttp.kt
- `REV-006` String.format without an explicit Locale — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/Formulas.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/sim/SandboxEngine.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/account/Account.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/flyer/Flyer.kt
- `DEBT-001` Long parameter list in stage() — app/src/main/java/com/propslab/agent/Tools.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/components/AutopilotCard.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/claim/DeepTest.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/Circadian.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/feed/FeedSync.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ocr/SlipParser.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/domain/BetNarrative.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Circadian.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/ocr/ScreenshotOcr.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DEBT-001` Long parameter list in score() — app/src/main/java/com/propslab/feed/GameLogModel.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/availability/InjuryNews.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/flyer/Flyer.kt
- `DEBT-005` Talk-to-many class: PropsRepository — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/GameLines.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/availability/Availability.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/GameModel.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DEBT-004` Mutable singleton state: SandboxHash — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Calibration.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/mathx/MathXAssistant.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/AuditScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/sim/PromotionGate.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/AutopilotUi.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/availability/InjuryNews.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/theory/TheoryLab.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/LearningUi.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/work/FreshnessWorker.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/Season.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/theme/Theme.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/DomainTest.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.LargeClass` detekt: detekt.complexity.LargeClass — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/sim/Autopilot.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/agent/SystemTools.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/availability/InjuryNews.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/FixtureSeeder.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Parlay.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/DomainTest.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/Nav.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/GameModel.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/GameForecasts.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DEBT-001` Long parameter list in plan() — app/src/main/java/com/propslab/sim/Autopilot.kt
- `DETEKT-detekt.style.MayBeConst` detekt: detekt.style.MayBeConst — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DEBT-002` Oversized unit: ParlaySeed — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedBoard.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/GameLogModel.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/Daos.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/mathx/LlamaCppModel.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/availability/LeagueInjuries.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/GameModelTest.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/work/FeedWorker.kt
- `DEBT-001` Long parameter list in SplitRing() — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DEBT-001` Long parameter list in pairOutcomes() — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/AutopilotUi.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/PropsDatabase.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/feed/ResultsApi.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/work/LearningWorker.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt
- `DEBT-003` Deprecated API: onBackPressed — app/src/main/java/com/propslab/ui/screens/ManualActivity.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/sim/Autopilot.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/availability/LeagueInjuries.kt
- `DEBT-002` Oversized unit: PlayerLogUi — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/SandboxTruth.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/test/java/com/propslab/AgentToolsTest.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/GameForecasts.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/theme/Type.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/work/SandboxWorker.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/mathx/FormulaLab.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/SandboxDao.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/inference/Scorer.kt
- `DEBT-002` Oversized unit: FeedSync — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/domain/Odds.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ocr/SlipParser.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/SportsDbTest.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/SandboxUi.kt
- `DEBT-004` Mutable singleton state: ParlayStaking — app/src/main/java/com/propslab/domain/Parlay.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/Autopilot.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/availability/InjuryParser.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/DomainTest.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/Parlay.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/SignalDao.kt
- `DETEKT-detekt.complexity.NestedBlockDepth` detekt: detekt.complexity.NestedBlockDepth — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/feed/SportsDb.kt
- `REV-007` Wall-clock time used as a duration — app/src/main/java/com/propslab/billing/Tiers.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/GbmBundleScorer.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DEBT-002` Oversized unit: SlateState — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/GameForecasts.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/FormulaTest.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Parlay.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/feed/GameLogModel.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/Nav.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/FeedConfig.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/mathx/LlamaCppModel.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/ConsoleRouter.kt
- `DEBT-002` Oversized unit: FantasyLine — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DEBT-002` Oversized unit: ParlayViewModel — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/test/java/com/propslab/LossInsightsTest.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/availability/LeagueInjuries.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/theory/TheoryLab.kt
- `DEBT-001` Long parameter list in Stepper() — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/PlainEnglishScreen.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/inference/CloudModel.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DEBT-005` Talk-to-many class: FeedViewModel — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DEBT-005` Talk-to-many class: FeedScreenState — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/theory/TheoryLab.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/Format.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/GameLines.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/AuditScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/components/Components.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Formulas.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/FeedHttp.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/GameLines.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/BetNarrative.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/hub/Updater.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DEBT-002` Oversized unit: LossObservation — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/availability/LeagueInjuries.kt
- `DEBT-001` Long parameter list in drawWrapped() — app/src/main/java/com/propslab/flyer/Flyer.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/AutopilotUi.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/data/GameForecasts.kt
- `DETEKT-detekt.style.MayBeConst` detekt: detekt.style.MayBeConst — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/Scorer.kt
- `DEBT-001` Long parameter list in candidate() — app/src/test/java/com/propslab/ParlayLabTest.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/feed/ResultsApi.kt
- `DETEKT-detekt.exceptions.TooGenericExceptionCaught` detekt: detekt.exceptions.TooGenericExceptionCaught — app/src/main/java/com/propslab/work/FreshnessWorker.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/components/Components.kt
- `COMP-002` Hardcoded user-visible strings in Text() — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DEBT-001` Long parameter list in game() — app/src/test/java/com/propslab/GameModelTest.kt
- `DETEKT-detekt.style.UnusedParameter` detekt: detekt.style.UnusedParameter — app/src/main/java/com/propslab/feed/ResultsApi.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/AuditScreen.kt
- `DEBT-005` Talk-to-many class: ParlaySeed — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/work/FeedWorker.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/availability/InjuryParser.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/ui/screens/NewsScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DEBT-002` Oversized unit: SandboxScreenState — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/ChampionTable.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/ImportScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/ocr/SlipParser.kt
- `DETEKT-detekt.naming.MatchingDeclarationName` detekt: detekt.naming.MatchingDeclarationName — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/Format.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/NewsScreen.kt
- `DETEKT-detekt.style.MayBeConst` detekt: detekt.style.MayBeConst — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/NewsScreen.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.complexity.ComplexCondition` detekt: detekt.complexity.ComplexCondition — app/src/main/java/com/propslab/domain/LinePush.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/claim/ClaimCompiler.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Parlay.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DEBT-004` Mutable singleton state: Licensing — app/src/main/java/com/propslab/billing/Tiers.kt
- `DEBT-005` Talk-to-many class: SandboxTest — app/src/test/java/com/propslab/SandboxTest.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/agent/Tools.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/feed/EspnApi.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/agent/SystemPort.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/AccountScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/claim/ClaimModels.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/test/java/com/propslab/AgentToolsTest.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/sim/LossInsights.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DEBT-001` Long parameter list in writeState() — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/inference/DeviceAssistant.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/domain/Parlay.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/UpdatesScreen.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/ParlayTickets.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/OddsApi.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/data/LearningDao.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/GameLines.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/billing/Tiers.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/PortfolioScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/components/Motion.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/test/java/com/propslab/FantasyDeskTest.kt
- `DETEKT-detekt.complexity.CyclomaticComplexMethod` detekt: detekt.complexity.CyclomaticComplexMethod — app/src/main/java/com/propslab/sim/SandboxEngine.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/sim/SandboxTruth.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/claim/ClaimModels.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/SignalEntities.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/account/Account.kt
- `REV-007` Wall-clock time used as a duration — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/domain/LinePush.kt
- `DEBT-001` Long parameter list in observation() — app/src/test/java/com/propslab/LossInsightsTest.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DEBT-001` Long parameter list in measure() — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/sim/PromotionGate.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/inference/LocalAssistant.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/billing/Tiers.kt
- `DEBT-005` Talk-to-many class: FeedTest — app/src/test/java/com/propslab/FeedTest.kt
- `DETEKT-detekt.style.MayBeConst` detekt: detekt.style.MayBeConst — app/src/main/java/com/propslab/sim/SandboxModels.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/hub/ModelHub.kt
- `DETEKT-detekt.complexity.LongParameterList` detekt: detekt.complexity.LongParameterList — app/src/main/java/com/propslab/ui/screens/LabScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/SportsDb.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/agent/AgentTools.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/ConsoleScreen.kt
- `DETEKT-detekt.empty-blocks.EmptyElseBlock` detekt: detekt.empty-blocks.EmptyElseBlock — app/src/main/java/com/propslab/agent/Tools.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/data/Daos.kt
- `DETEKT-detekt.style.LoopWithTooManyJumpStatements` detekt: detekt.style.LoopWithTooManyJumpStatements — app/src/main/java/com/propslab/fantasy/GameLog.kt
- `DETEKT-detekt.complexity.TooManyFunctions` detekt: detekt.complexity.TooManyFunctions — app/src/main/java/com/propslab/compute/KaggleClient.kt
- `DEBT-002` Oversized unit: BestViewModel — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/PremiumScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt
- `DETEKT-detekt.style.WildcardImport` detekt: detekt.style.WildcardImport — app/src/main/java/com/propslab/ui/screens/AuditScreen.kt
- `DETEKT-detekt.naming.FunctionNaming` detekt: detekt.naming.FunctionNaming — app/src/main/java/com/propslab/ui/screens/BestScreen.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/feed/FeedSync.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/ui/screens/AuditScreen.kt
- `DETEKT-detekt.style.MaxLineLength` detekt: detekt.style.MaxLineLength — app/src/main/java/com/propslab/flyer/Flyer.kt
- `DETEKT-detekt.style.MagicNumber` detekt: detekt.style.MagicNumber — app/src/main/java/com/propslab/feed/FeedBoard.kt
- `DETEKT-detekt.complexity.LargeClass` detekt: detekt.complexity.LargeClass — app/src/main/java/com/propslab/data/PropsRepository.kt
- `DETEKT-detekt.style.ReturnCount` detekt: detekt.style.ReturnCount — app/src/main/java/com/propslab/inference/ChampionTable.kt
- `DETEKT-detekt.complexity.LongMethod` detekt: detekt.complexity.LongMethod — app/src/main/java/com/propslab/fantasy/GameLog.kt

## Coverage

```json
{
  "checked_rule_ids": [
    "AI-001",
    "AI-002",
    "AI-003",
    "AI-004",
    "AI-005",
    "AI-006",
    "AI-007",
    "AI-008",
    "AI-009",
    "AUD-001",
    "AUD-003",
    "COMP-001",
    "COMP-002",
    "CONC-001",
    "CONC-002",
    "CONC-003",
    "CONC-004",
    "CONC-005",
    "LLM-001",
    "LLM-002",
    "PLAY-001",
    "PLAY-002",
    "PLAY-004",
    "PLAY-005",
    "PLAY-006",
    "PLAY-007",
    "PLAY-008",
    "PLAY-009",
    "PLAY-010",
    "PLAY-011",
    "SEC-001",
    "SEC-002",
    "SEC-003",
    "SEC-004",
    "SEC-005",
    "SEC-006",
    "SEC-007",
    "SEC-008",
    "SEC-009",
    "SEC-010",
    "SEC-011",
    "SEC-012",
    "SEC-013"
  ],
  "rule_statuses": [
    {
      "rule": "SEC-001",
      "category": "Security",
      "title": "Hardcoded secret / API key / private key in source",
      "status": "no finding detected",
      "severity": "CRITICAL"
    },
    {
      "rule": "SEC-002",
      "category": "Security",
      "title": "TLS validation disabled (trust-all manager / hostname verifier)",
      "status": "no finding detected",
      "severity": "CRITICAL"
    },
    {
      "rule": "SEC-003",
      "category": "Security",
      "title": "Weak crypto (MD5/SHA-1/DES/RC4/AES-ECB)",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-004",
      "category": "Security",
      "title": "Cleartext HTTP traffic allowed",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-005",
      "category": "Security",
      "title": "Token/password/secret stored in plain SharedPreferences",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-006",
      "category": "Security",
      "title": "Risky WebView configuration",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-007",
      "category": "Security",
      "title": "Exported component without permission protection",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "SEC-008",
      "category": "Security",
      "title": "SQL built by string concatenation",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-009",
      "category": "Security",
      "title": "World-readable/writable file mode",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-010",
      "category": "Security",
      "title": "android:debuggable=\"true\" in manifest",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-011",
      "category": "Security",
      "title": ".gitignore missing keystore / local.properties / .env entries",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "SEC-012",
      "category": "Security",
      "title": "Signing password hardcoded in Gradle",
      "status": "no finding detected",
      "severity": "CRITICAL"
    },
    {
      "rule": "SEC-013",
      "category": "Security",
      "title": "allowBackup=\"true\" (app data extractable via backup)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "PLAY-001",
      "category": "Play compliance",
      "title": "targetSdk below 36",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "PLAY-002",
      "category": "Play compliance",
      "title": "compileSdk below targetSdk or below 36",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "PLAY-003",
      "category": "Play compliance",
      "title": "Native code without 16 KB page-size alignment evidence",
      "status": "not assessed",
      "severity": "HIGH"
    },
    {
      "rule": "PLAY-004",
      "category": "Play compliance",
      "title": "Deprecated edge-to-edge flags (misbehave at target 36)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "PLAY-005",
      "category": "Play compliance",
      "title": "Orientation / resizability lock (ignored on large screens at target 36)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "PLAY-006",
      "category": "Play compliance",
      "title": "startForeground() used but no foregroundServiceType declared",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "PLAY-007",
      "category": "Play compliance",
      "title": "Notifications used but POST_NOTIFICATIONS not declared",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "PLAY-008",
      "category": "Play compliance",
      "title": "Play Billing Library below 8.0.0",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "PLAY-009",
      "category": "Play compliance",
      "title": "Component has intent-filter but no android:exported",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "PLAY-010",
      "category": "Play compliance",
      "title": "Release build type has minification disabled",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "PLAY-011",
      "category": "Play compliance",
      "title": "Restricted permission (needs Play declaration/justification)",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "AI-001",
      "category": "AI-code smells",
      "title": "Stub left in code (TODO() / NotImplementedError)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "AI-002",
      "category": "AI-code smells",
      "title": "Many TODO/FIXME/stub/placeholder comments",
      "status": "no finding detected",
      "severity": "LOW"
    },
    {
      "rule": "AI-003",
      "category": "AI-code smells",
      "title": "Auth/validation function hard-wired to return true",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "AI-004",
      "category": "AI-code smells",
      "title": "Dynamic / unpinned dependency version",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "AI-005",
      "category": "AI-code smells",
      "title": "Deprecated/removed Android API (training-data lag)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "AI-006",
      "category": "AI-code smells",
      "title": "Empty catch block swallows exceptions",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "AI-007",
      "category": "AI-code smells",
      "title": "Heavy use of !! (null-assertion) in a file",
      "status": "no finding detected",
      "severity": "LOW"
    },
    {
      "rule": "AI-008",
      "category": "AI-code smells",
      "title": "Placeholder text/keys (lorem ipsum, YOUR_API_KEY, example.com)",
      "status": "no finding detected",
      "severity": "LOW"
    },
    {
      "rule": "AI-009",
      "category": "AI-code smells",
      "title": "No unit or instrumentation tests found",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "CONC-001",
      "category": "Concurrency/lifecycle",
      "title": "GlobalScope coroutine (leaks, ignores lifecycle)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "CONC-002",
      "category": "Concurrency/lifecycle",
      "title": "runBlocking in app code (ANR risk on main thread)",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "CONC-003",
      "category": "Concurrency/lifecycle",
      "title": "collectAsState() instead of collectAsStateWithLifecycle()",
      "status": "no finding detected",
      "severity": "LOW"
    },
    {
      "rule": "CONC-004",
      "category": "Concurrency/lifecycle",
      "title": "Thread.sleep in app code",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "CONC-005",
      "category": "Concurrency/lifecycle",
      "title": "Activity/Context/View held in a static (object/companion) field",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "COMP-001",
      "category": "Compose",
      "title": "mutableStateOf in a composable without remember (state resets on recomposition)",
      "status": "no finding detected",
      "severity": "LOW"
    },
    {
      "rule": "COMP-002",
      "category": "Compose",
      "title": "Hardcoded user-visible strings in Text()",
      "status": "finding detected",
      "severity": "LOW"
    },
    {
      "rule": "LLM-001",
      "category": "On-device LLM",
      "title": "Large model file inside the project tree",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "LLM-002",
      "category": "On-device LLM",
      "title": "Model loaded from a hardcoded shared-storage path",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "AUD-001",
      "category": "Audio/MIDI",
      "title": "RECORD_AUDIO declared but never requested at runtime",
      "status": "no finding detected",
      "severity": "HIGH"
    },
    {
      "rule": "AUD-002",
      "category": "Audio/MIDI",
      "title": "Real-time-unsafe call in a file containing an audio callback",
      "status": "not assessed",
      "severity": "MEDIUM"
    },
    {
      "rule": "AUD-003",
      "category": "Audio/MIDI",
      "title": "MIDI API used but android.software.midi feature not declared",
      "status": "no finding detected",
      "severity": "MEDIUM"
    },
    {
      "rule": "BUILD-000",
      "category": "Build oracle",
      "title": "Gradle build oracle skipped",
      "status": "not assessed",
      "severity": "INFO"
    },
    {
      "rule": "BUILD-001",
      "category": "Build oracle",
      "title": "Gradle lint/test/assemble failed",
      "status": "not assessed",
      "severity": "HIGH"
    },
    {
      "rule": "BUILD-002",
      "category": "Build oracle",
      "title": "Unbalanced block comment (Kotlin/Java comments nest)",
      "status": "not assessed",
      "severity": "HIGH"
    },
    {
      "rule": "APK-001",
      "category": "Package integrity",
      "title": "APK ZIP CRC integrity",
      "status": "not assessed",
      "severity": "HIGH"
    },
    {
      "rule": "APK-002",
      "category": "Package integrity",
      "title": "APK container readability",
      "status": "not assessed",
      "severity": "CRITICAL"
    }
  ],
  "assessed_rule_count": 43,
  "not_applicable_rule_count": 0,
  "total_rule_count": 50,
  "applicable_rule_count": 50,
  "coverage_percent": 86,
  "assessed_percent": 86,
  "by_category": {
    "Security": {
      "assessed": 13,
      "total": 13
    },
    "Play compliance": {
      "assessed": 10,
      "total": 11
    },
    "AI-code smells": {
      "assessed": 9,
      "total": 9
    },
    "Concurrency/lifecycle": {
      "assessed": 5,
      "total": 5
    },
    "Compose": {
      "assessed": 2,
      "total": 2
    },
    "On-device LLM": {
      "assessed": 2,
      "total": 2
    },
    "Audio/MIDI": {
      "assessed": 2,
      "total": 3
    },
    "Build oracle": {
      "assessed": 0,
      "total": 3
    },
    "Package integrity": {
      "assessed": 0,
      "total": 2
    }
  },
  "warnings": [],
  "semantics": "‘No finding detected’ means every detector that covers this rule ran and did not trigger. ‘Not assessed’ means the evidence or tool for that rule was unavailable. ‘Not applicable’ means the rule’s specific code surface was absent. A quiet detector is not proof of safety."
}
```

## Quality (RepoRank) and grade (Grader)

- RepoRank **85/100** — naming 100, idiom 85, hygiene 60, config 95
- Weights used: {"naming": 0.18, "idiom": 0.18, "hygiene": 0.14, "complexity": 0.14, "duplication": 0.12, "tests": 0.12, "configCoherence": 0.12}; dependency freshness is unmeasured offline and excluded, not assumed.
  - Complexity: 100 — 2.3 decisions/function
  - Duplication: 100 — 0.008 duplicated block ratio
  - Tests: 50 — 22/153 files are tests
- Grader **52 (F)** — needs-work
  - Security: 74 (weight 30)
    - Scored app findings: LOW 13
    - Excluded from scoring: 1632 advisory/baselined/library finding(s).
  - Quality: 50 (weight 25)
    - 22 test file(s) against 153 source file(s).
    - 0.8% duplicated 6-line blocks.
    - 2.3 average decisions per function.
    - 0 TODO/FIXME marker(s), 4 file(s) over 800 lines.
  - Architecture: 31 (weight 20)
    - 1 Gradle module(s); layers detected: data, domain, ui.
    - 26 package directories; DI framework not found.
    - Package cycles: com.propslab -> com.propslab.ui -> com.propslab.ui.components -> com.propslab.ui.theme -> com.propslab; com.propslab -> com.propslab.ui -> com.propslab.ui.screens -> com.propslab; com.propslab -> com.propslab.ui -> com.propslab.ui.screens -> com.propslab.flyer -> com.propslab; com.propslab -> com.propslab.ui -> com.propslab.ui.screens -> com.propslab.ui.components -> com.propslab.ui.theme -> com.propslab; com.propslab -> com.propslab.ui -> com.propslab.ui.screens -> com.propslab.ui.theme -> com.propslab.
    - UI importing data directly: com.propslab.ui.components -> com.propslab.data; com.propslab.ui.screens -> com.propslab.data.
  - Licence: 40 (weight 15)
    - LICENSE file: no
    - declared licence: none
    - SPDX headers: none
  - Market vitality: unmeasured (weight 10)
    - Unmeasured: needs registry/store benchmarking, which this desk does not do offline.
- Gate decision: **pass**
- Review checks: 5 advisory finding(s); advisory findings are shown but never scored.

### Debug evidence (not executed)

- Build-time flags: {"debuggableAttribute": false, "webViewDebugging": false, "strictModeConfigured": false, "leakCanaryDependency": false, "timberDependency": false, "logCalls": 2, "buildConfigDebugGuards": 0, "proguardEnabled": true}
- No StrictMode configuration found; main-thread violations will go unreported.
- LeakCanary is not a dependency, so no runtime leak evidence is being collected.
- **StrictMode on a debug build** — Reports main-thread disk/network and leaked closeables that no static scan can see. _not executed — requires a running device_
- **LeakCanary heap retention** — Only a live heap dump can prove an Activity/Fragment is retained. _not executed — requires a debug build and a device_
- **Startup and frame timing** — Cold-start and jank numbers cannot come from source. _not executed — requires a device_
- **Crash and ANR trace review** — A swallowed exception visible only at runtime is invisible to static rules. _not executed — requires a device_

## Architecture debt and reviewer layer

- Debt score **0/100** from 59 advisory finding(s).
- Merged external reports: none (set `AAD_REVIEW_SARIF` to a semicolon-separated list of SARIF files).

- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/account/Account.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/agent/AgentTools.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-001` Long parameter list in stage() — app/src/main/java/com/propslab/agent/Tools.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-004` Mutable singleton state: Licensing — app/src/main/java/com/propslab/billing/Tiers.kt: Move state behind an injected repository/state holder.
- `DEBT-001` Long parameter list in explain() — app/src/main/java/com/propslab/data/ParlayTickets.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-002` Oversized unit: ParlayCandidate — app/src/main/java/com/propslab/data/ParlayTickets.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-001` Long parameter list in measure() — app/src/main/java/com/propslab/data/PropsRepository.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-002` Oversized unit: ParlaySeed — app/src/main/java/com/propslab/data/PropsRepository.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-005` Talk-to-many class: ParlaySeed — app/src/main/java/com/propslab/data/PropsRepository.kt: Split by caller: each public surface should belong to one responsibility.
- `DEBT-005` Talk-to-many class: PropsRepository — app/src/main/java/com/propslab/data/PropsRepository.kt: Split by caller: each public surface should belong to one responsibility.
- `DEBT-001` Long parameter list in reprice() — app/src/main/java/com/propslab/domain/LinePush.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in evaluate() — app/src/main/java/com/propslab/domain/Odds.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-004` Mutable singleton state: ParlayStaking — app/src/main/java/com/propslab/domain/Parlay.kt: Move state behind an injected repository/state holder.
- `DEBT-001` Long parameter list in footballScore() — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in build() — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-002` Oversized unit: FantasyLine — app/src/main/java/com/propslab/fantasy/FantasyDesk.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-002` Oversized unit: NewsItem — app/src/main/java/com/propslab/fantasy/FantasyFeed.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-001` Long parameter list in buildBoard() — app/src/main/java/com/propslab/feed/FeedSync.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in writeState() — app/src/main/java/com/propslab/feed/FeedSync.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-002` Oversized unit: FeedSync — app/src/main/java/com/propslab/feed/FeedSync.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/feed/FeedSync.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-001` Long parameter list in score() — app/src/main/java/com/propslab/feed/GameLogModel.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in pairOutcomes() — app/src/main/java/com/propslab/feed/OddsApi.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/feed/OddsApi.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-001` Long parameter list in drawWrapped() — app/src/main/java/com/propslab/flyer/Flyer.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in text() — app/src/main/java/com/propslab/flyer/Flyer.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in plan() — app/src/main/java/com/propslab/sim/Autopilot.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in finding() — app/src/main/java/com/propslab/sim/LossInsights.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-002` Oversized unit: LossObservation — app/src/main/java/com/propslab/sim/LossInsights.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/sim/LossInsights.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-004` Mutable singleton state: SandboxHash — app/src/main/java/com/propslab/sim/SandboxModels.kt: Move state behind an injected repository/state holder.
- `DEBT-001` Long parameter list in SplitRing() — app/src/main/java/com/propslab/ui/components/Components.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/components/Motion.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-002` Oversized unit: BestViewModel — app/src/main/java/com/propslab/ui/screens/BestScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-002` Oversized unit: DetailViewModel — app/src/main/java/com/propslab/ui/screens/DetailScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-002` Oversized unit: PlayerLogUi — app/src/main/java/com/propslab/ui/screens/FantasyScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-002` Oversized unit: FeedScreenState — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-005` Talk-to-many class: FeedScreenState — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt: Split by caller: each public surface should belong to one responsibility.
- `DEBT-005` Talk-to-many class: FeedViewModel — app/src/main/java/com/propslab/ui/screens/FeedScreen.kt: Split by caller: each public surface should belong to one responsibility.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/FlyerScreen.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-002` Oversized unit: LabViewModel — app/src/main/java/com/propslab/ui/screens/LabScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/LabScreen.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-002` Oversized unit: LensesViewModel — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/LensesScreen.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-003` Deprecated API: onBackPressed — app/src/main/java/com/propslab/ui/screens/ManualActivity.kt: Use OnBackPressedDispatcher.
- `DEBT-002` Oversized unit: ModelShopScreen — app/src/main/java/com/propslab/ui/screens/ModelShopScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-002` Oversized unit: ParlayViewModel — app/src/main/java/com/propslab/ui/screens/ParlayScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-002` Oversized unit: SandboxScreenState — app/src/main/java/com/propslab/ui/screens/SandboxScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-001` Long parameter list in Stepper() — app/src/main/java/com/propslab/ui/screens/SettingsScreen.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-007` Repeated blocks within a file — app/src/main/java/com/propslab/ui/screens/SignalsCard.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-002` Oversized unit: SlateState — app/src/main/java/com/propslab/ui/screens/SlateScreen.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-001` Long parameter list in prop() — app/src/test/java/com/propslab/AgentToolsTest.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-005` Talk-to-many class: FeedTest — app/src/test/java/com/propslab/FeedTest.kt: Split by caller: each public surface should belong to one responsibility.
- `DEBT-001` Long parameter list in game() — app/src/test/java/com/propslab/GameModelTest.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in observation() — app/src/test/java/com/propslab/LossInsightsTest.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-001` Long parameter list in candidate() — app/src/test/java/com/propslab/ParlayLabTest.kt: Group the parameters into a named type; call sites become readable and defaultable.
- `DEBT-002` Oversized unit: ParlayLabTest — app/src/test/java/com/propslab/ParlayLabTest.kt: Split by responsibility (data access, policy, presentation) before adding behaviour.
- `DEBT-007` Repeated blocks within a file — app/src/test/java/com/propslab/ParlayLabTest.kt: Extract the repeated block once; two copies already drift apart.
- `DEBT-005` Talk-to-many class: SandboxTest — app/src/test/java/com/propslab/SandboxTest.kt: Split by caller: each public surface should belong to one responsibility.
- External review findings merged: 0

### Reviewer status

- **agent-audit** — not-installed · pip install agent-audit to enable its AGENT-* rules
- **kodus** — not-installed · requires a deployed service plus a model key; use its SARIF export, merged via AAD_REVIEW_SARIF.
- **brooks-lint** — not-installed · model-key driven; the deterministic DEBT-* rules above cover the same angle offline.
- **zeroshot** — not-installed · harness, not a scanner; no artefact contract to consume, so it is not wired into scoring.
- **git-lrc** — not-installed · git hook plus model key; commit-time by design, so a static audit cannot invoke it.
- **coderabbit** — unavailable · hosted service; cannot run against a local upload

## Agent surface (MCP, skills, instructions)

0 finding(s): 0 scored, 0 advisory. OWASP Agentic coverage: none.

## OSS analysis tools

- **gitleaks** — ok · 8.30.1 · 2.95s · 1 rule(s) assessed
  - Scanned 1 tree(s); 0 secret finding(s), 0 in app-origin files
- **semgrep** — ok · 1.182.1 · 8.14s · 6 rule(s) assessed
  - Applied 6 curated rule(s) to 1 tree(s); 0 match(es)
- **detekt** — ok · 1.23.8 · 16.0s · 0 rule(s) assessed
  - 1938 detekt finding(s); advisory, not scored
- **ktlint** — skipped · 0.0s · 0 rule(s) assessed
  - not installed
- **android-lint** — skipped · lint: version 9.3.0 [15859902] · 0.0s · 0 rule(s) assessed
  - Gradle project: standalone lint errors on Gradle projects. Run ./gradlew lint in a sandbox; it is not executed here.
- **detect-secrets** — skipped · 0.0s · 0 rule(s) assessed
  - not installed
- **agent-audit** — skipped · 0.0s · 0 rule(s) assessed
  - not installed
