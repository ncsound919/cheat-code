# PropsLab audit — 2026-10-30

Source snapshot `#14`, audited with the local Android Audit Desk on `127.0.0.1:8765`.

| | Before | After |
|---|---|---|
| Heuristic score | **0/100 (F)** | **74/100 (C)** |
| Critical | 2 | **0** |
| High | 15 | **0** |
| Scored findings | 34 | **13** (all `COMP-002`, hardcoded English copy) |
| Total findings | 1,685 | 1,650 |

The score is the desk's JEV triage heuristic. It is not a security certificate.

## What was fixed

- `getInstallSourceInfo` is guarded for API 29 phones; the deprecated installer lookup is the fallback.
- The manual's WebView now uses `OnBackPressedDispatcher`, blocks navigation outside `file:///android_asset/`, and disables content access, file-URL access and universal access.
- WorkManager failures are reported to the audit trail instead of being swallowed; the freshness worker now retries.
- Bootstrap records a partial-failure audit event instead of discarding failed steps.
- Access and refresh tokens are no longer stored: nothing in this release used them, and only the verified email remains on the device. The account copy no longer claims cross-device sync.
- Room migration DDL is a literal string per migration, not assembled by concatenation.
- `!!` assertions were removed from the agent tools and history import; malformed rows are refused with a reason.
- The unsafely ignored `live` flag in unfiltered Best picks is now honoured.
- Gallery saves delete a half-written MediaStore row instead of leaving an empty image.
- ML Kit recogniser is one process-wide client.
- Update and model downloads refuse non-HTTPS URLs; the auth project URL must be HTTPS.
- Target/compile SDK 36, AGP 8.13, release minification and resource shrinking with JNI/WorkManager keep rules.
- Backup and device-transfer extraction are disabled explicitly.
- ML Kit 16.0.1 removes the unaligned third-party 16 KB native library. The app's own JNI library links with `max-page-size=16384`.

## Verification

```bash
gradle testDebugUnitTest        # 248 tests, 4 live-network skips, 0 failures
gradle lintDebug                # 0 errors
gradle assembleRelease          # R8 + resource shrinking, signed
zipalign -c -P 16 -v 4 app/build/outputs/apk/release/app-release.apk
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

`zipalign` reports `Verification successful` and `apksigner` verifies the PropsLab certificate. The release APK is `app/build/outputs/apk/release/app-release.apk` (27 MB).

The 34 migration statements were also replayed in SQLite from schema 1 and compared with the exported schema 9: all 21 tables, every column type/nullability and every primary key match, and no index is missing.

Android Lint's remaining warnings are advisory: newer dependency notices, Compose modifier ordering, SharedPreferences KTX suggestions, and `OldTargetApi` because the installed SDK 37 is newer than the Play-required target 36.

## Remaining audit findings

### Scored (13 × LOW): hardcoded user-visible strings

`COMP-002` flags English copy passed directly to `Text(...)` across 13 screens. This is localization debt, not a runtime or security defect; the app ships English-only. Moving roughly 130 strings to `strings.xml` is the next clean-up, but it was not bundled into this repair because it touches nearly every screen and has no behavioral effect.

### Advisory only

- **Detekt style** (1,600 medium): magic numbers, line length, return count, test-method naming and Compose function naming. These are excluded from the score by the desk's advisory rule.
- **Brooks debt**: oversized repository/UI units and long parameter lists. Splitting `PropsRepository` and the large screens is a refactor, not a bug fix.
- **Reviewer false positives**, verified in source:
  - `REV-006` locale: the flagged formatters already pass `Locale.ROOT`.
  - `REV-007` wall clock: subscription expiry and cache freshness are calendar-time decisions; `elapsedRealtime` would be wrong.
  - `DEBT-003` on `ManualActivity`: the deprecated `onBackPressed` override is gone; the heuristic matches `handleOnBackPressed`.
  - `DEBT-004` mutable globals: `Licensing`, `ParlayStaking` and `SandboxHash` contain immutable constants and functions only.

Full report: [PropsLab-audit.md](PropsLab-audit.md) · machine tasks: [AGENT-FIX.md](AGENT-FIX.md) · raw JSON: [report.json](report.json).
