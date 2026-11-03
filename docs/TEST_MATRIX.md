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
| D-pad reaches every control; Back exits every dialog | ⏳ | | |
| One click opens the most-used streaming apps | ⏳ | | |
| No animations or non-essential live updates | ⏳ | | |

## Functionality

| Check | Result | Date | Notes |
| --- | --- | --- | --- |
| Catalog matches real enabled launchable apps | ⏳ | | |
| Known brands map to installed packages | ⏳ | | |
| Unknown apps show a generic tile and open | ⏳ | | |
| Favorites/profiles persist across force-stop and reboot | ⏳ | | |
| Missing/disabled apps fail gracefully | ⏳ | | |
| No content pin implies access to provider watch history | ⏳ | | |
| Inputs work or are hidden | ⏳ | | |
| NextDNS tile real or visibly not connected | ⏳ | | |

## Safety and performance

| Check | Result | Date | Notes |
| --- | --- | --- | --- |
| Stock launcher, DNS and disabled-package set untouched | ⏳ | | compare with `docs/baseline/` |
| No key/token/secret in source, APK or logs | ⏳ | | |
| No background service or unneeded permission | ⏳ | | |
| 5+ min idle PSS / CPU / swap recorded vs stock | ⏳ | | |
| Reboot: remote, audio, HDMI, casting, playback OK | ⏳ | | |
| 20-minute navigation soak: no crash/ANR/growth | ⏳ | | |
| Force-stop Orrery → stock home still reachable | ⏳ | | |
| HOME replacement only with approval + proven rollback | — | | Phase 6 |
