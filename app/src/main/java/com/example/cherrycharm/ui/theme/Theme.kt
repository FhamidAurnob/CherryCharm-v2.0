package com.example.cherrycharm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cherrycharm.data.local.ThemeMode

// ── Palette ───────────────────────────────────────────────────────────────────
private val Cherry = Color(0xFFB5274E)
private val CherryDark = Color(0xFF7D0D30)
private val CherryLight = Color(0xFFFFB1C1)
private val Blush = Color(0xFFFDE8EF)
private val Gold = Color(0xFFC9A227)
private val Ink = Color(0xFF1C1A1B)

private val LightColors = lightColorScheme(
    primary = Cherry,
    onPrimary = Color.White,
    primaryContainer = Blush,
    onPrimaryContainer = CherryDark,
    secondary = Color(0xFF6E5A5F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E3E7),
    onSecondaryContainer = Color(0xFF2A1A1E),
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1C9),
    onTertiaryContainer = Color(0xFF3D2F00),
    background = Color(0xFFFFFBFB),
    onBackground = Ink,
    surface = Color(0xFFFFFBFB),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4EDEF),
    onSurfaceVariant = Color(0xFF5E5458),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCF5F6),
    surfaceContainer = Color(0xFFF8F0F2),
    surfaceContainerHigh = Color(0xFFF3EAEC),
    surfaceContainerHighest = Color(0xFFEDE4E6),
    outline = Color(0xFF8F8286),
    outlineVariant = Color(0xFFE3D7DA),
    error = Color(0xFFD32F2F),
)

private val DarkColors = darkColorScheme(
    primary = CherryLight,
    onPrimary = Color(0xFF5E0B25),
    primaryContainer = CherryDark,
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFDBC0C5),
    onSecondary = Color(0xFF3E2B30),
    secondaryContainer = Color(0xFF564146),
    onSecondaryContainer = Color(0xFFF8DCE1),
    tertiary = Color(0xFFE9C451),
    onTertiary = Color(0xFF3D2F00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFE08B),
    background = Color(0xFF151213),
    onBackground = Color(0xFFEDE0E2),
    surface = Color(0xFF151213),
    onSurface = Color(0xFFEDE0E2),
    surfaceVariant = Color(0xFF3A3236),
    onSurfaceVariant = Color(0xFFD5C2C6),
    surfaceContainerLowest = Color(0xFF100D0E),
    surfaceContainerLow = Color(0xFF1E1A1B),
    surfaceContainer = Color(0xFF231E1F),
    surfaceContainerHigh = Color(0xFF2D2829),
    surfaceContainerHighest = Color(0xFF383234),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347),
    error = Color(0xFFFFB4AB),
)

/** Colors Material doesn't name but a shop needs. */
@Immutable
data class ShopColors(
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val star: Color,
    val sale: Color,
)

private val LightShop = ShopColors(
    success = Color(0xFF2E7D32), onSuccessContainer = Color(0xFF1B5E20), successContainer = Color(0xFFE6F4E7),
    warning = Color(0xFFB26A00), warningContainer = Color(0xFFFFF3E0),
    star = Color(0xFFF5A623), sale = Color(0xFFD7263D),
)
private val DarkShop = ShopColors(
    success = Color(0xFF81C784), onSuccessContainer = Color(0xFFC8E6C9), successContainer = Color(0xFF1E3A20),
    warning = Color(0xFFFFB74D), warningContainer = Color(0xFF3E2A10),
    star = Color(0xFFFFC857), sale = Color(0xFFFF6B7D),
)

val LocalShopColors = staticCompositionLocalOf { LightShop }

/** Access with `MaterialTheme.shop.success` etc. */
val MaterialTheme.shop: ShopColors
    @Composable get() = LocalShopColors.current

// ── Type: serif display for a boutique feel, clean sans for everything else ──
private val Display = FontFamily.Serif
private val base = Typography()

private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CherryCharmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalShopColors provides if (dark) DarkShop else LightShop) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
