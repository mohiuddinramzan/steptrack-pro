# StepTrack Pro

Offline step counter for Android, written in Kotlin with native Views. No internet permission, no accounts, no data leaves the phone.

## Features
- Live step count from the hardware step counter sensor (TYPE_STEP_COUNTER)
- Foreground service (health type) for tracking with the screen off or the app closed
- Restart after phone reboot or app update (boot receiver)
- Daily goal, distance, estimated calories, active time
- Day / Week / Month statistics with bar charts
- Walking streak, achievements and a daily goal notification
- Profile: height, weight and goal, stored on the device
- Premium dark theme, battery optimization guidance

## Tech
Kotlin 2.0.21, Android Gradle Plugin 8.9.1, Gradle 8.11.1, JDK 17, Room 2.6.1, Material 3.
minSdk 26, targetSdk 36. Built entirely from a phone with Termux, GitHub and GitHub Actions.

## Build
Push to `main` (or run the workflow manually). GitHub Actions runs the unit tests and builds
`app/build/outputs/apk/debug/app-debug.apk`, uploaded as the artifact `StepTrackPro-debug-<run number>`.
The version code is the workflow run number; the version name comes from `gradle.properties`
or the optional `version_name` input of the manual run.

## Privacy
The app asks for physical activity, notifications and boot-completed permissions only. Step data
is stored in a local Room database and is not backed up (`allowBackup=false`).

## Known limitations
- Phone makers (for example Vivo) may stop background services. Follow the in-app Battery guidance.
- Steps taken just before a shutdown may be lost, because the sensor counter restarts at boot.
- Distance and calories are estimates from height and weight.
- Past days keep the distance and calories calculated when they were recorded.
- The interface is English only. Only debug APKs are built; there is no signed release yet.
- Tested on one physical phone; behaviour on other Android versions and brands is unverified.

## Documents
`ROADMAP.md`, `CHANGELOG.md`, `progress.md`, `docs/testing.md`
