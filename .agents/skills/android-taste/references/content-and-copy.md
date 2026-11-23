# Content and copy: the words, numbers and pictures

Contents: 1 Copy, 2 Sample data, 3 Numbers, dates and units, 4 Icons and imagery, 5 App icon, 6 Localization hygiene

Most generated UI fails here before it fails anywhere else: placeholder words, fake round numbers, a different tone on every screen.

## 1. Copy

- Plain, specific, short. Say what happened and what to do next. Sentence case everywhere.
- Same voice on every screen. Decide it once (calm and exact, friendly and brief, dry and playful) and write the empty states, errors and buttons in it.
- Buttons are verbs that name the result: "Add account", "Save changes", "Start run". Not "Submit", "OK" (except in alerts that need it), or "Click here".
- Errors: what failed, why if known, what to do. "Couldn't load your transactions. Check your connection and try again." Not "Oops! Something went wrong!"
- Empty states: what belongs here and the one action that fills it. "No budgets yet. Create one to track a category."
- No exclamation marks except in rare celebration. No ALL CAPS. No emoji as decoration or as icons.
- Do not apologize for the app. Do not say "please" more than once per screen.
- Destructive actions name the thing and the consequence: "Delete 'Groceries'? Its 42 entries will be removed."
- Permission rationales say the benefit before the ask: "Allow notifications to get a reminder when a bill is due."
- Everything user-visible lives in `strings.xml` (or the Compose `stringResource`), with `<plurals>` for counts and positional arguments for sentences so translators can reorder.

## 2. Sample data

Previews, screenshots and seed data are part of the design. They should look like real use.

- Names, titles and amounts that vary in length and shape: a long title that wraps, a short one, a missing optional field.
- Non-round numbers: 1,284.50 not 1,000. Dates spread over the past week, not all "Today".
- Include the awkward cases on purpose: very long name, zero items, one item, 999+ badge, negative amount, RTL string, missing image.
- No "Item 1 / Item 2 / User / Test / Lorem ipsum / John Doe". Invent plausible, neutral names and content for the app's domain.
- Keep sample data in preview or debug source sets, never in production code paths.
- Avatars: initials on a tonal circle (derived from the theme), not a stock photo or a robot.

## 3. Numbers, dates and units

Locale-correct formatting is a taste signal and a correctness requirement.

- Currency: `NumberFormat.getCurrencyInstance(locale)` (or a `Currency` set explicitly if the app has its own). Store money as integer minor units or `BigDecimal`, not floating point. Digit grouping follows the locale; do not hand-insert commas.
- Dates and times: `DateTimeFormatter.ofLocalizedDate/Time(FormatStyle.X)` with the user's locale; relative dates ("Yesterday") for recent items, absolute for older. Respect the user's 12/24 hour setting.
- Below the API level that ships `java.time`, enable core library desugaring instead of `SimpleDateFormat`.
- Units: use `android.icu.text.MeasureFormat`/`NumberFormatter` (API 24+) or the project's helper rather than concatenating "km" to a number.
- Align numbers in lists with tabular figures and right alignment (start alignment in RTL follows layout direction).
- Rounding and abbreviations (1.2K, 3.4M) are consistent across screens and never hide meaning for financial or health values.

## 4. Icons and imagery

- One icon family and one style (Outlined or Rounded), 24dp, from Material Symbols or one coherent set. Filled variant only for the selected state.
- Vector drawables for icons and simple illustrations; tint with theme attributes or `LocalContentColor`.
- Illustrations: a small number of simple vector scenes in the palette's roles beat stock images. Use open-licensed sources only, and record the source and license in the repo.
- Photographs: only real or properly licensed images. Do not use someone else's app screenshots, brand logos you do not have the right to, or AI faces as "users". Credit sources where the license requires it.
- Images need a loading placeholder in `surfaceContainerHigh`, a fixed aspect ratio so the layout does not jump, and a failure state.
- Text over images needs a scrim that passes contrast; check it in screenshots.

## 5. App icon

- A single clear mark in the primary hue on a calm background; no gradients that do not survive tiny sizes, no text.
- Adaptive icon with background and foreground layers, plus a monochrome layer for themed icons (see `views-xml.md`). Keep the mark inside the central safe zone.
- Replace the template's launcher icon entirely. The template icon is the loudest "unfinished" signal an app has.
- Check the icon at 48dp on light, dark and a themed wallpaper, and in the Play listing size.

## 6. Localization hygiene

- No concatenated sentences, no strings built from fragments, no text inside images.
- Layouts tolerate 30-40% longer text (German, Finnish) and mirrored layout (RTL). Test with a long string and with `android:supportsRtl="true"`.
- Fonts cover the app's scripts; set a fallback for scripts the display font lacks.
- Use `Locale`-aware APIs for case, sorting and plurals (`String.format(locale, ...)`, `<plurals>`), never `toUpperCase()` without a locale.
