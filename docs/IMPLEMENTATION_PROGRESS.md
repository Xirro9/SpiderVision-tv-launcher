# Implementation progress

Source of truth: `references/APPROVED_ORRERY_COMIC_EDITION.png` (visual) and
`references/magicpath-original/src/components/generated/OrreryComicEdition.tsx` (details).
Scope, phases and safety rules: `references/IMPLEMENTATION_GUIDE.md`.

Status legend: done · in progress · pending · blocked

## Phase 0 — repo, source lock, environment, baseline

| Item | Status | Notes |
| --- | --- | --- |
| Repo created, Magic Path export committed unmodified | done | `references/magicpath-original/` |
| Approved screenshot committed, hashes recorded | done | see `DESIGN_FIDELITY.md` |
| Phase docs created | done | this file, `DESIGN_FIDELITY.md`, `TEST_MATRIX.md`, `ROLLBACK.md` |
| Toolchain | done | Android SDK (platforms 34/36, build-tools 35/36.1), Android Studio JBR 21, Gradle 8.14, AGP 8.13.1, Kotlin 2.2.20 |
| Device identity | done | `192.168.1.79:5555`, Philips_24NA_4K / `eleonas`, Android 14 (SDK 34), `UKN7.260716.014`, `armeabi-v7a` |
| HOME handler + disabled packages snapshot (read-only) | done | `docs/baseline/` |
| Idle performance baseline (Google TV home, settled) | in progress | preliminary capture taken while screensaver (`dreamx`) and Netflix were running; a settled home-screen capture is still needed |
| Cover art + fonts rights/availability | done (household use) | user-supplied collage, bundled locally; Bangers + Manrope are SIL OFL 1.1 |

## Phase 1 — native visual skeleton

App name: **SpiderVision** (package `com.spidervision.tv`), built from the Orrery Comic Edition design.

| Item | Status | Notes |
| --- | --- | --- |
| Compose TV app, banner, `LEANBACK_LAUNCHER` only, no HOME | done | no permissions requested; `<queries>` for TV/launcher intents only |
| Header, profiles, clock, cover, Watch panels, Favorites, Home base, Settings/Inputs/All apps, footer | done | 12×6 comic grid, sizes = prototype's 1920-px CSS values ÷ 2 |
| Tokens and fonts | done | Bangers + Manrope bundled (OFL, `licenses/`); no network fonts |
| 18 brand marks | done | ported path-for-path from the prototype to Compose drawing |
| Native focus ring | done | single yellow ring, drawn outside the panel |
| Installed on TV as a normal app, screenshot compared | done | `docs/screenshots/phase1-first-build.png` (2026-11-02) |
| Honest content | done | cover and Favorites are app shortcuts; network tile says "not connected"; Settings/Inputs/All apps report "later build" |
| Real app launch | done (early) | `getLeanbackLaunchIntentForPackage` with launcher fallback; full validation is Phase 2 |

First on-device numbers (debug build, idle on the board, 2026-11-02 22:33):

| Metric | SpiderVision (debug) | Stock launcherx (preliminary) |
| --- | --- | --- |
| TOTAL PSS | 66–78 MB settling | ~123 MB (+50 MB swapped) |
| CPU at idle | 0.0 % | not comparable yet |

The debug build is not minified; a release build and a settled stock baseline are needed before
claiming the < 60 MB target.

Known drift from the reference, to fix before Phase 2 sign-off:

- "Disney+" and "Crunchyroll" names truncate in the one-row Watch panels.
- The OEM "Art Time" overlay (`com.smartdevice.dailyshortcut`) can sit above any app after idle; it
  clears on the first remote press. Not caused by SpiderVision; noted for testing.

## Phase 2 — navigation and real apps

| Item | Status | Notes |
| --- | --- | --- |
| Deterministic D-pad map | done | `FocusMap.kt`: Left/Right use the panel's top row, Up/Down its left column; Up from the top row → active profile chip; Down from chips → cover; dead edges do nothing |
| Real app catalog | done | 27 enabled leanback apps; 17 designed brands mapped (Peacock not installed) |
| All apps board | done | every installed app; brands with custom marks (favourites first, wide), others with their own TV banner rendered at tile size, loaded only while the board is open |
| Launch + return | done | Netflix, YouTube, TV Settings opened from the remote; focus returns to the launching tile (home and All apps) |
| Back handling | done | Back closes All apps → focus on the All apps tile; Back on home exits to the system as normal |
| Inputs | done | opens the TV's real input picker (`com.android.tv.action.VIEW_INPUTS`): Google TV, Live TV, AV, HDMI 1–3 |
| Missing apps | done | missing brands drop out of Watch; a pinned missing app reads "Not installed" and launching reports why |
| Footer recomposition | done | selected-item text lives in its own scope, so D-pad moves don't recompose the board |

Remote D-pad walk verified on the TV (2026-11-02, via `adb input keyevent`, footer read back after each press):
cover → Netflix → YouTube → Home base → Settings → Inputs → All apps → YouTube Music pin → Spotify pin →
cover → Mahin chip → Aria chip → Mahin chip → cover → Netflix → Crunchyroll → Spotify pin →
Crunchyroll pin → YouTube pin → YouTube Music pin. All as designed.

Findings:

- Netflix and TV Settings ignore Back at their own root screen. Users leave them with Home, which
  goes to Google TV while SpiderVision is not the HOME app (Phase 6 decision).
- Cold start is 5.6–7.4 s for the debug build; to be measured on a release build in Phase 5.
- PSS ≈ 73 MB with the All apps banners loaded (debug).
- Still to do by a person: an unscripted pass with the physical remote from the couch.

## Phase 3 — persistent personalization

| Item | Status | Notes |
| --- | --- | --- |
| Per-profile saved state | done | Preferences DataStore (`LauncherStore`): favourites, app order, icon style, larger text, cover pick, 4 pins, plus the active profile. Apps stored by package, so any installed app works |
| App options from the remote | done | **Menu** key (or long-press OK) on any app panel, pin, cover or All apps tile → side sheet: favourite, Earlier/Later, Move to front, cover pick, pin to slot 1–4. Unavailable actions are disabled, not hidden |
| Settings sheet | done | Custom/original icons, Larger text (×1.15), Open TV settings, Reset layout (press twice), version |
| Honest controls | done | No Kids PIN, sleep timer or fake HDMI devices anywhere; network tile still "not connected" |
| Non-brand apps anywhere | done | Watch/pins/cover/chips fall back to the app's own TV banner or icon |
| Sheets as dialog windows | done | remote can't wander onto the board behind; Back closes; no dim, blur or animation |

Verified on the TV (2026-11-02, scripted remote presses):

- Menu on the Spotify pin → options sheet; "Add to favorites" + "Make it Mahin's cover pick" →
  Spotify became the 5th Watch tile and the cover.
- Force-stop + relaunch: Spotify still cover/favourite. **App update** (`install -r`): still kept.
- Profile switch to Aria → her own cover (Prime Video); kept after relaunch; Up from the board
  goes to the active chip.
- Settings: original icons + larger text applied across the board; Reset restored Mahin's defaults.
- All apps: Menu → Later moved Max after Crunchyroll, Move to front moved it back; focus followed
  the app. "Later" correctly disabled on the last favourite.
- HOME still `launcherx`, 19 disabled packages, no permissions requested.

Not yet verified: persistence across a **TV reboot** (needs a reboot; DataStore is app-private
storage, so it is expected to hold). Debug-build PSS after heavy use: ~85 MB; tuning is Phase 5.

## Phases 4–7

Pending. See the guide for exit gates.

## Device facts that shape the build

- Logical display 1920×1080 at density 320 → a **960×540 dp** canvas. The approved
  screenshot (966×537) maps almost 1:1 to dp, so layout is authored in dp against it.
- 1.84 GB RAM, ~370 MB available, ~796 MB in zram swap at capture time: memory is the
  constraint that matters most.
- 27 leanback-launchable activities; 17 of the 18 designed brands are installed
  (Peacock is not).
- Stock HOME: `com.google.android.apps.tv.launcherx/.home.HomeActivity`. Not to be changed
  without explicit approval (Phase 6).
