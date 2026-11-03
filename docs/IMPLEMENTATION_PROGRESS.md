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

TV reboot (2026-11-02 23:08, with the owner's go-ahead): before, Aria active with Larger text on.
After boot (wireless ADB reconnected on its own, ~20 s to `boot_completed`), SpiderVision opened
with Aria active and her Prime Video pick; the saved file still had `aria.largeText = true`.
HOME (`launcherx`), Private DNS (`hostname`) and the 19 disabled packages were unchanged.
Cold start after reboot: 393 ms reported by `am start -W` (Activity only; process was pre-warmed).

Testing note: a scripted OK press sent before the board had focus opened Netflix (nothing
played). Scripts now read the selected item before pressing OK.

Debug-build PSS after heavy use: ~85 MB; tuning is Phase 5.

## Phase 4 — network widget (NextDNS) and real profile names

| Item | Status | Notes |
| --- | --- | --- |
| Profile chips = the household's Google TV profiles | done | Mahin, Tabitha, John, Raph. Android doesn't let apps read or switch Google TV profiles (4 Google accounts + 1 TV profile user found read-only; the profile chooser only accepts a private Google intent), so each chip keeps its own SpiderVision layout |
| Stats repository behind the tile | done | `NextDnsClient` (HTTPS, no extra libraries), `NetView` states |
| Credential delivery | done | owner chose "key stored on the TV": ADB-only `ProvisionReceiver` + Keystore AES-GCM vault; `tools/link-nextdns.ps1` |
| Honest states | done | not linked / loading / live / offline with cached numbers / check key; empty chart frame when no data |
| Refresh only while visible | done | 10 min interval, exponential backoff, no service |
| Tested on TV | partial | not linked ✅, malformed key rejected ✅, fake key → "NextDNS rejected the saved key" via a real API call ✅, key absent from logcat ✅, unlink ✅ |
| Verified against NextDNS web | deferred | owner's first link attempt never reached the TV (script hardened since); owner chose to skip linking for now (2026-11-02). Tile stays "NOT LINKED" |

New permission: `INTERNET` (only for NextDNS).

## Phase 5 — profile and optimise on the TV

| Item | Status | Notes |
| --- | --- | --- |
| Release build measured vs stock | done | 30–36 MB vs 103–107 MB; free RAM equal or better; cold 318–355 ms, hot 115–130 ms |
| D-pad smoothness | done | panel render layers + cached panel art: median frame 57 → 15 ms, janky 100 % → 11 % |
| Unit tests | done | 8 JVM tests: app ordering/favourites/moves and the D-pad map (reachability, no dead ends) |
| Soak | partial | stopped at ~6 of 20 min by the owner; no crash, flat memory |
| Playback, audio, casting, HDMI, Bluetooth, Wi-Fi after reboot | owner check | listed in `docs/PERFORMANCE.md` |
| Roommate usability test | owner check | |

Full numbers: `docs/PERFORMANCE.md`.

## Phase 6 — HOME replacement experiment

Done with owner approval, then rolled back: the firmware keeps the Home button on Google TV's own
system Home activities (`launcherx`, then `setupwraith`) regardless of the HOME role. Backup and
verified rollback in `platform-toolsackup-2026-11-03_home-phase6`. Details:
`docs/HOME_EXPERIMENT.md`. Then (owner-approved) `setupwraith` was disabled as well: **SpiderVision is now the TV's Home**,
survives a reboot, and the Google TV home no longer runs (Used RAM ~67 MB lower, swap ~177 MB
lower). The rollback script re-enables both packages.

## Phase 7

Pending.

## Device facts that shape the build

- Logical display 1920×1080 at density 320 → a **960×540 dp** canvas. The approved
  screenshot (966×537) maps almost 1:1 to dp, so layout is authored in dp against it.
- 1.84 GB RAM, ~370 MB available, ~796 MB in zram swap at capture time: memory is the
  constraint that matters most.
- 27 leanback-launchable activities; 17 of the 18 designed brands are installed
  (Peacock is not).
- Stock HOME: `com.google.android.apps.tv.launcherx/.home.HomeActivity`. Not to be changed
  without explicit approval (Phase 6).

## Owner fixes (2026-11-03)

| Request | Done | Verified on TV |
| --- | --- | --- |
| No duplicate Max; Instagram → AirCast | Tabitha's pins are now Hulu, YouTube Music, AirCast, Apple Music (defaults + one-time migration of saved pins) | ✅ |
| "…'s favorite" = most-opened app | SpiderVision counts each profile's launches; the big panel shows the most-opened app not already pinned beside it, else slot 1 | ✅ Netflix → "Most opened · Opened 1 time" |
| Rotating "Tonight's issue" | Netflix → YouTube → Disney+ → Prime Video → Crunchyroll → Max every 5 s while on screen, position dots; OK opens what's showing | ✅ |
| YouTube Music works | opens; the YouTube Music TV app itself hands off to the YouTube app (Google's behaviour, same from Google TV) | ✅ |
| Inputs / HDMI | Inputs tile → system picker → HDMI 1 switched (PassthroughPlayerActivity, "No signal": device in standby) → Home back to SpiderVision | ✅ |
| TV function checks | speaker active, not muted; Chromecast built-in + AirCast running; Bluetooth on with the BLE remote; Wi-Fi on; HDMI 1–3 report a connected device in standby | ✅ (read-only) |

## Polish pass (2026-11-03, owner request: "sleek, snappy animations, Spider-Man sounds, low RAM")

| Added | Detail | Cost |
| --- | --- | --- |
| Focus lift | selected panel scales up 3 % in 150 ms with the yellow ring fading in; drawn above neighbours | transforms the cached panel texture only |
| Cover cut | rotating title slides up (comic-panel cut), "Play!" burst pops, position dots glide | ~250 ms every 5 s |
| Sheets | side sheets slide in from the right; switch knobs glide | 200 ms |
| Board entrance | board and All apps slide up 18–24 px as they open / return | 220 ms |
| Speech bubble | status messages pop in with a small bounce | — |
| Spider-sense | profile switch spins the masthead web and plays a two-note tingle | 560 ms |
| UI sounds | original synthesized web-tick (focus), thwip (open), zip (back), spider-sense (profile), buzz (can't open); `tools/make_sounds.py`; 37 KB total; quiet; rate-limited; per-profile **Sounds** switch | SoundPool, 3 streams |
| Number keys | remote 1–4 switch to Mahin / Tabitha / John / Raph | — |

Measured on the TV (release): 40 D-pad moves → 455 frames, median 19 ms, 90th 34 ms, janky 7.7 %
(slowest frames are the cover title slide); PSS 46 MB (was 36 MB; still < 60 MB target and about
half of Google TV's home). Number keys verified. Sounds can't be verified audibly from the PC.

## Art pass (2026-11-03, owner request: "use these images, less minimal, gorgeous, startup animation")

| Added | Detail |
| --- | --- |
| Owner artwork | 7 supplied images + the original collage in `art-src/` (household use only). `tools/make_art.ps1` crops each to the exact size it's shown at: 18 app tiles (400×300), 4 utility panels, 6 cover posters (600×860). 1.7 MB in the APK |
| Art tiles | every designed app (Watch, All apps, chips, Favorites) shows its own scene under a dark scrim, logo on a dark plate, brand-colour stripe along the bottom, cream web in the corner |
| Panels | Settings (symbiote), Inputs (comic-panel hand), All apps (streak lettering), Home base (Daily Bugle Miles under a navy wash), big favourite (its app's art) |
| Cover | each featured app brings its own poster; posters crossfade as the issue rotates |
| Intro | live-drawn spider-web build, a spider dropping on its thread, comic POW burst + logo with spider-sense and thwip, punch-out into the board; ~1.6 s, any key skips, once per TV boot |
| Cascade | board panels pop in one after another after the intro; All apps tiles cascade in |
| Memory | `ArtImages`: 16-bit decode off the main thread, 8 MB LRU |

Measured on the TV (release, with focus art-zoom): PSS 54 MB; 20 D-pad moves → median 24 ms,
90th 46 ms. The focus art-zoom was then removed (it forced a full redraw of the tile each frame)
and the intro limited to once per boot; that final build was installed but the TV went offline
(standby) before it could be measured or the intro rule checked.
