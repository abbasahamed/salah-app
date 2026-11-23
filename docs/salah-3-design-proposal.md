# Salah 3.0 · Sacred Minimalism

Stage 1 design proposal · 21 November 2026 · Approval required before implementation

**Recommendation: C. Midnight & Champagne.** A fine, open arch frames the next prayer. The blue-black canvas, recessed dial and restrained champagne accent establish a distinct identity in dark mode. Warm porcelain and bronze carry the same structure into light mode. Preserve the existing Light, Dark and System preferences and their defaults.

[Obsidian & Emerald](/Users/abbas/Desktop/Salah/docs/salah-3-concepts/a-today.png) · [Pearl & Sage](/Users/abbas/Desktop/Salah/docs/salah-3-concepts/b-today.png) · [Midnight & Champagne](/Users/abbas/Desktop/Salah/docs/salah-3-concepts/c-today.png) · [Midnight & Champagne Settings](/Users/abbas/Desktop/Salah/docs/salah-3-concepts/c-settings.png)

The four PNGs are rendered design concepts. Their illustrative schedule depicts the Kattankulathur development fixture after Isha. They demonstrate composition and theme relationships. Runtime layouts and all displayed values will be verified against the Compose app during implementation.

## Visual audit

Reviewed all 21 saved Salah captures in `screenshots/emulator` and `screenshots/ui-redesign`. The older images provided context for the 2.5 changes. Current light and dark Today, Settings, selectors, narrow and 200% layouts, offline source and release unavailability supplied the design evidence. Reviewed `TodayScreen.kt`, `Screens.kt`, `SalahApp.kt`, `SalahTheme.kt`, `Components.kt`, `QiblaIcon.kt`, `CalculationSettings.kt`, the Today state flow, `SalahAppTest.kt` and the previous design specification. The tests cover rendering, navigation and theme persistence. This audit evaluates the visible interface separately.

| Element | Before | Proposed after | Why |
|---|---|---|---|
| Typography | The system sans is legible. The 28 sp wordmark, prayer name and start time have similar visual weight, and the right-aligned time is distant from the countdown. | Manrope wordmark and central tabular time. Inter for labels and controls. A smaller prayer name directly anchors the numeral. | The name, time and remaining interval read as a single instrument. |
| Spacing | The compact top stack fits six rows. On the tall captures, a large blank field separates the methodology line from the grounded nav. | Give the dial the larger share of the first viewport, keep consistent 20 dp rails, and place method/source next to the schedule heading. | The added height serves the main task and gives the sections a more deliberate cadence. |
| Surface depth | The flat accent card and the white or charcoal schedule card are differentiated mainly by green tint. | Canvas, recessed dial bed, raised schedule panel and dock each get a semantic tone and a specific edge treatment. | Subtle depth conveys hierarchy without a shadow under every group. |
| Geometry | The shared 20 dp radius carries Today cards and Settings groups. | 24 dp hero, 18 dp groups, 22 dp dock and 10–16 dp controls. Use the open arch as the signature inside the hero. | Each shape corresponds to the job and scale of the component. |
| Color | Dark backgrounds, cards and controls share a green cast. Selected controls can dominate their section. | Reserve champagne or bronze for the active arc, next state, focus and selected tab. Use neutral blue-black and porcelain surfaces. | Accent color points to meaning. |
| Schedule hierarchy | Six rows are aligned and readable. Sunrise alone carries a small arc, and the provider line reads as a detached footnote. | Shared glyph grid, a parallel state column that can wrap, tabular trailing times and a compact provenance line. | The schedule looks intentional and retains a clear distinction for Sunrise. |
| Navigation | A full-width attached strip with a divider and small icon pills provides reliable navigation. The palette change is its main visual transition. | An inset solid dock with one shared selection plate moving between four stable tabs and with measured scroll clearance. | The full component feels integrated with the screen while preserving routes and touch targets. |
| Iconography | Material outline icons sit beside a custom compass and the hand-drawn Sunrise mark. Their optical weights differ. | One original rounded-stroke Android vector family for shell, calendar, compass, settings and solar phases. | Consistent keylines and stroke rhythm make the shell and schedule feel authored together. |
| Motion | The prayer name crossfades and tab colors tween over 150 ms. The line progress changes with each tick. | One coordinated dock transition, a coherent boundary swap, and direct draw updates to the arc and countdown. | Motion confirms state changes without turning elapsed time into a recurring animation. |
| Settings and originality | Segmented appearance and grouped selectors work. The screen still resembles standard cards, and large-font adjustment rows reflow with inconsistent label alignment. | External group headings, a shared field/value/chevron grid, one stepper alignment policy per width, and a provider detail group. | Settings inherits the same visual language and predictable reading order as Today. |

[Current light Today](/Users/abbas/Desktop/Salah/screenshots/ui-redesign/today-light.png) · [Current dark Today](/Users/abbas/Desktop/Salah/screenshots/ui-redesign/today-dark.png) · [Current dark Settings](/Users/abbas/Desktop/Salah/screenshots/ui-redesign/settings-dark.png) · [Current large-font Settings](/Users/abbas/Desktop/Salah/screenshots/ui-redesign/settings-narrow-200-font.png) · [Current release unavailable state](/Users/abbas/Desktop/Salah/screenshots/ui-redesign/today-unavailable-release.png)

## Sprint benchmark

Sprint was accessible. Reviewed the implemented Today screen, `SprintDial`, surface and typography tokens, root navigation wiring, the design specification and six saved device captures. The comparison used existing captures alongside source inspection.

| Sprint evidence | What Salah should adopt |
|---|---|
| The activity number dominates the circular aperture. Supporting units and metrics keep a clear scale relationship. | Give the start time one focal position. The readout should stand on its own when the arc is suppressed. |
| Its dial separates Canvas drawing from Compose text. `TextMeasurer` checks the aperture, and long metrics and large fonts move outward. | Use cached arc geometry and measured text bounds. Reflow rather than shrinking the user’s font scale. |
| Porcelain and dark neutral surfaces carry distinct elevation roles. High-saturation orange occupies a limited part of the screen. | Use surface tone and edges before shadow. Preserve Salah’s quieter navy, porcelain and champagne balance. |
| Rounded panels, optical icon keylines and typographic weights make its dashboard and profile feel related. | Build one shape and glyph system across Today, Settings and navigation. Salah’s thinner arch and lighter type create its own rhythm. |
| The floating capsule sits above system insets and content reserves clearance. Its lens uses interruption-aware motion. | Adopt measured clearance and a press-responsive selection plate. Salah’s dock is solid, shorter-cornered and tap-driven. |
| Fonts, ring geometry, states and motion are specified independently. The dial exposes semantic descriptions and handles a missing metric. | Separate the interval visualization from the time readout and provide an equivalent TalkBack statement. Keep progress absent when the interval is unknown. |

[Sprint design system](/Users/abbas/Desktop/Sprint/DESIGN.md) · [Sprint Canvas dial](/Users/abbas/Desktop/Sprint/app/src/main/java/com/sprint/health/ui/components/SprintDial.kt) · [Sprint navigation wiring](/Users/abbas/Desktop/Sprint/app/src/main/java/com/sprint/health/MainActivity.kt:1450) · [Light Home](/Users/abbas/Desktop/Sprint/img/light_01_home_top.png) · [Dark Home](/Users/abbas/Desktop/Sprint/img/dark_01_home_top.png)

## Three visual directions

| | A. Obsidian & Emerald | B. Pearl & Sage | C. Midnight & Champagne |
|---|---|---|---|
| Identity | Dark luxury with a crisp instrument face. | Warm daylight and soft botanical tones. | Architectural arch, blue-black depth and a quiet metallic highlight. |
| Light and dark | Obsidian and layered midnight in dark. Porcelain green and deep evergreen in light. | Warm pearl and white in light. Eucalyptus-charcoal in dark. | Navy-black and midnight in dark. Limestone, white and bronze in light. |
| Typography | Inter throughout. 44 sp tabular time, 22 sp prayer name. | Manrope headings and figures, Inter supporting text. 39 sp start time with a separate 28 sp remaining readout. | Manrope 50 sp time, 20 sp prayer name and Inter for supporting text and controls. |
| Depth and geometry | 26 dp edged inset hero, 18 dp raised schedule, 30 dp pill dock. Narrow emerald edge highlight appears only on the active ring. | 24 dp soft-tone hero, ruled schedule on canvas, 28 dp low-profile dock. Mostly tonal separation. | 24 dp recessed hero with a low-contrast perimeter, 18 dp schedule, 22 dp solid dock with an inset active plate. |
| Prayer visualization | Single full ring, 106 dp radius in the study, 8 dp stroke. Clockwise progress starts at the top. Central stack with date below the ring. | 180° horizon sweep with a 6 dp stroke. Name and time lead across the top, and remaining time sits inside the low arc. | 228° open arch, 5 dp stroke, thirteen calibrated tick positions and one endpoint. Start time is centered in the aperture and date closes the card. |
| Navigation | Floating capsule, icon highlight grows in place, labels stay stable. | Floating soft dock, one shallow selected icon lozenge. | Floating solid dock, shared plate moves behind icon and label. |
| Motion | Crisp 120–160 ms state changes; no replay when returning to Today. | 140–180 ms tonal shifts, with a quiet horizon that tracks the same timer state. | 160–190 ms plate travel, 120–160 ms readout boundary transition, linear cue for elapsed progress. |
| Strength and tradeoff | Strong contrast and immediate metric recognition. Full ring may read as a fitness goal and feels closest to Sprint. | Most approachable in light mode and lowest visual complexity. Soft green keeps some of the existing visual associations. | The open profile is easy to recognize and carries both themes well. Bronze requires a dedicated light accent and the arch needs text measurement at narrow sizes. |

C gives the most distinct silhouette. Its open base signals a span between prayers, and the more neutral surfaces keep the accent concentrated on the passage of time. Use B as the runner-up if a softer, light-led identity is the priority.

## Color tokens

The following tokens define the six concept palettes. All hex values are opaque sRGB. Map them to semantic slots in the existing Compose design-system module. Draw the hero edge and row separators with the divider token. Enabled content always uses ink, muted or accent according to its role. Status text labels accompany any status color.

| Role | A light | A dark | B light | B dark | C light | C dark |
|---|---|---|---|---|---|---|
| Canvas | #F2F7F4 | #0B1115 | #F5F5F0 | #151C18 | #F5F3EE | #0B1118 |
| Panel | #FFFFFF | #142126 | #FFFFFF | #202A24 | #FFFFFC | #142029 |
| Dial bed | #E5EFEA | #101D22 | #E8EFE8 | #222F26 | #F0EEE5 | #111C24 |
| Navigation dock | #F9FCFA | #19272B | #FCFDF9 | #26342B | #FCFBF7 | #18252E |
| Ink | #172A25 | #F2F2EA | #242D2B | #F1F3E9 | #232C33 | #F2F2EA |
| Muted ink | #456056 | #AABCB7 | #4E6056 | #B2C2B2 | #505D62 | #ADBBBE |
| Accent / active arc | #176D52 | #58C8A2 | #28664F | #A7C9A7 | #765520 | #D6BD89 |
| Ink on accent | #FFFFFF | #0C231B | #FFFFFF | #182F23 | #FFFFFF | #251D12 |
| Selected wash | #D9EEE2 | #1C3E34 | #DCEADF | #364B3D | #EEE2C8 | #312C25 |
| Hairline | #C8DAD1 | #2C4146 | #D0DDD2 | #35473B | #DDD8CB | #2B3D46 |
| Neutral track | #C2D7CD | #294148 | #C2D3C5 | #3C5041 | #CCC4B0 | #324651 |
| Focus | #176D52 | #75E2BA | #28664F | #C5E4BF | #765520 | #F0D9A6 |
| Error text | #9D342E | #FFB4A9 | #9D342E | #FFB4A9 | #9D342E | #FFB4A9 |

For C, use ink on panel as the label in inactive and selected controls, with accent glyphs on selected wash. Use a 2 dp focus outline when keyboard focus is visible. The track and quiet tick marks are redundant with the percentage semantics and readout. Mark consequential errors with text and one accent slot for retry. Apply the error token on panel backgrounds. Keep native pressed indication visible and retest contrast for the pressed and focused combinations on device.

Computed contrast for the nominal colors is listed as light / dark. These checks use the WCAG relative-luminance formula and cover the proposed pairings. Fine marks and motion still require visual inspection.

| Pair | A | B | C |
|---|---:|---:|---:|
| Ink on canvas | 13.90 / 16.89 | 12.93 / 15.47 | 12.80 / 16.85 |
| Muted ink on panel | 6.86 / 8.30 | 6.71 / 7.94 | 6.80 / 8.38 |
| Accent on dial bed | 5.34 / 8.35 | 5.78 / 7.69 | 5.85 / 9.46 |
| Ink on accent | 6.28 / 8.02 | 6.77 / 7.86 | 6.79 / 9.10 |
| Active arc against track | 4.15 / 5.25 | 4.32 / 4.78 | 3.91 / 5.40 |
| Accent on selected wash | 5.16 / 5.70 | 5.44 / 5.18 | 5.29 / 7.58 |

## Typography and iconography

**Manrope + Inter** for C. Manrope gives headings and figures a geometric profile, and Inter keeps dense labels clear at small sizes. Inter-only is the alternative for a smaller font bundle. Android system sans is the fallback for unsupported glyphs or unavailable bundled fonts. Validate localized numerals and fallback font metrics in Compose. Both checked Google Fonts binaries ship under SIL OFL 1.1 and expose `tnum` in their OpenType feature tables. The concept PNGs use those fonts. A later implementation should pin the selected versions and include the associated OFL notices. [Inter OFL](https://github.com/google/fonts/blob/main/ofl/inter/OFL.txt) · [Manrope OFL](https://github.com/google/fonts/blob/main/ofl/manrope/OFL.txt)

| Role | Family | Size / line height | Weight | Notes |
|---|---|---|---:|---|
| Wordmark and screen title | Manrope | 32 / 38 sp | 750 | −0.7 sp tracking |
| Dial start time | Manrope | 50 / 56 sp | 680 | Tabular digits, −0.9 sp tracking. In 12-hour mode, place the period in a separate readable span and allow it to wrap. |
| Dial prayer name | Manrope | 20 / 26 sp | 730 | Sentence case |
| Section heading | Manrope | 18 / 24 sp | 720 | Semantically marked as a heading |
| Countdown | Inter | 14 / 20 sp | 650 | Tabular digits with localized spoken duration |
| Prayer row and settings value | Inter | 15 / 21 sp | 550 or 650 | Reserve the heavier weight for the relevant state and time column. |
| Body and explanatory copy | Inter | 14 / 21 sp | 450 | Wrap in place |
| Metadata and nav label | Inter | 12 / 17 sp | 550–680 | No reliance on a tiny development/source label for essential context |

Build original rounded-cap glyphs on a 24 dp optical grid, with about 1.75 dp stroke and a shared centerline. Schedule glyphs render at 18–20 dp, navigation at 20–22 dp. Fajr is a dawn horizon, Sunrise adds rays, Dhuhr a high sun, Asr a lower sun, Maghrib a descending horizon, Isha a crescent. Pair every glyph with its name. Today uses a threshold-shaped home glyph, Calendar a two-tab calendar, Qibla a compass, and Settings a sliders glyph. Use Compose `ImageVector` or Android vector resources and keep illustrative geometry out of the prayer model. The concept marks are original drawings. Review any future third-party icon license before importing an asset.

## Salah Dial · recommended specification

- At reference width 390 dp, target a 350 dp card width and approximately 290 dp natural height. Place a 210 dp diameter arch inside a 224–236 dp drawing region. Use a 5 dp curved track from 156° through 228° in Compose coordinates, with 13 evenly spaced guides and longer start, midpoint and end marks. Render progress clockwise for this clock metaphor in every locale and keep spoken reading order locale-aware.
- The center reads “Next prayer”, the obligatory prayer name, the formatted start time and remaining time. Position the label on the card’s top axis, and the readout inside the arch. Put the next date below the open base when the prayer falls on a later local date. Use muted guides and a single solid active endpoint. A 0% interval has an empty track and no lit endpoint.
- Progress represents elapsed time between the actual preceding obligatory prayer and the resolved next prayer. With two validated, ordered endpoints, compute `(now − precedingInstant) / (nextInstant − precedingInstant)` or the equivalent using the existing remaining duration, clamp to 0–1, and format text independently. Use instants for arithmetic across midnight and daylight-saving changes, and use the schedule location's zone for the date and time labels.
- Match occurrences by instant, prayer and schedule date. Today shows a separate tomorrow date for next-day Fajr. Never promote today’s earlier Fajr row to “Next” in that state. A “Previous” label may mark today’s Isha only when it is the actual interval start. Sunrise never becomes a dial endpoint.
- The existing Today presentation can derive a preceding candidate from the visible schedule on an ordinary day. Before Fajr, the ViewModel already considers the preceding day's schedule during resolution, but it does not expose that endpoint to Today. Start with available validated UI facts. If the correct endpoint is unavailable, suppress progress and retain the next-prayer readout. If Stage 2 needs a pre-Fajr arc and resolved endpoints can be forwarded from existing data, add only a presentation-state field. Keep the resolver, provider calls, cache behavior and calculation outputs intact.
- Loading retains stable hero dimensions with a concise status. For a known next prayer with unknown interval, keep the name, time and countdown and use a quiet readout without a progress value. For unavailable data, display the actual status and existing retry action where supported. Keep the width negotiable on small devices and move the readable stack above a shorter sweep when measured text exceeds its aperture.
- Treat the dial as read-only. Merge a localized semantic statement such as “Next prayer, Fajr, tomorrow at 4:57 AM. 7 hours 45 minutes remaining. 23 percent of the interval elapsed.” Supply range semantics for known progress, and hide decorative guides from accessibility. Avoid a per-second live region. Hold a stable focus node and announce a genuine prayer change at most once if the user is on Today.

## Component inventory

| Component | Design and native behavior |
|---|---|
| Today header | Wordmark, local date and location with a quiet development-fixture label. Retain the existing location action. |
| `SalahDial` and readout | Canvas arch and Compose text. Cache fixed geometry by size, density and direction. Use a readout presentation object derived from the existing state. |
| Schedule section and prayer row | Six chronological entries in one surface. Use 16 dp insets, 1 dp inset separators, a shared glyph grid, a 40–44 dp nominal height for static rows and a trailing tabular time column. Expand height for wrapped text. |
| Schedule provenance | Discreet method and source at the section header or below it when width is tight. Use distinct text for AlAdhan online, saved AlAdhan and offline calculation. Omit the provider status while pending. Where the upcoming prayer and daily schedule differ in source, the schedule label still describes only the daily rows. |
| Floating dock and navigation rail | Four persistent destinations and one moving selection plate in compact width. Replace the dock with a matching rail in wide layouts. Preserve current route objects and state restoration. |
| Settings group and value row | External headings, grouped rows, full-width click surfaces, native anchored choices, current value and selected state. Let the full method name wrap. Move focus back to the opener when dismissing a menu. |
| Appearance selector | System, Light and Dark in an adaptive single-select control. Use the persisted mode for selection and show save failure through the existing error state. Stack vertically when the segments cannot meet sizing and wrapping requirements. |
| Adjustment stepper | Left-aligned label, accessible decrease and increase buttons around a tabular signed value. 48 dp minimum targets and rows. At narrow large-font sizes, stack all labels above controls consistently. Preserve the existing ±60 bounds. |
| Timing information | A quiet section below adjustments explains online AlAdhan, saved schedules, on-device Adhan, the full selected calculation method and coordinate use. Use the actual source when presenting information about a specific loaded schedule. |

## Navigation and motion

Use a solid 22 dp rounded container, nominal 64 dp height, 24 dp horizontal inset at 390 dp and a width cap near 420 dp. Set the base offset 8–12 dp above the Android navigation inset. Place label and icon in each full-width tab, with at least 48 × 48 dp interactive bounds per tab. Each selected tab has a stable label, a filled selection plate and a semantic selected state. Keep the outline icon geometry consistent across the states. Use logical start/end alignment in RTL.

Use one measured moving plate driven by the actual selection, with interruptible motion. Activation updates the destination and semantics immediately. The content does not wait for the plate. Reserve clearance from the dock’s measured height plus the bottom inset and an 8–12 dp gap so a final row or action can scroll fully into view. Consume insets once, and use native tab and focus semantics. Retain Today, Calendar, Qibla and Settings, root back behavior and Location routing. Keep Calendar and Qibla at their current placeholder scope.

| Trigger | Motion | Reduced motion |
|---|---|---|
| Tab tap | 160–190 ms decelerating plate travel and synchronized icon tint. Keep text and destination layout stable. | Immediate selection and destination update. |
| Prayer boundary | 120–160 ms readout fade as a group. Atomically update name, time, date and countdown. Start the new interval at its actual fraction. | Immediate readout and progress update. |
| Countdown tick | Replace digits on the existing lifecycle tick and draw the current arc fraction. Use direct or short linear updates local to the Canvas. | Direct updates. |
| Menu and settings press | Android press indication and anchored menu. About 150–180 ms on open, shorter on dismissal. | Native immediate or reduced transitions. |
| Theme switch | Apply theme tokens together and preserve scroll, focus and settings value. | Immediate. |

Honor the Android system animation scale. Avoid repeated dial entrances, idle pulse, per-row staggering and steady background animation. Use native Compose animation primitives and keep date and schedule recomposition separate from the per-second readout where practical.

## Responsive and accessibility requirements

- Use a 4 dp grid with 20 dp reference gutters, 16 dp compact gutters, 16–24 dp section gaps, and 12–16 dp insets. Use content-based heights. Ensure all six prayer times fit above the dock on a representative 390 × 844 dp portrait layout at standard font with gesture navigation. On shorter phones, 3-button navigation, large fonts and split windows, allow scrolling with full bottom clearance.
- Test 320 dp and 390 dp widths, 600 dp or wider, landscape and split screen. At wide widths, use a rail and two columns only if the dial and schedule both meet usable minimum widths. Otherwise center a single column. Preserve a logical reading and focus order.
- Test font scales 1.0, 1.3 and 2.0 including long method names, localized date strings and AM/PM. Use measured layout to move text out of the aperture. Keep critical text fully readable, maintain 48 dp touch targets and add TalkBack and keyboard verification. [Android Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) · [Android scalability](https://developer.android.com/develop/ui/compose/accessibility/scalable-content)
- Target 4.5:1 for normal text and 3:1 for large text, functional marks and focus indicators. Use labels and semantics for selected, next, loading and unavailable states. Verify source and row states with TalkBack, dark mode, larger text, high contrast and RTL.
- Account for status, gesture and 3-button navigation bars, display cutouts and IME where relevant. Preview and test both light and dark surfaces while scrolling beneath the dock. Confirm screen reader focus can reach the last prayer and Settings information.

## Implementation sequence after approval

1. Confirm the selected direction, review current screenshots and add the chosen palette, typography, spacing, motion and icon tokens in the existing Compose design system.
2. Build the Today header, dial and prayer schedule with previews for known interval, tomorrow Fajr, unknown preceding endpoint, loading and unavailable states. Compare with the concept at full size on device.
3. Add the dock and rail using the existing navigation graph. Measure insets, clearance and focus before polishing animation.
4. Refine Settings around existing preference flows and persisted setters. Verify selection and adjustment interactions, including recovery from a save error.
5. Run parity and visual checks on the emulator in both themes and accessibility configurations. Change presentation and test selectors only where necessary. Keep API, Room, offline engine, source selection, calculation methods, Asr rules, timezone/date behavior, offsets and preference persistence intact. Leave Milestone 3 location scope gated separately.

## Visual acceptance criteria

- One clear focal point on Today across both themes. The open arch, tabular start time and readable prayer metadata define the first viewport. The schedule has six aligned entries and uses one unambiguous state cue per relevant row.
- First-view six-row fit at the reference size, and complete reachability at 320 dp and 200% font with gesture and 3-button navigation. No clipped time, label, source line, stepper or focus target.
- Stable readout and arc on repeated visits. Verify same-day, exactly at prayer start, after Isha, after midnight, next-day Fajr, unknown prior endpoint and timezone transitions against existing tests and runtime fixtures. Numeric updates follow the real state. Verify an interrupted tab change, rapid taps and reduced-motion mode.
- Both themes pass actual screenshot review for type scale, surface contrast, labels and selected states, with TalkBack and keyboard checks. Capture Today, Settings, chooser open, offline, cache, loading/error, and the genuine release no-location state. Mark Compose previews separately from on-device captures.
- Pass existing calculation, source, countdown, preference and navigation regression tests. Recheck all four destinations, calculation method, Asr, 12/24-hour display and offsets. Keep source labels faithful to the schedule they describe.

The current session lists global `frontend-design` and a global `emil-design-eng` entry. I used the project-pinned Emil file and the project-local Salah skill directly because this session did not list those copies. Frontend-design informed hierarchy and visual focus, Emil informed short interruption-safe transitions, and Salah defined native rendering, accessible review and protection of prayer-time behavior.

**Decision for approval:** select A, B or C before Stage 2. The Stage 1 deliverables are confined to this proposal and its concept previews.
