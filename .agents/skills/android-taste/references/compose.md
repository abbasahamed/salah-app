# Kotlin / Jetpack Compose: building from the dealt tokens

Contents: 1 Theme.kt, 2 Spacing tokens, 3 Typography with the dealt fonts, 4 Edge-to-edge and insets, 5 Adaptive layout, 6 State screens, 7 Moments, 8 Previews, 9 Rules

Every snippet is a shape to adapt, not a drop-in. Confirm API names against the project's Compose BOM and Material 3 version before using them, and keep the project's package names and existing structure.

## 1. Theme.kt

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // decide on purpose; see material3-theming.md
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DealtDarkColorScheme   // from .android-taste/deal/compose/DealtTheme.kt
        else -> DealtLightColorScheme
    }
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = DealtShapes,
            content = content,
        )
    }
}
```

Delete the template's `Purple80` / `Pink40` color vals and its `Color.kt` once the dealt schemes are merged.

## 2. Spacing tokens

```kotlin
@Immutable
data class Spacing(
    val xs: Dp = 4.dp, val s: Dp = 8.dp, val m: Dp = 16.dp,
    val l: Dp = 24.dp, val xl: Dp = 32.dp, val xxl: Dp = 48.dp,
)
val LocalSpacing = staticCompositionLocalOf { Spacing() }
val MaterialTheme.spacing: Spacing
    @Composable @ReadOnlyComposable get() = LocalSpacing.current
```

Screens write `Modifier.padding(MaterialTheme.spacing.m)`, never `padding(13.dp)`. Screen margin: `m` on compact, `l` on medium and expanded.

## 3. Typography with the dealt fonts

Two options. Prefer bundled fonts (`res/font`) for production reliability; downloadable Google Fonts need Google Play services and a network on first use (fonts then fall back until they arrive).

Downloadable (needs `androidx.compose.ui:ui-text-google-fonts` and the certificates array `R.array.com_google_android_gms_fonts_certs` that the Android Studio font tooling generates):

```kotlin
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)
private fun family(name: String) = FontFamily(
    Font(GoogleFont(name), provider, FontWeight.Normal),
    Font(GoogleFont(name), provider, FontWeight.Medium),
    Font(GoogleFont(name), provider, FontWeight.SemiBold),
)
```

Bundled: put the `.ttf`/`.otf` files in `res/font/` (lowercase, underscores) and use `FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), ...)`. Check each font's license (the library fonts are open fonts, but confirm before shipping) and ship only the weights you use.

Map the pairing: display and headline use the display family, everything else the body family.

```kotlin
private val Display = family("Plus Jakarta Sans")   // the dealt display font
private val Body = family("Inter")                  // the dealt body font

private val base = Typography()
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Display),
    displayMedium = base.displayMedium.copy(fontFamily = Display),
    displaySmall = base.displaySmall.copy(fontFamily = Display),
    headlineLarge = base.headlineLarge.copy(fontFamily = Display),
    headlineMedium = base.headlineMedium.copy(fontFamily = Display),
    headlineSmall = base.headlineSmall.copy(fontFamily = Display),
    titleLarge = base.titleLarge.copy(fontFamily = Body),
    titleMedium = base.titleMedium.copy(fontFamily = Body),
    titleSmall = base.titleSmall.copy(fontFamily = Body),
    bodyLarge = base.bodyLarge.copy(fontFamily = Body),
    bodyMedium = base.bodyMedium.copy(fontFamily = Body),
    bodySmall = base.bodySmall.copy(fontFamily = Body),
    labelLarge = base.labelLarge.copy(fontFamily = Body),
    labelMedium = base.labelMedium.copy(fontFamily = Body),
    labelSmall = base.labelSmall.copy(fontFamily = Body),
)
```

Tabular figures for values that align or change: `MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum")`. Add a localized fallback family if the app supports non-Latin scripts.

## 4. Edge-to-edge and insets

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppTheme { AppRoot() } }
    }
}
```

- Apply `Scaffold`'s `innerPadding` to the content. Do not stack extra top padding for the status bar.
- Lists scroll under the bars: use `contentPadding = innerPadding` (plus your own spacing) on `LazyColumn`, not `Modifier.padding(innerPadding)`, so content slides behind translucent bars.
- For full-screen content (media, maps) use `WindowInsets.safeDrawing` / `systemBars` explicitly and handle the IME with `imePadding()`.

## 5. Adaptive layout

Compute the window size class once near the root and pass a small value down (not the whole class):

```kotlin
// material3-window-size-class (confirm the artifact and whether your version prefers currentWindowAdaptiveInfo)
val windowSizeClass = calculateWindowSizeClass(this)   // inside the Activity
val layout = when (windowSizeClass.widthSizeClass) {
    WindowWidthSizeClass.Compact -> LayoutMode.Compact
    WindowWidthSizeClass.Medium -> LayoutMode.Medium
    else -> LayoutMode.Expanded
}
```

- Compact: single pane, `NavigationBar`. Medium: `NavigationRail`, list and detail can co-exist. Expanded: rail plus list-detail or supporting pane, content width capped (for example `Modifier.widthIn(max = 840.dp)`).
- If the project has Material 3 adaptive, `NavigationSuiteScaffold` and `ListDetailPaneScaffold` do much of this. Use them only if the dependency is already there or the user agrees to add it.
- State survives resizing: hoist it into the ViewModel or `rememberSaveable`.

## 6. State screens

Empty:

```kotlin
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(MaterialTheme.spacing.s))
        FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
}
```

Loading: skeleton blocks that match the final layout, pulsing between two alphas.

```kotlin
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "alpha",
    )
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)))
}
```

Error: keep stale content, show a `Snackbar` or inline banner with a retry action. Model these as `UiState` variants so the screen cannot show two at once.

## 7. Moments

Reduced motion helper (skip the flourish, keep the final state):

```kotlin
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
```

Count-up hero number (starts at zero on first appearance, then lands):

```kotlin
@Composable
fun CountUp(target: Int, style: TextStyle, modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    var started by rememberSaveable { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { started = true }
    val value by animateIntAsState(
        targetValue = if (started) target else 0,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "countUp",
    )
    Text(NumberFormat.getIntegerInstance().format(value), style = style.copy(fontFeatureSettings = "tnum"), modifier = modifier)
}
```

Staggered entrance, first screenful only:

```kotlin
@Composable
fun Staggered(index: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index.coerceAtMost(6) * 40L); visible = true }
    AnimatedVisibility(visible, enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 8 }) { content() }
}
```

Haptic confirm on completion only: `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` is the widely available type; use newer confirm-style types if your Compose version has them. Shared element, predictive back and large title collapse: see `library/moments.tsv` for the dealt hints and confirm version support first.

## 8. Previews

Preview every stateless composable in light, dark and large font, with realistic data:

```kotlin
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun BalanceCardPreview() { AppTheme { BalanceCard(sampleBalance) } }
```

(`@PreviewLightDark`, `@PreviewFontScale` and `@PreviewScreenSizes` exist in newer `ui-tooling-preview` versions; use them if available.) Keep sample data in a preview-only file.

## 9. Rules

- No `Color(0x...)`, `Color.White/Black`, literal `sp` / `dp` numbers in screens; use `MaterialTheme.colorScheme`, `typography`, `spacing`.
- Components take `modifier: Modifier = Modifier` as the first optional parameter.
- One icon family; icons are 24dp; decorative icons get `contentDescription = null`, informative ones a real description.
- `collectAsStateWithLifecycle()` for UI state, `rememberSaveable` for UI state that must survive recreation.
- Do not mix `androidx.compose.material` and `material3`.
- Run `scripts/taste_lint.sh`; clear T0 and T1 before showing screenshots.
