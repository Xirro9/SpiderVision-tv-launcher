# Changelog

## 1.0.0 — 2026-11-03

First signed release, installed on the living-room Philips 24NA 4K as the Home app.

**Board**
- Comic-book home board from the approved Orrery Comic Edition design, 12 × 6 grid, deterministic
  D-pad navigation.
- *Tonight's issue* rotates Netflix → YouTube → Disney+ → Prime Video → Crunchyroll → Max every
  5 s with its own posters; OK opens what's showing.
- Owner artwork on every app tile and utility panel; All apps board with all 27 installed apps.
- Each profile's big panel shows its most-opened app.
- Inputs tile opens the TV's HDMI / Live TV picker.

**People**
- Profiles Mahin, Tabitha, John and Raph with their own order, pins, cover, large text, icon style
  and sounds, saved across reboots; remote keys 1–4 switch.
- Menu on any app: add/remove favourite, move earlier/later, pin to a profile slot.

**Feel**
- Spider-web intro once per TV boot (any key skips), panel cascade, focus lift, sliding sheets,
  comic-panel cover cuts.
- Five original UI sounds (web-tick, thwip, zip, spider-sense, buzz), per-profile switch.

**System**
- Works as the Home app; Back is ignored while it's Home; one-command rollback to Google TV.
- NextDNS Home base: reads Private DNS, shows honest *not linked* state; API key only via ADB,
  stored encrypted with Android Keystore.
- Release signing with an off-repo key and a v3 rotation lineage from the debug key.

**Numbers (release, on the TV)** — 30–36 MB plain / ~54 MB with artwork (Google TV home: 78–107
MB), cold start ~330 ms, D-pad frames 15 ms median.

## Pre-release builds (2026-11-02 → 03)

Phases 0–6, owner fixes, polish pass and art pass — see
[`docs/IMPLEMENTATION_PROGRESS.md`](docs/IMPLEMENTATION_PROGRESS.md).
