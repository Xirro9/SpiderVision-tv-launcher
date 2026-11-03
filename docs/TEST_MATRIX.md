# Test matrix

All results must come from the Philips TV (`Philips_24NA_4K`, Android 14). PC or browser
results do not count as passes.

Legend: ✅ pass · ❌ fail · ⏳ not yet run · — not applicable yet

## Design and UX

| Check | Result | Date | Notes |
| --- | --- | --- | --- |
| Screenshot matches approved composition, palette, hierarchy | ⏳ | | |
| Text readable from couch, nothing clipped at screen edges | ⏳ | | |
| Yellow focus ring visible on every control, including red/blue tiles | ⏳ | | |
| D-pad reaches every control; Back exits every dialog | ✅ | 2026-11-02 | scripted walk of every home panel + chips; Back closes All apps |
| One click opens the most-used streaming apps | ✅ | 2026-11-02 | Netflix, YouTube opened with one OK |
| No animations or non-essential live updates | ✅ | 2026-11-02 | 0.0 % CPU idle; clock ticks once a minute while visible |

## Functionality

| Check | Result | Date | Notes |
| --- | --- | --- | --- |
| Catalog matches real enabled launchable apps | ✅ | 2026-11-02 | 27 = `docs/baseline/leanback-launchable.txt` |
| Known brands map to installed packages | ✅ | 2026-11-02 | 17 of 18 (Peacock not installed) |
| Unknown apps show a generic tile and open | ✅ | 2026-11-02 | own banners; TV Settings opened from All apps |
| Favorites/profiles persist across force-stop and reboot | ⏳ | | |
| Missing/disabled apps fail gracefully | ⏳ | | |
| No content pin implies access to provider watch history | ⏳ | | |
| Inputs work or are hidden | ✅ | 2026-11-02 | opens system input picker (HDMI 1–3, AV, Live TV) |
| NextDNS tile real or visibly not connected | ⏳ | | |

## Safety and performance

| Check | Result | Date | Notes |
| --- | --- | --- | --- |
| Stock launcher, DNS and disabled-package set untouched | ✅ | 2026-11-02 | HOME = launcherx, 19 disabled, unchanged |
| No key/token/secret in source, APK or logs | ⏳ | | |
| No background service or unneeded permission | ✅ | 2026-11-02 | no services, no permissions |
| 5+ min idle PSS / CPU / swap recorded vs stock | ⏳ | | |
| Reboot: remote, audio, HDMI, casting, playback OK | ⏳ | | |
| 20-minute navigation soak: no crash/ANR/growth | ⏳ | | |
| Force-stop Orrery → stock home still reachable | ⏳ | | |
| HOME replacement only with approval + proven rollback | — | | Phase 6 |
