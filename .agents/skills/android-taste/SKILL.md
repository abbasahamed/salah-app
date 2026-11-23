---
name: android-taste
description: Gives coding agents taste when building or redesigning Android apps, so the result looks art-directed instead of like the Android Studio template. Classifies the app, deals one design direction (Material 3 light and dark palette, type pairing, shape scale, home-screen layout, motion, signature moments), builds tokens first in Kotlin/Compose or XML/Views, then verifies on a real device or emulator with contrast checks, a taste linter, screenshots in light and dark at larger font scales, and a finish gate. Covers Kotlin, Kotlin DSL (Gradle), Java (JVM 21 toolchain, Java 8 bytecode, core library desugaring), XML and Bash. Use whenever the user asks to design, style, theme, polish, redesign, "make it look good/premium/modern/less generic" or "add a UI" for an Android app, screen, theme.xml, Theme.kt or Compose UI, or asks for a Material 3 look, dark mode, a design system or a better-looking Android project, even if they never say "taste".
---

# android-taste

Most generated Android UI is the Android Studio template with more screens: the purple and pink baseline palette, `Greeting("Android")`, a green robot icon, system font everywhere, `dynamicColor = true` quietly replacing whatever palette was chosen, spinners for every loading state, and no dark theme, tablet layout or motion. It compiles and demos fine. It looks like nobody decided anything.

This skill makes the decisions first, writes them down as tokens, builds every screen from those tokens, and then checks the result by looking at it on a device. The structure follows Leonxlnx/taste-skill (MIT, https://github.com/Leonxlnx/taste-skill), which does this for websites: deal a direction, build from one system, verify in a real browser, gate the finish. This version is written for Android.

## Languages and where to read

| Task touches | Read |
|---|---|
| Direction, hierarchy, what "good" means on a phone | `references/direction.md` (always) |
| Material 3 roles, tonal surfaces, dynamic color, type scale, shape, dark theme | `references/material3-theming.md` |
| Kotlin / Jetpack Compose UI | `references/compose.md` |
| XML layouts, themes, styles, manifest-level UI settings, Views | `references/views-xml.md` |
| Kotlin DSL (Gradle) dependencies, fonts, JVM 21 / Java 8 bytecode / desugaring, Java UI code | `references/build-and-java.md` |
| Microcopy, sample data, icons, imagery, number and date formatting | `references/content-and-copy.md` |
| The finish gate and the final rubric | `references/finish-gate.md` |
| Bash scripts in `scripts/` | usage block at the top of each script (`--help`) |

Read only what the task touches, but always `direction.md` and `finish-gate.md`.

## Ground rules

1. **Decide before you draw.** No screen is written until a direction is dealt and recorded (Step 2). A screen built without tokens is a screen to throw away.
2. **One system.** Colors come from theme roles, type from the type scale, spacing from the 4dp scale, shapes from the shape scale. No literal `Color(0xFF...)`, `#hex`, `17.sp` or `13.dp` in screens.
3. **Real content.** No lorem ipsum, "Item 1", "Test", or `Hello Android`. Write plausible sample data and real microcopy, in the app's voice.
4. **The platform is the baseline, not the ceiling.** Edge-to-edge, dark theme, window size classes, 48dp targets, 200% font scale and predictive back are not features, they are table stakes. Do them before ornament.
5. **Project conventions win.** If the project already has a brand, a design system, a Theme.kt or a shared XML style, extend it. Deal a direction only for the parts that are missing, and say what you kept.
6. **Behavior stays.** Restyling must not change what the app does. Flag anything that does (for example, changing the theme parent changes default component shapes; setting `dynamicColor = false` changes colors for Android 12+ users).
7. **Verify by looking.** Contrast numbers and lint are necessary, not sufficient. Take screenshots and read them.
8. **Never claim what you did not run.** If there is no device, no Gradle or no adb in this environment, say so and give the exact commands for the user.
9. **No version guessing.** Compose, Material Components and AGP APIs change. Read the project's versions and confirm any API you are not certain of. Do not upgrade dependencies to get a prettier API without asking.

## Workflow

### Step 0: Orient

Read, without editing: `settings.gradle(.kts)`, module `build.gradle(.kts)`, `gradle/libs.versions.toml`, AGP / Kotlin / Compose BOM / Material Components versions, minSdk / targetSdk / compileSdk, whether UI is Compose, Views or mixed, existing theme files (`Theme.kt`, `Color.kt`, `Type.kt`, `themes.xml`, `colors.xml`), `res/` qualifiers present, launcher icon, any brand assets or reference images the user supplied. If a Bash tool exists, run:

```bash
bash <skill-dir>/scripts/taste_lint.sh <project-root>
```

to see how template-looking the project is before changing anything. If the user shared screenshots or a reference app, study them first: what to keep, what to drop.

### Step 1: Classify

Pick one kind. It decides palettes, type, shape, layout, motion and moments.

| Kind | Typical apps |
|---|---|
| `finance` | budgeting, wallets, trading, invoices, expense trackers |
| `productivity` | tasks, notes, calendars, planners, habit trackers |
| `health` | fitness, sleep, meditation, medication, wellbeing |
| `media` | music, video, podcasts, reading, photo galleries |
| `social` | feeds, chat, communities, dating, events |
| `commerce` | shops, marketplaces, food delivery, bookings |
| `education` | learning, flashcards, language, quizzes, student tools |
| `travel` | maps, trips, transit, itineraries, local guides |
| `utility` | calculators, scanners, file tools, device tools, dashboards |
| `game` | casual games, companions, trackers with a game feel |

Write a three-line brief: who uses it, the one job of the home screen, and three words for the feel. Ask the user only when the kind or the job is genuinely missing; otherwise state your assumption and go.

### Step 2: Deal the direction

```bash
bash <skill-dir>/scripts/deal.sh --kind <kind> --out .android-taste/deal
```

It prints the direction and writes `compose/DealtTheme.kt` (light and dark `ColorScheme`s plus `Shapes`), `res/values/dealt_colors.xml`, `res/values-night/dealt_colors.xml`, `res/values/dealt_themes.xml` (a Material 3 theme with the palette and shape scale wired in) and `DEAL.md`. Same `--seed` reproduces a deal. Use `--palette NAME` to pick a specific palette, `--list` to see the library.

Rules for the deal:
- Accept the deal. Rerolling until something pleasant appears is how you end up generic again. Reroll only if it clashes with the brand or an existing asset, and say why.
- If the client has a brand color, keep it as the seed: generate the role set with Material Theme Builder (or the project's existing tooling), keep the dealt type, shape, layout and motion, and validate with `scripts/contrast.sh --project`.
- If no Bash tool is available, read `library/*.tsv` and deal by hand: filter rows by kind, choose one per file, and say so.
- Record the direction in the project (for example `docs/design-direction.md`, or the top of `Theme.kt`) so later sessions keep it.

### Step 3: Tokens first

Merge the dealt files into the project, then add the rest of the system before any screen:

- Color and shape: dealt schemes in `Theme.kt` (Compose) or `themes.xml` + `colors.xml` (Views). Decide `dynamicColor` on purpose (see `references/material3-theming.md`).
- Type: the dealt pairing mapped onto the Material type scale (`references/material3-theming.md`, `references/compose.md` or `references/views-xml.md`). Tabular figures wherever numbers align.
- Spacing: a named 4dp scale; screens use names, not numbers.
- Motion: durations and easings for the dealt personality, one place.
- Foundations: edge-to-edge, dark theme, adaptive layout skeleton, splash screen, themed adaptive icon.

### Step 4: Build screens, one at a time

For each screen: one job, one primary action, a clear hierarchy (large number or title first, supporting text second, actions third). Start from the dealt home-screen archetype and its compact and expanded behavior. Design the empty, loading, error and offline states as part of the screen, not afterwards. Use Material 3 components for their purpose (see `references/compose.md` and `references/views-xml.md`). Previews with realistic data for every stateless composable.

### Step 5: Moments

Every app gets a hero entrance on the home screen and the one or two dealt moments (shown in the deal output, listed in `library/moments.tsv`). Implement them quietly: they punctuate, they do not perform. Each must degrade with reduced motion and never delay input.

### Step 6: Verify

1. Build and install if a toolchain exists (`./gradlew :app:installDebug`), then:
   ```bash
   bash <skill-dir>/scripts/shoot.sh --package <applicationId> --link <deeplink> ...
   ```
   (light and dark, font scale 1.0 and 1.5 by default; restores device settings afterwards).
2. Open the screenshots and read them as a critic would (checklist in `references/finish-gate.md`): contrast in both modes, clipping at large font, insets under the system bars, empty and error states, tablet or foldable behavior if you can run it.
3. Run the gate:
   ```bash
   bash <skill-dir>/scripts/gate.sh --project . --shots taste-shots --need-shots 4 --lint
   ```
   FAIL on T0 or T1 findings, bad contrast, missing screenshots or failing Gradle lint. Fix and rerun. **Three rounds at most**: after that, stop, hand over, and list what is open. Waive a finding on purpose in `.android-taste/waivers.txt` with a reason, never silently.

### Step 7: Report

Short: the dealt direction (palette, type, shape, layout, motion, moments), what was kept from the project, files changed, `BEHAVIOR CHANGE` items, gate result (round number, waivers), what was not run (and the exact command for the user).

## The tells this skill removes

- The template palette, `Greeting`, the green launcher icon, `Theme.MyApplication`.
- Material purple on everything; gradients as decoration; shadows on every card instead of tonal surfaces.
- System font only, one weight, no hierarchy; numbers in proportional figures that jitter as they change.
- `dynamicColor = true` by default, so the chosen palette disappears on most phones.
- Mixed icon families, emoji as icons, a bare spinner for every loading state.
- No dark theme, no edge-to-edge, no tablet layout, no motion, no splash, no themed icon.
- Odd spacing (13dp, 15dp), literal colors and sizes in screens, ALL CAPS and exclamation marks in copy.
- Screens that look fine at 1.0 font scale and break at 1.5.
