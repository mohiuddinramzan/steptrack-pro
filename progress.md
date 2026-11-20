# Progress

## Status
Phases 1-8 are implemented. The build is a debug APK. Nothing here is declared production ready.

## Verified by automated tests (run in GitHub Actions)
- Step delta logic: duplicates, reboot, stale values
- Date, midnight timing, distance, calories, formatting
- Statistics periods and summaries
- Streaks, achievements, validators

## Needs verification on a phone
Everything in docs/testing.md: sensor, permissions, screen off, app closed, reboot, midnight,
database persistence over several days, battery restrictions, other Android versions.

## Next action
Run the checklist in docs/testing.md over a few days and record the results there.
