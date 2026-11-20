# Device test checklist

Mark each row after testing on a real phone. Note the phone model and Android version.

| # | Test | How | Expected | Result |
|---|------|-----|----------|--------|
| 1 | Sensor availability | Open the app | Tracking active, or a clear "no sensor" message | [ ] |
| 2 | Permission handling | Deny, then allow in settings | Message and button; tracking starts after allowing | [ ] |
| 3 | Counting, screen on | Walk 50 steps (count them) | Within about 10 percent after 1 minute | [ ] |
| 4 | Screen off tracking | Pocket, 50 steps, wait 2 minutes | Count increased | [ ] |
| 5 | App closed tracking | Remove from recents, 30 steps | Notification stays, count increased | [ ] |
| 6 | Duplicate prevention | Leave the phone still for 1 minute | Count does not change | [ ] |
| 7 | Phone restart recovery | Note count, restart, do not open the app, walk 30 steps | Notification returns by itself; count is old value + new steps | [ ] |
| 8 | Midnight reset | Walk before and after 00:00 | New day starts near zero; app open across midnight also resets | [ ] |
| 9 | Database persistence | Next day, Statistics > Day > previous day | Yesterday's total is still there | [ ] |
| 10 | Goal notification | Set goal 100, walk 100 steps | One notification; none again that day | [ ] |
| 11 | Streak and achievements | Reach the goal on consecutive days | Streak grows; first-goal achievement stays unlocked | [ ] |
| 12 | Battery optimization | Apply the in-app guidance, leave the phone for several hours | Tracking still running, steps counted | [ ] |
| 13 | Other Android versions | Install on another phone if available | Same behaviour | [ ] |

## Notes
- Long-press the version text on Home to open Diagnostics (sensor counter, last event time, boot count).
