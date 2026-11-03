# Orrery Comic Edition: Android TV Implementation Guide

**Status:** Build specification and acceptance checklist, not a completed APK  
**Target:** Philips-24NA-4K (Skyworth `eleonas`), Android TV OS 14, build `UKN7.260716.014`  
**Approved design:** Orrery Comic Edition, as captured in `reference/APPROVED_ORRERY_COMIC_EDITION.png`  
**Primary source:** `magicpath-export/src/components/generated/OrreryComicEdition.tsx`  
**Design philosophy:** Design fidelity + reliable TV remote navigation + measurably low resource usage. No videos, particle effects, moving backgrounds, or constantly running background services.

## 0. Read this first

This handoff contains the exact Magic Path export, approved screenshot, this guide, and a launch prompt for Claude Code. **The exported code is React/TypeScript/Vite and is a visual prototype, not an Android launcher.** It should be studied and translated into a native Android TV application using Kotlin and Jetpack Compose for TV. Do not wrap the React app in a WebView.

The Magic Path share URL `https://www.magicpath.ai/files/468216304277549056` redirected to the Magic Path home page from the review environment. The actual code and screenshot in this handoff are the authoritative references. Treat other Magic Path designs as out of scope.

**Suggested workflow:** review the reference -> baseline TV performance -> create native project -> faithful static screen -> real app launching -> preferences -> native TV integration -> NextDNS stats -> performance signoff -> optional HOME replacement.

**Never disable/uninstall the stock launcher, reset TV settings, root, flash firmware, change existing DNS, or modify the previously debloated packages without explicit approval.** Keep the working backup and rollback scripts external to this project.

---

## 1. What is already in the design, and what is not implemented

| Prototype element | Found in export | Native implementation needed |
| --- | --- | --- |
| Orrery wordmark, dark comic palette, red/blue accents, cream panel borders, static web/halftone motifs | Yes, source definitions | Reproduce with Compose Canvas/VectorDrawable/static drawable assets |
| Editorial comic cover hero | Yes, **external Magic Path JPG** | Preserve art rights, acquire a local optimized source asset, bundle or provide offline fallback |
| 18 custom app icon drawings, original/custom icon toggle | Yes, generated SVG-like marks | Map to real apps, use original OS icons if necessary, add scalable native assets |
| Top six Watch slots and Pinterest-inspired 12-column board | Yes, responsive web CSS grid | Native fixed TV layout with deliberate D-pad focus order |
| 4 visual household profiles | Yes, hardcoded demo content | Local app profiles with persisted favorites; do not impersonate Google TV accounts |
| Hero "continue watching", episode names, progress | Hardcoded fiction | Hide or present clearly user-configured shortcuts unless a legitimate app integration exists |
| Favorites, reordering, icon mode, settings dialog | Browser state only | Persist with DataStore; restore focus on return |
| Apps grid | Hardcoded 18 app definitions | Query actual installed, enabled and launchable TV apps at runtime |
| Remote navigation | JS arrow-key spatial algorithm | Native D-pad focus with deterministic mapping, Back handling, selection |
| Launching apps | **No:** `launch()` only shows a toast/message | Android PackageManager launch intent and error handling |
| Network status and trend chart | **No:** `NET` contains static fictional values (18,402 queries, 2,318 blocked, 14 ms, six devices) | Secure NextDNS integration or honest unlinked state |
| HDMI 1/2/3 | **No:** sample PS5/soundbar + status message | Probe platform/OEM capabilities; hide or link to system Inputs if switching unsupported |
| Kids PIN / sleep-after-idle toggles | **No actual enforcement** | Remove until implemented and tested, or clearly mark unavailable |
| Default HOME activity | Not present | Try only after tested and approved; firmware may block reassignment |

### Key review findings from source

- Main component: `src/components/generated/OrreryComicEdition.tsx`, approximately 60 KB / 1,454 lines.
- `src/App.tsx` renders only `OrreryComicEdition`. `src/main.tsx` contains browser-specific animation and DOM setup.
- `src/index.css` and the component load Google-hosted **Bangers** and **Manrope** fonts. Avoid network font downloads at TV runtime. Check redistribution licenses, bundle approved font assets if licensed, and provide native fallbacks. An occasional `Marcellus` fallback reference appears in SVG text.
- Cover image comes from `https://storage.googleapis.com/storage.magicpath.ai/component-assets/468214371865219072/468215455874699264/bc3be34fa1fe12828de6cd70ceed772bc8ba0620fb8e98ce3977660ac079cbaa.jpg`. It is **not in the ZIP**. Do not rely on a third-party runtime URL. Download only if authorized and check image usage rights, otherwise provide a locally licensed alternative.
- Source contains 18 hardcoded apps, whereas the previously reported device inventory lists 28 visible app entries. The launcher must show actual installed apps and safely fall back for unrecognized packages.
- Native app must avoid a permanent service or frequent polling. A clock refresh once per minute is sufficient if it only runs while the launcher is visible.

## 2. Immutable visual contract: what Claude must preserve

The approved **Orrery Comic Edition** screenshot is the visual source of truth. Preserve:

- Top-left Orrery masthead and small "Living room / Vol. 1" strip.
- Top row of clearly indicated profile chips, and clock/date on the right.
- Large comic cover art on the left, with caption, title, metadata and optional progress strip.
- Compact editorial grid of TV apps to its right. Panels can have different sizes but must form a predictable remote-navigation map.
- Right-hand Home Base/NextDNS status tile and compact Settings, Inputs, All Apps tiles.
- Second-tier favorites or content pins aligned to the grid.
- Bottom focus/remote help line.
- Charcoal/black backgrounds, muted navy, classic suit red and blue, off-white/cream dividers, restrained web/halftone graphics.
- **High contrast yellow focus outline** visible on exactly one selected control.
- Flat solid surfaces and static illustration. No loops, shaders, WebView, blurs, shadows, parallax, video banners, continuously animated borders, or dynamically updating background art.

Colors sampled directly from the React source:

| Token | Value | Use |
| --- | --- | --- |
| `bg` | `#111216` | App background |
| `surface` | `#1A1C22` | Content panels |
| `suit` | `#0B0C0F` | Custom app tiles |
| `navy` | `#16203A` | Cool blue elevation |
| `text` | `#F2F3F5` | Main text |
| `silver` | `#B8BDC8` | Secondary text |
| `red` | `#E0353F` | Editorial accent |
| `blue` | `#4A7BE0` | Selected blue motifs |
| `cream` | `#F1E7CF` | Comic borders/captions |
| `focus` | `#FFD23F` | D-pad focus ring |

At 16:9, use reference screenshot alignment rather than arbitrary density scaling. The TV's panel is 4K, but Android may render the UI at a lower logical resolution: query `wm size`, `wm density`, actual display modes and screenshot resolution first. Account for screen overscan/safe margins. Preserve readable typography at couch distance. Use a simple static hero + structured grid rather than a true continuously reflowing masonry engine.

**Visual signoff:** Save native screenshot from TV, compare side by side with the supplied image, document deviations, fix layout and typography before adding new features.

## 3. Recommended architecture

**Stack:** Kotlin, Android Gradle Plugin/Gradle wrapper with compatible current SDK, Jetpack Compose for TV (`androidx.tv:tv-material`), standard Compose Foundation for layout/focus, Android PackageManager, Preferences DataStore, a bounded image loader (Coil or equivalent, if genuinely needed). Use Material TV components consistently; avoid dragging in unused libraries.

Suggested code organization:

```text
orrery-android/
  app/src/main/
    AndroidManifest.xml
    java/.../orrery/
      MainActivity.kt
      ui/         # HomeBoard, Hero, AppTile, Focus, Dialogs, DesignTokens
      apps/       # AppCatalog, PackageResolver, AppLauncher, IconProvider
      profiles/   # LocalProfiles, FavoritesRepository, SettingsRepository
      network/    # DnsStatus, NextDnsStatsRepository interface
      inputs/     # TvInputsGateway and capability detection
    res/drawable/ # Optimized approved artwork, vector icons, app banner
  docs/
    DESIGN_FIDELITY.md
    DEVICE_BASELINE.md
    TEST_MATRIX.md
    IMPLEMENTATION_PROGRESS.md
    ROLLBACK.md
```

Keep data and UI separate so network stats can be added later without refactoring the screen. Use immutable state and lifecycle-aware collection; refresh data only while visible. Store a small local profile model, not Android/Google account credentials. Cache decoded assets with strict sizes and avoid retaining offscreen images.

### Required Android manifest behavior

- TV-compatible activity: `MAIN` + `LEANBACK_LAUNCHER` so it appears as a normal TV app. Declare TV banner and TV feature support as appropriate.
- Initially **do not claim HOME**. Install as a regular app first.
- After testing, consider a separate/updated `MAIN` + `HOME` + `DEFAULT` activity intent filter if supported. Check whether Android resolves it as a HOME candidate and whether the firmware offers a selection UI; do not assume a normal Play Store app can override the Home key.
- Declare only the package visibility needed to discover TV launchable apps. Prefer `<queries>` intent declarations for `MAIN` + `LEANBACK_LAUNCHER` (and relevant launcher entries); request `QUERY_ALL_PACKAGES` only if genuinely needed and compatible with distribution policy.
- Use minimal permissions: internet only if implementing secure NextDNS data fetch, and no Accessibility Service or overlay by default.

Android references:

- [Compose for TV](https://developer.android.com/training/tv/playback/compose)
- [Create a TV app and LEANBACK_LAUNCHER](https://developer.android.com/training/tv/get-started/create)
- [TV focus guidance](https://developer.android.com/design/ui/tv/guides/styles/focus-system)
- [Package visibility](https://developer.android.com/training/package-visibility/declaring)
- [Leanback app launch intents](https://developer.android.com/reference/kotlin/android/content/pm/PackageManager#getLeanbackLaunchIntentForPackage(kotlin.String))

## 4. App discovery, icons, and actions

The React `APPS` list contains exactly these 18, for design reference only:

`Netflix`, `YouTube`, `Prime Video`, `Disney+`, `Max`, `Crunchyroll`, `Spotify`, `YouTube Music`, `Hulu`, `Apple TV`, `Peacock`, `Google TV`, `Tubi`, `Pluto TV`, `The Roku Channel`, `Instagram`, `Apple Music` (third-party app `com.brkchen.music`), `AirCast`.

Previously confirmed package IDs useful for mapping, not hardcoded launch activity names:

| App | Package |
| --- | --- |
| Netflix | `com.netflix.ninja` |
| YouTube | `com.google.android.youtube.tv` |
| Prime Video | `com.amazon.amazonvideo.livingroom` |
| Disney+ | `com.disney.disneyplus` |
| Max | `com.wbd.stream` |
| Crunchyroll | `com.crunchyroll.crunchyroid` |
| Spotify | `com.spotify.tv.android` |
| YouTube Music | `com.google.android.youtube.tvmusic` |
| Hulu | `com.hulu.livingroomplus` |
| Apple TV | `com.apple.atve.androidtv.appletv` |
| Peacock | `com.peacocktv.peacockandroid` |
| Google TV | `com.google.android.videos` |
| Tubi | `com.tubitv` |
| Pluto TV | `tv.pluto.android` |
| The Roku Channel | `com.roku.web.trc` |
| Instagram | `com.instagram.airwave` |
| Apple Music-style app | `com.brkchen.music` |
| AirCast | `com.smartdevice.tv.aircast` |

**Implementation requirements:**

- Read current installed/enabled TV launchable packages dynamically. Treat disabled, missing, or updated apps gracefully.
- Resolve a TV-specific launch intent using `PackageManager.getLeanbackLaunchIntentForPackage()`, with an appropriate fallback for installed apps that expose a regular launcher activity. Do not launch by blindly constructing component class names.
- Validate each major app opens from the new launcher, including the Back path and returning focus to the prior tile.
- Maintain a user-editable **favorites** subset and order, stored in DataStore and recoverable after restart.
- Keep app logos recognizable. Use native app icons as fallback. Recreate the existing 18 stylized vector marks using approved vector/raster assets; don't assume web SVG JSX can be pasted directly into Compose.
- Unrecognized apps need a correctly labeled generic tile, not a crash or an invisible app. The total should match the device's current launchable-app inventory, not a hardcoded `18 installed`.
- Avoid showing system services, overlays, accessibility packages, or disabled packages as entertainment apps.

**Content pins and Resume buttons:** The prototype hardcodes fictional films and playlists. In MVP, treat these as configurable **app shortcuts** with honest labels. Deep-linking into specific shows, playlists or episodes is an optional later phase and requires a verified provider-supported intent/link. Do not imply that Netflix/Disney/Hulu browsing history can be read across apps without official integrations.

## 5. Profiles, settings, and navigation

- Four launcher-local profiles may remain as an aesthetic choice. Their favorites, order, icon style, and hero selection must persist independently. They are **not** Google TV accounts or access boundaries.
- Expose profile switching without blocking normal streaming app logins. Kids profile needs a real PIN gate only if implemented and security tested; otherwise hide the switch, rather than falsely promising enforcement.
- Focus movement: map directional arrows to adjacent panels deterministically. Test every large/small tile, side drawer, app grid, input list, settings, exit/back action and nested sheet. There must be no dead ends.
- Pressing Back closes a dialog and restores prior focus; Back from home should not trap the user.
- OK/Select launches the focused app or action; physical Home behavior is system controlled until the default HOME experiment is approved.
- Inputs: verify a supported intent or OEM API for the Philips firmware. Android third-party apps generally cannot assume direct hardware input switching privileges. If unavailable, provide a clearly labeled path to the TV's input chooser, or defer the tile. Do not show fake HDMI device names.
- Clock: update while UI is resumed/visible, approximately every minute. No always-on ticker.

## 6. NextDNS integration, without introducing a home server requirement

The TV's existing Android Private DNS is configured to the user's NextDNS profile. **Do not change it as part of the launcher.** The concept's Network widget is entirely sample data. The real app must initially show an honest state such as **"NextDNS stats not connected"** and may show a separately verified system DNS mode indicator.

The documented NextDNS API is in beta: [official API reference](https://nextdns.github.io/api/). Useful authenticated endpoints include:

- `GET /profiles/{profile}/analytics/status` for blocked/allowed/default query counts.
- `GET /profiles/{profile}/analytics/domains?status=blocked` for top blocked domains.
- `GET /profiles/{profile}/analytics/status;series` for trend chart intervals.
- `GET /profiles/{profile}/analytics/devices` to find the configured TV device filter.

Do not present DNS blocked queries as the number of visible advertisements removed. Do not claim network-wide stats; this DNS profile currently serves the TV, not the whole home.

**Authentication design:** The API uses an `X-Api-Key` account key with broad account access. Never embed this key in APK assets, GitHub, logs, build config, hardcoded strings, screenshots, or client JavaScript. Preferred production solution is an authenticated read-only proxy that returns limited aggregated stats. If no always-on proxy is available yet, defer the live feature. An encrypted-at-rest local key can be offered only as a clearly explained optional tradeoff; Android Keystore cannot make a secret fully inaccessible to a compromised client. No need to install a Pi-hole or an always-on NextDNS app on the TV.

Refresh only when the network tile is visible or detail panel is open, cache the latest aggregate values, back off on HTTP failure, use short connect/read timeouts, and avoid a persistent background poller. Show last-updated time and an error/unavailable state. Disable logging of raw browsing domains unless necessary and approved.

**Acceptance:** Never show mock counts as live. Make an API request through the approved architecture, compare counts against NextDNS web Analytics for the same period, and test no-network and unauthorized states.

## 7. Performance budget for this specific TV

Evidence from prior ADB profiling indicates about 1.84 GB physical RAM reported by Android, significant compressed swap usage and a high rate of reclaim/page faults. The original Google TV home screen has heavy live recommendation/banner work. Speed cannot be inferred just from the new app's PSS: the original launcher may continue running.

**Provisional targets, to validate on device:**

- Prefer a native launcher process **under 60 MB PSS** at settled idle, if feasible; report truthfully if design assets exceed it.
- No recurring background service. No full-screen video/banner playback or live wallpaper.
- Avoid repeated disk reads and network requests during D-pad navigation.
- Aim for responsive focus changes (e.g. under 100 ms median measured input-to-visual response) and no visible stutter. Compare actual measurements rather than asserting goals are met.
- Decode hero artwork near display size, not original 4K if logical UI composition only needs ~600-900 px across. Avoid very large decoded images and large bitmap pools.
- Bundle two or three tested asset resolutions as needed; prefer static WebP for artwork, vectors for linework/icons, fallback font resources.
- Disable work when Activity is paused. No unbounded recomposition or polling.

**Baseline and post-build instrumentation:**

1. Same TV state, same Home/launcher screen, after 10-15 minutes settled after boot. Repeat measurements several times.
2. Read PSS (`adb shell dumpsys meminfo <your.package.id>`), process CPU (`top`/`dumpsys cpuinfo`), `dumpsys meminfo`, swap / memory pressure signals available on that firmware, and low-memory kills.
3. Record cold start, warm resume, remote focus navigation, app launching, back/return and 15-minute idle consumption.
4. Compare total system memory pressure and old Google launcher residency. A small custom app is not a win if both launchers constantly remain active and reclaim increases.
5. If initial design is too heavy, reduce image dimensions and layout complexity before adding more animations (the approved design intentionally has none).

## 8. Phase-by-phase execution and stop/go criteria

### Phase 0: repo, source-lock, environment, baseline

- [ ] Create a new repo (suggested `orrery-tv-launcher`) and commit unmodified Magic Path export under `references/magicpath-original/` or equivalent.
- [ ] Commit approved reference screenshot. Record source hash/tag to prevent later design mixing.
- [ ] Create `docs/DESIGN_FIDELITY.md`, `docs/IMPLEMENTATION_PROGRESS.md`, `docs/TEST_MATRIX.md`, `docs/ROLLBACK.md`.
- [ ] Install Android Studio and compatible JDK/SDK/Gradle wrapper on Windows; confirm build tools and ADB.
- [ ] Verify target device build and Android version. Discover actual ADB serial. Multiple ADB transports may exist; always use `-s` when ambiguous.
- [ ] Snapshot the TV's system HOME handler and already-disabled package state **read-only**.
- [ ] Establish comparable idle performance baseline with current Google TV launcher.
- [ ] Confirm rights and availability of full-resolution cover image and any additional artwork/fonts.
- **Exit gate:** Baseline + original backup retained, design frozen, native build toolchain ready.

### Phase 1: native visual skeleton

- [ ] Create minimal native Android TV app using Compose for TV, TV banner, only necessary permissions, `LEANBACK_LAUNCHER` entry.
- [ ] Recreate static Orrery header, profile chips, clock/date, hero, tiles, right sidebar, comic line work, footer.
- [ ] Port exact design tokens and fonts legally, or document approved substitutes.
- [ ] Remove browser-only CSS/JS behavior and use native focus indicators.
- [ ] Install and launch APK as a **normal app**. Do not claim HOME.
- [ ] Capture TV screenshots, compare with approved PNG and fix obvious drift.
- **Exit gate:** Visually faithful static home screen, no crashes, no surprise background processes.

### Phase 2: navigation and real apps

- [ ] Load real enabled launchable apps from PackageManager; map the 18 known brand treatments and generic fallback icons.
- [ ] Make the 6 watch tiles, All Apps grid, Favorites, Settings, and Back navigation functional.
- [ ] Build and test explicit D-pad focus order across mixed-size tiles.
- [ ] Launch every representative streaming app and return successfully to Orrery.
- [ ] Recover gracefully if an app was uninstalled, disabled or needs a login.
- [ ] Never rely on the hardcoded web `launch()` message as proof of functionality.
- **Exit gate:** Remote-only usability passes, real app launching works.

### Phase 3: persistent personalization

- [ ] Implement local user profiles, saved favorites, app ordering, icon-style toggle and large-text setting.
- [ ] Persist all preferences across force-stop, reboot and app update.
- [ ] Replace fictional Continue Watching tiles with honest configurable shortcuts, unless validated real provider integrations are available.
- [ ] Hide unfinished Kids PIN, sleep and HDMI actions rather than showing switches that do nothing.
- **Exit gate:** Every exposed control has real behavior; nothing is misleading.

### Phase 4: network widget

- [ ] Initially show a clear NextDNS-unlinked placeholder, not the sample statistics from the design.
- [ ] Implement a NextDNS statistics repository interface behind the widget.
- [ ] Select credential-safe data delivery (preferred read-only proxy, or knowingly defer this feature).
- [ ] Display actual blocked DNS queries, percent and historical trend with correct time windows, cache and last-updated label.
- [ ] Test unavailable network, expired auth, low query volume and profile mismatches.
- **Exit gate:** Numbers independently verifiable in NextDNS portal; no exposed account key.

### Phase 5: profile and optimize on TV

- [ ] Run repeatable CPU/PSS/pressure tests vs original launcher and document raw measurements.
- [ ] Profile D-pad navigation, startup, resume and scrolling; reduce excessive image or recomposition work.
- [ ] Verify Netflix, YouTube, Disney+, streaming audio, volume, casting, Bluetooth remote, Wi-Fi and HDMI behavior are unaffected.
- [ ] Have roommates perform unscripted remote-only usability testing from normal viewing distance.
- [ ] Add automated unit/UI tests for app catalog, ordering, focus movement, persistence and stat display.
- **Exit gate:** Stable performance at least competitive in real use, no broken core TV functions.

### Phase 6: optional HOME replacement experiment (explicit approval required)

- [ ] Capture one more launcher/system baseline and verify a manual fallback route.
- [ ] Determine whether firmware will recognize a third-party HOME activity using only read-only probes first.
- [ ] Ask for explicit approval before registering/changing HOME behavior or changing any default.
- [ ] Test Home key, cold boot, reboot, account picker, app return, Netflix/YouTube, remote and HDMI.
- [ ] If unsupported, preserve Orrery as a normal launchable app; do not use risky root/package disabling to force it.
- [ ] Keep the stock Google TV launcher enabled and document easy recovery.
- **Exit gate:** HOME key and boot behavior proven on device or documented as unsupported, never assumed.

### Phase 7: release and handoff

- [ ] Commit source and README with before/after screenshots, installation steps, limitations and exact build commands.
- [ ] Build signed **private** release APK with keystore backed up securely off-repo. Do not commit signing secrets.
- [ ] Save versioned APK and SHA-256 to release assets (no credentials or licensed art without permission).
- [ ] Update `IMPLEMENTATION_PROGRESS.md` with everything complete, pending, blocked and tested.
- [ ] Preserve a manual recovery route and install/run documentation.
- **Exit gate:** A new developer can rebuild and install, and a roommate can use the launcher without explanation.

## 9. Windows and ADB installation runbook

The TV previously appeared at `192.168.1.79:5555`, but this address/port can change. Confirm the current ADB device and **never assume the old serial is still valid**.

```powershell
# From the folder containing adb.exe or with adb.exe on PATH:
.\adb.exe devices -l
.\adb.exe connect 192.168.1.79:5555  # only if needed and still correct
.\adb.exe devices -l

# Set serial to the real online device shown by 'devices -l':
$tv = '192.168.1.79:5555'
.\adb.exe -s $tv shell getprop ro.build.id
.\adb.exe -s $tv shell wm size
.\adb.exe -s $tv shell wm density

# After Claude has built an APK, replace with its actual output path:
.\adb.exe -s $tv install -r 'C:\path\to\orrery\app-debug.apk'
```

If ADB lists the same TV twice, use one explicit serial via `-s`. Install commands must target the custom Orrery package only; do not `pm disable-user` or uninstall any stock package. To test the new app manually, launch it from Google TV's app list or use its **exact** activity component determined by `aapt`/package inspection. Do not guess the activity or package ID.

**Safety:** Physical remote control and Settings access must be maintained. Keep the prior TV backups intact, and use Android's Settings > Apps > Orrery > Uninstall if abandoning the custom app. Only uninstall the newly installed app package, never the stock launcher. For Home experiment recovery, document the actual on-device path to reselect the stock Home app before attempting any changes.

## 10. Test matrix with acceptance checklist

### Design and UX

- [ ] Screenshot matches approved comic board composition, palette and visual hierarchy.
- [ ] Text is readable from couch distance; no clipping or cropped 4K margins.
- [ ] Yellow focus ring is always clearly visible, even on red/blue tiles.
- [ ] D-pad reaches every interactive control; Back reliably exits dialogs.
- [ ] One click gets into the most-used streaming apps.
- [ ] No animations or nonessential live updates.

### Functionality

- [ ] Dynamic app catalog reflects actual enabled launchable apps.
- [ ] All 18 known app brands map to real packages where installed.
- [ ] Unknown/new apps show a generic icon and open correctly.
- [ ] Favorites and local profiles persist across reboot.
- [ ] Missing/disabled apps fail gracefully.
- [ ] Content pins never falsely promise access to provider watch histories.
- [ ] HDMI actions work or are explicitly hidden as unsupported.
- [ ] NextDNS stats are real or visibly not connected, never fictional.

### Safety and performance

- [ ] Stock Google launcher and existing DNS/debloat changes untouched.
- [ ] No key, personal token, or client secret in source, APK or logs.
- [ ] No permanent background process or unnecessary permission.
- [ ] Record 5+ minute idle PSS, CPU, swap pressure, navigation latency, app open time.
- [ ] Reboot and test TV remote, audio, HDMI, casting, streaming playback.
- [ ] 20-minute navigation test has no crashes, ANRs, unexpected memory growth.
- [ ] Force-stop custom app and verify stock home remains reachable.
- [ ] HOME replacement only with explicit approval and proven rollback.

## 11. Operational decisions and open items

These are **not blockers to beginning Phase 0/1**. Claude should use the defaults below until directed otherwise.

1. **Hero art:** Original cover is on a hosted URL and absent from the ZIP. Default: package an approved local image or safe placeholder. High-res original export is preferred.
2. **20+ other app icons:** Design contains 18 apps; earlier TV inventory saw 28 visible app entries. Default: dynamically include remaining real launchable entries with ordinary OS icons. Do not invent brand logos.
3. **Profiles:** Default: launcher-local profiles named as in approved design, editable later, no Google account integration or functioning parental gate unless built.
4. **Content pins:** Default: direct app shortcuts with editable labels, no fake streaming progress.
5. **Inputs:** Default: Settings/OS input chooser or hide if no supported API; no fake HDMI device map.
6. **NextDNS:** Already configured at OS level. Default: network tile is unlinked until secure auth architecture exists. Do not require a Pi-hole/home server now.
7. **Brand/trademark artwork:** For personal household use, retain the existing aesthetic. If publicly releasing the APK or repository, verify rights to hero art, Spider-Man imagery, third-party logos and bundled fonts; use independently owned substitutes where needed.
8. **HOME replacement:** Default: do not touch. Users can run Orrery from installed apps until the separate approval gate.

## 12. Final success definition

The project is successful when the **actual Philips TV** runs a visually faithful native Orrery interface; a person with only the remote can select profiles, navigate, open apps, save favorites, reach Settings and recover from failures; the NextDNS tile never shows fictional data; and benchmark evidence shows no unacceptable increase in memory pressure or lag. Deliver the working installable APK and source, not just browser screenshots or a Vite build.
