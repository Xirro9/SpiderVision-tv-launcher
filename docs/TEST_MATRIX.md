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
| Favorites/profiles persist across force-stop and reboot | ✅ | 2026-11-02 | force-stop ✅, app update ✅, TV reboot ✅ (Aria active + Prime Video pick + larger text kept) |
| Missing/disabled apps fail gracefully | ✅ | 2026-11-02 | missing pins/cover read "Not installed"; launch reports why; options disabled |
| No content pin implies access to provider watch history | ✅ | 2026-11-02 | pins and cover are labelled app shortcuts |
| Inputs work or are hidden | ✅ | 2026-11-02 | opens system input picker (HDMI 1–3, AV, Live TV) |
| NextDNS tile real or visibly not connected | ✅ / ⏳ | 2026-11-02 | not linked + rejected-key states ✅; live numbers vs NextDNS web pending the real key |

## Safety and performance

| Check | Result | Date | Notes |
| --- | --- | --- | --- |
| Stock launcher, DNS and disabled-package set untouched | ✅ | 2026-11-02 | HOME = launcherx, 19 disabled, unchanged |
| No key/token/secret in source, APK or logs | ✅ | 2026-11-02 | key only via ADB, encrypted on TV; test key absent from logcat and from the vault file in plain text |
| No background service or unneeded permission | ✅ | 2026-11-02 | no services; only INTERNET (NextDNS) |
| 5+ min idle PSS / CPU / swap recorded vs stock | ✅ | 2026-11-03 | release 30–36 MB vs launcherx 103–107 MB; free RAM equal or better (`docs/PERFORMANCE.md`) |
| Reboot: remote, audio, HDMI, casting, playback OK | ⏳ | 2026-11-02 | rebooted once: ADB, HOME, DNS, disabled set OK; remote in use after boot; audio/HDMI/casting/playback not yet checked |
| 20-minute navigation soak: no crash/ANR/growth | ⏳ partial | 2026-11-03 | stopped at ~6 min by the owner: 448 presses, no crash, 39–42 MB flat |
| Force-stop Orrery → stock home still reachable | ⏳ | | |
| HOME replacement only with approval + proven rollback | — | | Phase 6 |
