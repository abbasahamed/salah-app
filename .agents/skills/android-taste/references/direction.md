# Direction: what taste means on a phone

Contents: 1 What the deal gives you, 2 Hierarchy, 3 Density and rhythm, 4 One signature, 5 Color discipline, 6 Components in their roles, 7 States are screens, 8 Adaptive, 9 Squint and remove tests, 10 When there is already a brand

## 1. What the deal gives you

`scripts/deal.sh` picks from `library/` so that the decision is made once, by something other than your defaults:

| Dealt | What it fixes |
|---|---|
| Palette (light + dark Material 3 roles) | the template purple, or an arbitrary seed color with no tonal logic |
| Type pairing | system font with one weight and no voice |
| Shape scale | default 12dp rounded rectangles on everything |
| Home layout archetype (+ compact and expanded behavior) | every home screen being a centered column of cards |
| Motion personality | either no motion or the same fade everywhere |
| Two moments | an app with nothing memorable and no point of view |

The deal is a starting direction, not a template to copy. Every dealt item has a reason tied to the app's kind; keep the reason in mind when a screen needs a decision the deal did not cover.

## 2. Hierarchy

Each screen answers three questions in this order: what is this, what is the most important thing here, what can I do.

- One focal element per screen: a number, a title, an image, a canvas. It is the largest and highest-contrast thing.
- One primary action per screen, visually distinct (filled button, FAB, extended FAB). Secondary actions are tonal or text buttons. Do not put three filled buttons in a row.
- Use at most three text levels in a view: headline or hero, body, supporting. A fourth level means the screen is doing two jobs.
- Weight and size carry hierarchy before color does. Color marks state and the single accent.
- Group with space before lines and boxes. A divider is usually a spacing failure.

## 3. Density and rhythm

- 4dp grid; the common steps are 4, 8, 12, 16, 24, 32, 48. Odd values (13, 15, 17) are mistakes.
- Screen margin 16dp on compact width, 24dp on medium and expanded. Keep text columns to about 600-840dp on large screens; do not stretch lines edge to edge on a tablet.
- Keep vertical rhythm consistent: the same gap between sibling cards, the same gap between a section title and its content, a larger gap between sections.
- Touch targets at least 48x48dp, with at least 8dp between adjacent targets.
- Use tonal surface containers (`surfaceContainerLow` to `surfaceContainerHighest`) for layering instead of drop shadows. Elevation is for things that float: sheets, menus, FABs.

## 4. One signature

An app has one detail someone would remember: a hero number that counts up, a distinctive empty-state drawing, an unusual list entrance, a custom progress ring, a haptic tick on completion. The dealt moments are candidates. Pick them, make them excellent, and keep everything else calm. Three signatures is zero signatures.

## 5. Color discipline

- Neutrals carry the UI (surface, surfaceContainer*, onSurface, onSurfaceVariant, outlineVariant). The primary hue is for the main action, selection, and the focal element, not for every icon.
- Secondary is for supporting actions and chips; tertiary is for rare contrast (a highlight, a badge, a chart accent). If the tertiary shows up on every screen it has stopped meaning anything.
- Error means error. Do not use the error role for destructive-looking decoration.
- Charts: use the role colors plus tints; avoid rainbow palettes. Encode meaning with shape or label as well as color.
- No gradients as decoration. If an image needs a scrim for text, use a scrim with the minimum alpha that passes contrast.
- Check both modes. A palette that is beautiful in light and muddy in dark is not finished.

## 6. Components in their roles

Use the platform components for what they are for; custom components only for the signature.

- `NavigationBar` for 3-5 top-level destinations on compact width; `NavigationRail` on medium and expanded; `NavigationDrawer` for many destinations. `NavigationSuiteScaffold` (Compose adaptive) switches these automatically if the project has the dependency.
- Top app bar: small for simple screens; `LargeTopAppBar` / `MediumTopAppBar` where the title is part of the character; collapse on scroll.
- FAB for the single most important creation action; extended FAB when the label adds clarity; hide on scroll down when it covers content.
- Cards: filled (`surfaceContainerHighest`) or outlined, not elevated by default. Do not put a card inside a card.
- Lists: `ListItem` with leading, headline, supporting and trailing content; swipe actions only for frequent, reversible actions.
- Dialogs for decisions that need interruption only; snackbars for undo and light feedback; bottom sheets for secondary tasks.
- Chips for filters and suggestions, not as buttons. Segmented buttons for 2-5 mutually exclusive options.
- Icons: one family, 24dp, one style (Outlined or Rounded), filled variant only for selected state.

## 7. States are screens

Design these as carefully as the content state:

- **Loading:** show structure (skeleton or placeholder blocks that match the final layout) for anything over about 300ms; use an inline linear progress for refreshes; reserve a centered spinner for rare blocking work.
- **Empty:** one small drawing or icon, one sentence that says what goes here and why, one action. No apology.
- **Error:** say what failed in plain words and offer the next step (retry, check connection, contact). Keep the previous content visible when a refresh fails.
- **Offline:** show last known content with a quiet banner; never block reading.
- **Partial:** some items loaded, some failed. Mark the failed ones, do not blank the screen.
- **Permission denied:** explain the value before asking and offer a path when denied.

## 8. Adaptive

Compact phones are the baseline, not the whole market. Use window size classes: compact, medium, expanded.

- Compact: single pane, bottom navigation.
- Medium (foldables, small tablets): navigation rail, list and detail can share the screen.
- Expanded (tablets, Chromebooks, unfolded): rail plus list-detail or supporting pane, content capped in width.
- Handle orientation and window resizing (multi-window, desktop windowing) without restarting state.
- Each archetype in `library/layouts.tsv` states its compact and expanded behavior; build both.

## 9. Squint and remove tests

- Squint test: blur your eyes at the screenshot. You should still see the focal element, the primary action and the groups. If everything has the same weight, nothing has hierarchy.
- Remove test: delete one element (a divider, an icon, a card border, a label). If the screen is no worse, leave it deleted. Repeat until it hurts.
- Swap test: if you could swap the app's name for another app's name and the screen would still be correct, the screen has no point of view.

## 10. When there is already a brand

If the project or client already has colors, a logo or a design system:
- Treat brand assets as constraints. Keep brand color as the seed for the Material role set; keep the logo clear of effects.
- Still deal the missing pieces (type, shape, layout, motion, moments) so the app is more than a recolored template.
- Report what was kept so the user can verify it is their brand and not your guess.
- If a reference image or competitor screenshot was given, name what you took from it (structure, density, tone) and what you did not (their brand marks, their copy).
