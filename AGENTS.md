# AGENTS.md

Guidance for AI coding agents working in this repository. `README.md` is for humans.

## What this is

A native Android app (Kotlin, Jetpack Compose, Material 3). Offline tracker that answers "when did I last do this?": events, one-tap logging, history, optional target rhythm, backup, digest, widgets and app lock. No network permission, no account, no analytics. Domain rules (elapsed labels, status, stats, sorting) live in `core`; Room/Hilt/Compose in `app`.

## Hard constraints (do not break)

1. **Docker-only toolchain.** Never ask the user to install Java, Gradle, Android Studio or SDK packages on the
   host. Every build, test and `adb` command runs through `docker compose run --rm dev ...` (or `make`).
2. **Name only in `app.properties`.** The app name, id and URL are build parameters kept in the tracked
   `app.properties`. Do not hard-code the name in code, resources or docs: read it from `R.string.app_name` /
   `BuildConfig`. Internal names stay neutral (`App`, `app.lasttime`, `android-dev`).
3. **Native only.** No cross-platform frameworks, no WebView.
4. Do not commit secrets: `release.jks` and `keystore.properties` are git-ignored. Check `git status` before
   committing, and never `git add -A` blindly.
5. **Changelog.** Every user-visible change adds a line under `[Unreleased]` in `CHANGELOG.md`. CI
   (`.github/workflows/release.yml`) builds on GitHub's runner, not in Docker; the Docker-only rule is about the
   user's host. When a release is tagged, its entries move from `[Unreleased]` to a `## [x.y.z] - date` heading
   and `[Unreleased]` starts empty.

## Commands

```bash
make image      # once, and after Dockerfile changes
make check      # the standard check: unit tests and lint
make debug      # debug APK
make install    # needs a device/emulator
make release    # needs signing (README)
```

`RUN` defaults to the Docker wrapper; CI calls `make check RUN=` and `make release RUN=` so both use the same
Gradle commands. Keep the Makefile and the workflow in sync.

A change is done when tests pass, `lint` reports **0 errors**, and the release build still builds. Do not chain
`git commit` after a check with `&&` through a pipe or `grep`: a failed build can still let the commit run.

## Layout

```
core/             pure Kotlin/JVM, no Android dependencies, domain rules, JUnit tests
app/              Android application (namespace app.lasttime)
  src/main/kotlin/app/lasttime/   App, MainActivity, ui/theme
```

Keep domain logic in `core` and out of the UI. All randomness and time go through injected
dependencies so tests are deterministic.

## Conventions
- **Database:** Room is set up in the build (`app/schemas/` holds the exported schema). Any schema change needs a
  version bump and a real `Migration`; never use destructive migration. Commit the exported schema.
- **Dependency injection:** Hilt. `App` is `@HiltAndroidApp` and `MainActivity` is `@AndroidEntryPoint`; add
  `@HiltViewModel` view models and `@Module`s as the app grows.
- **UI text** lives in string resources. The app name comes from `R.string.app_name`, never hard-coded.
- **Accessibility:** touch targets at least 48dp, `contentDescription` on icon-only controls, and never colour as
  the only cue.

## Gotchas

- AGP 9 has built-in Kotlin: do not apply `org.jetbrains.kotlin.android`. `compileSdk` and `targetSdk` are 37;
  `minSdk` is 26.
- The launcher icon folder must stay `mipmap-anydpi-v26`. Lint suggests `mipmap-anydpi`, but that breaks the build.
- Paint the background behind the system bars (the root `Surface` fills the window; only content is inset),
  otherwise the light theme shows dark bars.
- The debug and release builds use different signatures: uninstall one before installing the other.
- A released keystore cannot be regenerated. Never delete or overwrite `release.jks`.

## App parameters

`APP_NAME`, `APP_ID`, `APP_URL`, `APP_VERSION_NAME`, `APP_VERSION_CODE` come from, in order: a `-P` Gradle property,
an environment variable, the tracked `app.properties` (holds `APP_NAME`, `APP_ID`, `APP_URL` only; the versions are
set by the release workflow), then the default in `app/build.gradle.kts`. Signing uses `SIGNING_KEYSTORE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` or `keystore.properties`.
