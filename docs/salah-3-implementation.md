# Salah 3.0 · Midnight & Champagne

Stage 2 implemented the approved visual direction in native Kotlin and Jetpack Compose. The existing navigation graph, repository calls, resolver and preference setters drive the interface. The only ViewModel change surfaces the most recent eligible obligatory prayer from the schedules already considered during resolution. The dial uses that instant, the resolved next instant and the existing remaining duration. It hides progress when the interval cannot be established.

## Before and after

| Before | After | Result |
|---|---|---|
| Rectangular countdown card with a horizontal track | Recessed 228° Canvas arch with 13 guides, a single active endpoint and a Manrope time readout | The upcoming prayer and its remaining interval form one focal point. |
| Sage-themed surfaces and a generic sans | Midnight and champagne in dark mode, porcelain and bronze in light, with Manrope and Inter | The dial, schedule and dock have distinct surface roles. |
| Six rows with a standalone Sunrise mark | Unified schedule with six original solar glyphs, trailing tabular times and explicit next or previous context | Both themes preserve a consistent reading order and distinguish Sunrise from obligatory prayers. |
| Full-width navigation strip | Inset opaque dock with one movable selection plate and measured insets | Navigation stays above Android system controls, with 48 dp targets and keyboard focus outlines. |
| Settings groups and plain adjustment symbols | Outer group headings, anchored selectors and aligned steppers with expanded hit targets | Controls keep their native persistence and reflow consistently at large text. |

[Before Today](/Users/abbas/Desktop/Salah/screenshots/ui-redesign/today-dark.png) · [Approved concept](/Users/abbas/Desktop/Salah/docs/salah-3-concepts/c-today.png) · [Implemented Today](/Users/abbas/Desktop/Salah/screenshots/salah-3/today-dark.png)

## Compose structure

- `SalahTheme` defines semantic color, shape and type roles in `:core:designsystem`. The Inter and Manrope variable fonts include their OFL 1.1 notices. Android 23–25 uses the platform sans fallback. [Inter license and upstream](https://github.com/google/fonts/blob/main/ofl/inter/OFL.txt) · [Manrope license and upstream](https://github.com/google/fonts/blob/main/ofl/manrope/OFL.txt)
- `SalahDial` renders an accessible readout and responsive Canvas arch. The visible time is measured before placing it inside the aperture. Large type moves the text above a smaller arch. The progress semantics expose the actual fraction and the readout speaks a localized date, time and duration. The timer updates directly and a short transition applies to boundary changes.
- `SalahIcons` provides the original rounded vector family for prayer phases and destinations. `TodayScreen` composes the header, schedule, and accurate method/source labels. Larger text puts the state label below the prayer name when space is tight.
- `SalahApp` retains its navigation destinations and renders the shared dock plate behind the tabs. The plate uses a 175 ms interruptible offset animation. `Scaffold` reserves the dock's measured height and the bottom safe area. Wide windows keep the rail.
- `Screens` and `CalculationSettings` retain theme, calculation, Asr, display format, offsets, and provider disclosures. The settings match persisted values. Stepper layout stacks consistently at large text sizes.

The font binaries are pinned by SHA-256 in this implementation. Inter is `29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031`. Manrope is `3ae11c49db0455a3cc33e37d380f20fdb8c7f8b41dc07625c177e3d87a9d6ae6`. Both were acquired during Stage 1 from Google Fonts and checked for the OpenType `tnum` feature.

## Visual review passes

1. Reviewed the first dark Today and Settings captures against the approved concept. Confirmed the arch geometry, restrained tiered surfaces, matching icons, aligned rows and dock clearance. Refined tomorrow context and gave loading a dedicated hero space.
2. Reviewed light mode, the 390 dp reference, 320 dp and 200% text. At 390 dp the entire schedule fits above the dock. At 200%, the readout moves above the arch. Isha's state label caused its time to wrap, so the state label was moved below the name in constrained layouts.
3. Reviewed both themes again on the final layout, including Settings, narrow and large-font screens. The six times remain aligned and reachable at 200% with scrolling. Verified native selection menus and inspected the actual unavailable release state.

The setup used the Anthropic frontend-design, project-pinned Emil and project-local Salah instructions, with the Sprint Canvas implementation and reference captures as a quality benchmark. Sprint's code was inspected read-only. Salah has no Git metadata, so the verified pre-edit source checkpoint is at [source.tar.gz](/Users/abbas/Desktop/Salah-stage2-backup.PMegNP/source.tar.gz).

## Verification

- Debug and unsigned release APKs built. The 26 JVM tests reran and passed. Both Compose UI tests passed with radios off and passed again on the final debug APK, including the dial description assertion and location/navigation and theme checks. Debug lint finished with 0 errors and 14 existing SDK/tool/dependency update warnings.
- On SprintPixel API 36, a live AlAdhan schedule reflected the one-minute Sunrise adjustment at 06:12 and displayed 06:11 after the adjustment was restored. The saved schedule loaded after a cold start with radios off. The uncached Hanafi setting produced 16:04 Asr offline and persisted over a cold restart. Standard, zero adjustments, 24-hour format, and System appearance were restored.
- The existing resolver displayed Fajr at 04:57 the next day. Successive semantics snapshots confirmed a ticking countdown. The final 12-hour visual pass kept the smaller AM/PM span in the arch. The release build showed the actual no-location and unavailable states.
- The debug app was left in the Android foreground on the running graphical emulator. The network, viewport and font scaling were reset to their initial values. The crash buffer showed no Salah crash. Full TalkBack and physical-device testing were outside this verification.
