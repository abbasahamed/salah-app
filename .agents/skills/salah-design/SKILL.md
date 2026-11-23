---
name: salah-design
description: Design or review Salah's native Android interface in Jetpack Compose while preserving prayer-time correctness and accessibility. Use for Salah UI redesigns and visual refinements.
---

# Salah design

Apply this skill when the user requests Salah interface design or implementation. Inspect the existing screen, design-system tokens, relevant ViewModels, and the user's specific brief before choosing changes.

## Visual direction

Create a premium, Apple-inspired minimalist experience adapted to native Android. Keep the interface calm, purposeful, and legible. Use light-first warm ivory and white surfaces with muted sage and restrained accents. Give dark mode its own considered surface hierarchy, contrast, and accent treatment. Establish a deliberate type scale with premium typography, clear hierarchy, readable numerals, and generous whitespace. Use a consistent spacing rhythm, rounded geometry, and coherent iconography. Remove decorative labels, redundant controls, and visual clutter.

Give prayer-time cards a clear hierarchy across prayer name, time, current and next states, and relevant source. Make the next-prayer visualization beautiful and immediately understandable while keeping the name, actual time, date when relevant, and countdown readable without relying on color or motion alone. Design loading, unavailable, cached, and offline states with equal care and truthful wording.

## Native implementation and motion

Build with Kotlin, Jetpack Compose, Material 3, and the project's existing design-system components and tokens. Use the upstream Anthropic frontend-design and Emil Kowalski design-engineering skills for transferable aesthetic and interaction principles. Translate those principles into appropriate Android and Compose APIs. Treat CSS, HTML, React, browser, and web-library instructions in those skills as web-specific examples.

Use restrained motion to clarify user actions, state changes, and spatial relationships. Keep transitions responsive and avoid decorative looping or repeated entrances. Respect Android's animation duration scale and reduced-motion settings, and preserve an immediate, understandable state when motion is disabled. Avoid animations that change the apparent value or timing of prayer data.

## Accessibility and layout

Support system font scaling, including 200% text, readable contrast in both themes, screen-reader semantics, meaningful labels, focus order, and touch targets. Do not communicate prayer state through color alone. Adapt to small phones, landscape, tablets, and varying window sizes using flexible and scrollable Compose layouts. Handle edge-to-edge system bars, display cutouts, and relevant insets so content and controls remain visible and reachable.

## Protect prayer-time behavior

Keep calculation, location, caching, source selection, formatting, countdown, and persistence in the existing domain, repository, preferences, and ViewModel boundaries. UI consumes their state and emits user events. Preserve the six entries, the distinction between Sunrise and obligatory prayers, time zones, dates, user adjustments, settings, source attribution, and unavailable states. Never fabricate or alter prayer times to improve a layout. Show preview values only in clearly identified previews. Preserve lifecycle-aware collection and avoid calculations, polling, or network requests in composables.

## Visual review

Review screenshots from the actual running app in light and dark themes at representative phone and wider window sizes, including large font scaling and long labels. Inspect Today, prayer cards, the next-prayer state, Settings, and relevant loading and error states for clipping, contrast, insets, and interaction clarity. Exercise screen-reader semantics and reduced motion when tooling permits. Report any checks that could not be performed.
