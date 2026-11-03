# Claude Code kickoff prompt

Build a **native, lightweight Android TV launcher** for my Philips-24NA-4K (Android TV 14, firmware UKN7.260716.014) using this handoff folder.

**Source of truth:** `reference/APPROVED_ORRERY_COMIC_EDITION.png` for visual fidelity, `magicpath-export/src/components/generated/OrreryComicEdition.tsx` for exact design details, and `IMPLEMENTATION_GUIDE.md` for scope, phases, acceptance criteria and safety rules. Do not mix in other Magic Path designs.

The Magic Path React code is a *prototype*, not a functioning launcher. Rebuild in Kotlin + Jetpack Compose for TV, **not a WebView**. Preserve the exact comic editorial layout, colors, hero, custom icon treatment, profiles and yellow focus indicators. Priority: low memory usage, fast D-pad navigation, clear readability, no animations, no continuous services.

Start with Phase 0 from the guide. Create a new GitHub repo `orrery-tv-launcher` (or a suitable available name), baseline this exact TV read-only using existing wireless ADB, and create `docs/IMPLEMENTATION_PROGRESS.md`, `docs/DESIGN_FIDELITY.md`, `docs/TEST_MATRIX.md` and `docs/ROLLBACK.md`. Then work through phases in safe, testable batches. Update progress and commit at each completed phase.

Implement *real* installed-app discovery and launch intents, native remote navigation, persisted favorites/settings, and robust fallbacks for missing apps. The demo's 18,402 DNS queries, fictional watch history, profile content, mock HDMI switching and toast-only app launch must not be passed off as working features. NextDNS is already configured at OS level; show honest "stats unavailable" until there is a secure API integration. Never put an account API key in the APK or repository.

Build and install test APKs to the Philips via ADB, preserving the factory Google TV launcher as default. **Never root, flash, disable system packages, override HOME, change DNS, or modify existing TV settings/packages without my explicit approval.** Test actual TV playback, remote, Wi-Fi, casting and HDMI after each meaningful milestone. If HOME override is not supported, keep a working standalone TV app; do not force it.

Deliver a source repository, signed private release APK when appropriate, performance results against the original launcher, screenshot comparisons, instructions to install/uninstall, and a clear limitations/rollback guide. Do not claim completion based only on tests on a PC or browser.

Before beginning implementation, summarize the actual source gaps, proposed tech stack, and Phase 0 baseline findings concisely. Then execute the plan within the safety gates. Avoid unnecessary permission requests and stop for user approval only on device-destructive or HOME-related changes.
