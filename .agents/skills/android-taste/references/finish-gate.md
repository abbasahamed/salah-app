# Finish gate and review rubric

Contents: 1 Order of checks, 2 What the scripts catch and what they cannot, 3 Screenshot review, 4 Rubric, 5 Rounds and waivers, 6 Handover

## 1. Order of checks

1. Build and install (`./gradlew :app:installDebug`) if a toolchain and a device or emulator exist. If not, say so; do not imply the screens were seen.
2. `scripts/taste_lint.sh <root>`: template and generic-UI tells.
3. `scripts/contrast.sh --project <root>`: Material 3 role pairs in light and dark.
4. `scripts/shoot.sh --package <id> [--link <deeplink> ...]`: screenshots in light and dark at font scale 1.0 and 1.5.
5. Read the screenshots (section 3).
6. `scripts/gate.sh --project <root> --shots taste-shots --need-shots <n> [--lint]`.

## 2. What the scripts catch and what they cannot

Catch: the template palette and greeting, default launcher green, named Android colors, inline color literals, hex in layouts, missing dark theme, missing edge-to-edge, no adaptive layout signals, mixed icon families, emoji in UI text, odd dp values, hardcoded font sizes, generic copy, shouty strings, missing motion, splash and themed icon, role pairs below WCAG.

Cannot catch: whether the hierarchy works, whether spacing feels right, whether text clips at large font, whether an image behind text is legible, whether the app feels like one product, whether copy has a voice, whether tablet layouts are actually good. Those need eyes. A passing gate means "no known tells", not "good".

Contrast only sees colors defined as named resources or Kotlin `val`s. Colors computed in code, dynamic color and image backgrounds are invisible to it; check those in screenshots.

## 3. Screenshot review

Open every screenshot (view the PNGs, do not just count them). For each screen, answer:

- **Focal point:** can you name the one thing the screen is about in one second? Squint: is it still the heaviest element?
- **Primary action:** is there exactly one, and is it unmistakable?
- **Dark mode:** same hierarchy as light? No glaring white blocks, no muddy low-contrast text, no baked-in light backgrounds?
- **Font scale 1.5:** nothing clipped, overlapped or hidden; buttons wrap rather than truncate; fixed heights absent from text containers.
- **System bars:** content scrolls under translucent bars; nothing is hidden behind the status bar, navigation bar, cutout or gesture area.
- **Spacing:** consistent margins; same gap between sibling groups; nothing touching an edge it should not.
- **States:** empty, loading, error and offline were captured or previewed, and each has a point of view.
- **Typography:** two families at most; display font only where it belongs; numbers align.
- **Color:** the primary hue is doing a job (the main action, selection, the hero), not everywhere; tertiary is rare.
- **Content:** no placeholder text, no identical rows, no "Test", no template greeting.
- **Large screens:** if a tablet or foldable run was possible, check rail navigation, list-detail, content width cap. If not possible, say it was not checked.

Write down what you changed because of what you saw. If you changed nothing after looking, look again.

## 4. Rubric (score yourself honestly, 0-2 each)

| Dimension | 0 | 1 | 2 |
|---|---|---|---|
| Direction | template look | some tokens, inconsistent | one dealt system used everywhere |
| Hierarchy | everything equal | focal element exists on some screens | clear focal element and one primary action on every screen |
| Type | system font, one weight | pairing set, uneven use | pairing mapped to the scale, tabular figures where numbers align |
| Color | template or arbitrary | roles used, some literals | roles only, tonal layering, passes contrast in both modes |
| Space and density | arbitrary values | mostly on the grid | 4dp grid, consistent rhythm, 48dp targets |
| States | content only | some states | empty, loading, error, offline designed |
| Platform | portrait phone only | dark or insets handled | dark, edge-to-edge, adaptive, font scale, back all handled |
| Motion | none or noisy | some purposeful motion | hero entrance plus 1-2 moments, reduced-motion safe |
| Content | placeholders | mixed | real copy, realistic data, locale-correct formatting |

Below 12 of 18: do another round (within the limit). 15 or more with no T0/T1 findings: ship.

## 5. Rounds and waivers

- Maximum three gate rounds per build. Each round: fix the listed FAIL items, rerun. After round three, stop and hand over with the open items named.
- A finding may be waived on purpose in `.android-taste/waivers.txt`, one substring per line, with a comment line above explaining why:

```
# Utility app uses the system font on purpose: it matches the device, and the type is not the product.
System font only
# Single-screen app for a kiosk tablet in landscape; adaptive layouts are out of scope.
No adaptive layout
```

- Waive T2 freely when it is a deliberate choice. Waive T0 and T1 rarely and always with a reason; a waiver without a reason is a skipped check.
- `gate.sh --reset` clears the round counter for a new build.

## 6. Handover

Report in this order, briefly:

1. The dealt direction (palette, type, shape, layout, motion, moments) and anything kept from the project.
2. Files added or changed.
3. `BEHAVIOR CHANGE` items (for example dynamic color turned off, theme parent changed, navigation component swapped, orientation lock removed).
4. Gate result: round number, remaining WARNs, waivers and their reasons.
5. What was not run or not seen (no device, no tablet, no Gradle) with the exact commands for the user.
6. Two or three things the user could decide next (a second palette, a brand seed, a widget, tablet polish).
