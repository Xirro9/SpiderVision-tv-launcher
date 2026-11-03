# Design fidelity

## Locked sources

| File | SHA-256 |
| --- | --- |
| `references/APPROVED_ORRERY_COMIC_EDITION.png` | `9bc3927abc7ad80d6a609a3100a398cd206db588bdf6302eb7e710f917282a48` |
| `references/magicpath-original/src/components/generated/OrreryComicEdition.tsx` | `926530a701f5bec66c5190230dd9f9d4acb12e2925f14059f16d57e809d20538` |
| `magicpath-project.zip` (as received) | `3d136a25c0978d2ff10c8604716fbc85bf455fca9beccff1391ae29bbe58176c` |

Only the Orrery Comic Edition is in scope. Other Magic Path designs (the Pinterest-board
Orrery, the first TV launcher) must not be mixed in.

## Canvas

The TV renders apps at 1920×1080, density 320 → 960×540 dp. The approved screenshot is
966×537 px, so 1 screenshot px ≈ 1 dp. Layout constants are measured from the screenshot
in dp; the React `clamp()`/`vw` values are used for proportions, not copied.

## Tokens (from the React source)

| Token | Value | Use |
| --- | --- | --- |
| bg | `#111216` | background |
| surface | `#1A1C22` | panels |
| suit | `#0B0C0F` | custom app tiles |
| ink | `#08090B` | cover panel, caption text |
| navy | `#16203A` | badges |
| text | `#F2F3F5` | primary text |
| silver | `#B8BDC8` | secondary text |
| red | `#E0353F` | accent, progress, starburst |
| blue | `#4A7BE0` | masthead strip, chart |
| cream | `#F1E7CF` | panel borders, captions |
| focus | `#FFD23F` | D-pad focus ring (only use of yellow) |

Fonts: Bangers (display) and Manrope (UI), both SIL OFL 1.1, bundled as resources.

## Deliberate deviations (to be kept up to date)

| Area | Prototype | Native | Why |
| --- | --- | --- | --- |
| Masthead / app name | "Orrery" | "SpiderVision" | owner's request (2026-11-02) |
| Favorites panels | fictional titles ("Late Night Jazz"), LIVE! burst | app name + "App shortcut" + category | no real content data |
| Cover content | fictional "The Quiet Meridian", 47 min left, progress | user-configured app shortcut, no progress bar | no real watch history is available |
| Network tile | 18,402 queries etc. | "NextDNS stats not connected" | sample data must not look live |
| Inputs tile | HDMI device names | opens system input chooser or hidden | no fake device map |
| App count | "18 installed" | real launchable count | dynamic catalog |

## Screenshot comparisons

To be added per milestone: `docs/screenshots/<date>-<phase>.png` beside the reference.
