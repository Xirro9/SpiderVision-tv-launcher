# Performance — Phase 5

All numbers are from the Philips 24NA 4K (Android 14, 1.84 GB RAM, `armeabi-v7a`) on
2026-11-03, using the **release** build (R8-minified, 2.4 MB APK) unless stated. Raw captures are
in `docs/perf/`.

## Memory (settled 3 min, 3 samples 40 s apart)

| State | Google TV launcher (`launcherx`) | SpiderVision | Free RAM (system) | zRAM in swap |
| --- | --- | --- | --- | --- |
| Google TV home on screen (SpiderVision stopped) | 103–107 MB | — | 529–541 MB | 516–540 MB |
| SpiderVision on screen (`launcherx` in background) | 78–104 MB | **30–34 MB** | 536–554 MB | 529–540 MB |
| SpiderVision after cached render layers | — | **36 MB** | — | — |

- SpiderVision is well under the guide's 60 MB target (debug builds were 66–85 MB).
- The stock launcher stays resident as HOME and trims itself to ~78 MB once in the background, so
  running SpiderVision on top did not reduce free RAM or raise swap versus the stock home.

## Startup (`am start -W`, release)

| Kind | Runs | Time |
| --- | --- | --- |
| Cold (force-stopped) | 5 | 318, 335, 355, 318, 329 ms |
| Hot (back from Home) | 5 | 130, 119, 127, 115, 115 ms |

## D-pad responsiveness (40 scripted arrow presses, `dumpsys gfxinfo`)

| Build | Frames | Median | 90th | 99th | Janky | Slow UI thread | Slow draw commands |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Release, single layer | 72 | 57 ms | 61 ms | 65 ms | 100 % | 71 | — |
| + panel render layers | 72 | 53 ms | 57 ms | 61 ms | 98.6 % | 5 | 71 |
| + cached panel art (shipped) | 72 | **15 ms** | **17 ms** | 18 ms | **11 %** | 5 | **0** |

Cause and fix: every focus move re-recorded and re-issued the whole 1920×1080 board (≈3,500
halftone dots, web corners, brand paths, cover art) to a weak GPU. Each panel is now its own
render layer whose artwork is cached as a texture (`CompositingStrategy.Offscreen`); the yellow
focus ring is drawn outside that cache, so a move redraws only two rings. Cost: ~2 MB.

## CPU

Neither launcher appeared among the TV's eight busiest processes in any idle snapshot
(system_server 3–6 % was the top entry). Earlier debug measurements showed SpiderVision at 0.0 %
when idle. The clock ticks once a minute; NextDNS refreshes only while visible.

## 20-minute D-pad soak

Stopped early at the owner's request after ~6 minutes (448 random arrow presses, no OK/Menu/Back).
Within that window: same process throughout (no restarts), 0 crash-buffer entries, SpiderVision
stayed in front, memory 39.4 → 41.6 → 40.2 MB (no upward trend). The full 20-minute run is
not done; `tools/soak.sh` can repeat it later (e.g. overnight).

## Not measurable from the PC (owner checks)

These need a person in the room; SpiderVision doesn't touch any of them, but they should be
confirmed once with the release build:

- [ ] Netflix / YouTube / Disney+ playback starts and sounds normal after opening from SpiderVision
- [ ] Volume and mute on the remote
- [ ] Casting to the TV (AirCast / Google Cast)
- [ ] Bluetooth audio, if used
- [ ] HDMI source switching via the Inputs tile
- [ ] Wi-Fi stays connected after a reboot
