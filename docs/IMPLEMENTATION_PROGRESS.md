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

Pending.

## Phases 2–7

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
