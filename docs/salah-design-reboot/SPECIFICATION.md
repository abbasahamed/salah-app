# Salah 3.0 — Design Reboot Specification
**Stage 1: Professional Art Direction & Visual Concepts**

---

## 1. Executive Summary & Problem Diagnosis

Salah 3.0 has achieved functional maturity: offline city lookup, precise timezone boundary resolution, room caching, and accurate astronomical prayer calculations. However, the existing UI still suffers from common generative AI design tropes:

1. **Repetitive "SaaS-Card Kit" Syndrome**: Every piece of content is trapped inside an outlined rounded rectangle (`SalahCard` with `outlineVariant` border).
2. **Oversized Mechanical Dial**: A 236 dp concentric canvas dial with 5 nested strokes and 13 tick marks dominates the upper half of the screen, pushing the actual daily schedule down into a cramped container.
3. **Monotonous Surface Tones**: Dark mode surfaces are flat and near-identical (`#131F29` vs `#14212B`), lacking depth, elevation, or visual hierarchy.
4. **Heavy Navigation Selection**: The floating navigation bar nests multiple bordered rectangles, creating visual clutter at the bottom of the screen.
5. **Cramped Prayer List**: Daily prayer rows are compressed to 41 dp with heavy, saturated block highlights for upcoming prayers.

This reboot establishes two genuinely distinct, production-ready design directions that eliminate these defects while strictly preserving all underlying prayer calculation and location engine contracts.

---

## 2. Concept A: Editorial Luxury (Recommended Direction)

### Philosophy
Editorial Luxury treats Salah not as an electronic utility dashboard, but as a calm, contemplative instrument of daily remembrance. It draws inspiration from fine editorial typography, quiet Swiss horology, and architectural minimalism. It completely discards the oversized circular dial in favor of an authoritative, dignified typographic hero stage.

### Visual Assets
- **Today Dark**: [concept-a-today-dark.png](file:///Users/abbas/Desktop/Salah/docs/salah-design-reboot/concept-a-today-dark.png)
- **Today Light**: [concept-a-today-light.png](file:///Users/abbas/Desktop/Salah/docs/salah-design-reboot/concept-a-today-light.png)
- **Settings Dark**: [concept-a-settings-dark.png](file:///Users/abbas/Desktop/Salah/docs/salah-design-reboot/concept-a-settings-dark.png)
- **Settings Light**: [concept-a-settings-light.png](file:///Users/abbas/Desktop/Salah/docs/salah-design-reboot/concept-a-settings-light.png)

---

### Specifications (Concept A)

#### 1. Color System
| Role | Dark Theme Token | Light Theme Token | Description |
|:---|:---|:---|:---|
| **Background** | `#0B0F17` (Deep Obsidian) | `#F9F7F2` (Warm Alabaster) | High-comfort root surface canvas |
| **Surface Container** | `#121824` (Midnight Slate) | `#FFFFFF` (Pure Alabaster) | Subtle container surface for grouped content |
| **Primary Accent** | `#E2C37A` (Champagne Gold) | `#9A7228` (Warm Bronze Gold) | Refined metallic accent for active prayer & focus |
| **Accent Container** | `rgba(226, 195, 122, 0.12)` | `#F4EEDB` | Soft tonal capsule fill for countdown pill |
| **Text Primary** | `#F4F5F7` (96% luminance) | `#1C1917` (Stone Charcoal) | Crisp contrast text for headlines and times |
| **Text Secondary** | `#94A1B2` (Muted Slate) | `#78716C` (Warm Taupe) | Quiet subtitles, dates, and prayer intervals |
| **Hairline Dividers** | `rgba(255, 255, 255, 0.04)` | `#F2EEE5` | Whispered row separators without heavy visual borders |

#### 2. Typography Hierarchy
- **Primary Typefaces**: `Manrope` (Display, Headlines, Times) and `Inter` (Body, Labels, Metadata).
- **Hero Prayer Time**: `Manrope Light (300)`, 54 sp, letter-spacing `-1.5sp`, with `fontFeatureSettings = "tnum"`.
- **Hero Prayer Name**: `Manrope Bold (700)`, 34 sp, letter-spacing `-0.5sp`.
- **Schedule Times**: `Manrope Medium (500)`, 15 sp, tabular numbers, aligned cleanly with secondary AM/PM text.
- **Eyebrow / Status**: `Inter SemiBold (600)`, 11 sp, letter-spacing `1.5sp`, uppercase.
- **Line-Height Stabilization**: Adopts Now in Android's `LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.LastLineBottom)` to prevent font padding jitter across dynamic weights.

#### 3. Layout Grid & Spacing
- **8dp Base Spacing Grid**: Content paddings follow strict 8dp multiples (`8dp`, `16dp`, `24dp`).
- **Horizontal Margins**: 24 dp screen padding for spacious breathing room.
- **Row Heights**: Daily schedule rows expanded from 41 dp to **52 dp** for comfortable touch ergonomics and optical serenity.

#### 4. Component Geometry & Elevation
- **No Repeated Outlines**: Eliminates the 1 dp border on every card. Surfaces use soft tonal contrast and subtle 0.5 dp edge highlights (`rgba(226, 195, 122, 0.12)`).
- **Corner Radii**:
  - Hero Stage: 24 dp rounded corners.
  - Schedule Table: 22 dp rounded corners.
  - Floating Nav Island: 30 dp capsule.

#### 5. Prayer Progress Representation
- Replaces the 236 dp circular gauge with a **Sleek Horizontal Architectural Timeline**:
  - 4 dp track height with a 68% champagne gradient fill (`#A8833A` to `#E2C37A`).
  - Clear, legible metadata flanking the progress bar: `Sunrise 06:17 AM` · `Interval 5h 46m` · `Asr 03:21 PM`.
  - Non-intrusive countdown pill: `"in 1 hr 24 min"` nested gracefully in the top-right of the hero stage.

#### 6. Schedule Row Design
- **Passed Prayers** (Fajr, Sunrise): 45% opacity with crisp vector icons, quietly receding into the background.
- **Active / Upcoming Prayer** (Dhuhr):
  - Luminous 3.5 dp vertical gold accent indicator on the left edge.
  - Soft, warm 8% champagne background wash (`rgba(226, 195, 122, 0.08)`).
  - Bold prayer name and time in champagne gold.
  - Discreet `"NEXT"` badge.

#### 7. Floating Navigation
- Suspended island at bottom: 60 dp height, 30 dp pill radius, `rgba(18, 24, 36, 0.92)` with `backdrop-filter: blur(20px)`.
- Replaces bulky double borders with a single tonal indicator capsule for the active tab.

---

## 3. Concept B: Atmospheric Minimalism

### Philosophy
Atmospheric Minimalism grounds the daily prayer cycle in celestial rhythm. It uses subtle twilight gradients, celestial curves, and soft luminous node markers to convey the movement of the sun from dawn to nightfall.

### Visual Assets
- **Today Dark**: [concept-b-today-dark.png](file:///Users/abbas/Desktop/Salah/docs/salah-design-reboot/concept-b-today-dark.png)
- **Today Light**: [concept-b-today-light.png](file:///Users/abbas/Desktop/Salah/docs/salah-design-reboot/concept-b-today-light.png)

---

### Specifications (Concept B)

#### 1. Color System
| Role | Dark Theme Token | Light Theme Token | Description |
|:---|:---|:---|:---|
| **Background** | Radial: `#152238` (Zenith) to `#04070D` (Horizon) | Radial: `#F0F4FA` to `#DEE6F2` | Deep celestial space vs. morning sky |
| **Glass Surface** | `rgba(18, 27, 44, 0.65)` | `rgba(255, 255, 255, 0.75)` | Translucent frosted container with 24px blur |
| **Celestial Arc** | Gradient `#475569` to `#E2C37A` | Gradient `#94A3B8` to `#9A7228` | 2.5 dp parabolic trajectory path |
| **Zenith Node** | `#E2C37A` with 8px aura | `#9A7228` with 6px aura | Luminous sun position marker |
| **Text Primary** | `#F8FAFC` | `#0F172A` | High legibility starlight/slate text |
| **Text Secondary** | `#8BA0BA` | `#64748B` | Ethereal muted labels |

#### 2. Celestial Horizon Arc
- Replaces the mechanical gauge with a parabolic 2.5 dp trajectory arc:
  - Six celestial nodes corresponding to the 6 prayers placed along the curve.
  - Active prayer (Dhuhr) sits at the solar apex with a radiant luminous ring.
  - Passed prayers appear as solid muted nodes; future prayers appear as open rings.

#### 3. Schedule Presentation
- Frosted translucent container (`backdrop-filter: blur(24px)`).
- Node dots along the left side connect the list items as an astronomical timeline.
- Countdown rendered as a soft glowing ambient pill (`box-shadow: 0 0 16px rgba(226, 195, 122, 0.2)`).

---

## 4. Concept Comparison & Critique

| Design Dimension | Existing Salah 3.0 | Concept A: Editorial Luxury | Concept B: Atmospheric Minimalism |
|:---|:---|:---|:---|
| **Hero Focal Point** | Heavy 236 dp canvas dial with 13 ticks & 5 concentric arcs | Authoritative, clean typography (54 sp) with horizontal timeline | Ethereal parabolic celestial arc with glowing celestial nodes |
| **Card Architecture** | Ubiquitous 1 dp outlined cards around every section | Single cohesive container with subtle edge highlight, zero outlines | Frosted glassmorphism container with backdrop blur |
| **Dark Theme Depth** | Flat, near-identical surfaces (`#131F29` / `#14212B`) | Considered tonal contrast (`#0B0F17` root, `#121824` surface) | Rich radial celestial depth (`#152238` zenith to `#04070D`) |
| **Schedule Rows** | 41 dp cramped rows, heavy 72% saturated block highlight | **52 dp** generous rows, elegant vertical left gold bar | 50 dp frosted rows with celestial timeline node dots |
| **Navigation Bar** | Bulky double-bordered floating container | Refined 60 dp glassmorphic island with tonal pill indicator | Translucent floating pill with ambient diffuse shadow |
| **Information Density** | Poor (dial wastes 50% screen; schedule cramped) | **Optimal** (dial compressed to 120 dp; schedule spacious) | Balanced (arc takes 150 dp; schedule legible) |
| **Accessibility & 200% Font Scale** | High risk of dial clipping & text collision | **Rock Solid** (linear layout flows naturally under 200% font scale) | Good (arc maintains responsive scaling) |

---

## 5. Architectural Recommendation

### **We strongly recommend Concept A (Editorial Luxury).**

**Justification**:
1. **Calm & Dignified**: Prayer times demand solemnity and focus. Concept A's restrained typography and architectural layout provide immediate clarity without visual gimmicks.
2. **Superior Accessibility**: When dynamic font scaling is set to 200% or when running on narrow 320 dp screens, horizontal linear timelines wrap and expand naturally without graphical clipping or distortion.
3. **Pristine Settings Integration**: Concept A translates seamlessly to multi-section Settings, dialogs, and picker sheets, providing a cohesive design language across the entire application.
4. **Preservation of Engineering Contracts**: Concept A maps 1:1 to existing ViewModels (`TodayViewModel`, `LocationViewModel`) and does not require continuous trigonometric recalculations on the main UI thread.

---

## 6. Verification & Safety Notice
- **Zero Production Code Modified**: No files in `app/src/main/`, `core/prayer/`, `core/location/`, or `core/preferences/` were touched.
- **Engine Intact**: AlAdhan provider, Adhan Kotlin offline calculator, Room cache, and GPS location providers remain completely unmodified.
- **Artifacts Saved**: All 6 concept renders and this specification are safely stored under `docs/salah-design-reboot/`.

