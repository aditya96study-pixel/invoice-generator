package com.amitabhtours.invoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF14213D)
val NavyLight = Color(0xFF24365F)
val Marigold = Color(0xFFF5B82E)
val MarigoldDeep = Color(0xFFC98A0E)
val Success = Color(0xFF1E8E3E)
val Danger = Color(0xFFC62828)

@Immutable
data class ExtraColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val hint: Color,
    val cardBorder: Color,
    val hero: Color,
    val onHero: Color,
    val accent: Color,
    val onAccent: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
    val chart6: Color,
)

private val LightExtra = ExtraColors(
    success = Color(0xFF1E7D3A), successContainer = Color(0xFFDDF3E2),
    warning = Color(0xFF8A5A00), warningContainer = Color(0xFFFFF0CC),
    danger = Color(0xFFB3261E), dangerContainer = Color(0xFFFCE4E2),
    hint = Color(0xFF9AA1AE), cardBorder = Color(0xFFE4E7EE),
    hero = Navy, onHero = Color.White, accent = Marigold, onAccent = Navy,
    // Validated categorical palette (colour-blind safe), fixed order.
    chart1 = Color(0xFF2A78D6), chart2 = Color(0xFFEB6834), chart3 = Color(0xFF1BAF7A),
    chart4 = Color(0xFFEDA100), chart5 = Color(0xFFE87BA4), chart6 = Color(0xFF008300),
)

private val DarkExtra = ExtraColors(
    success = Color(0xFF7FD69A), successContainer = Color(0xFF173A24),
    warning = Color(0xFFFFCF6B), warningContainer = Color(0xFF3D2F0B),
    danger = Color(0xFFFFB4AB), dangerContainer = Color(0xFF4A1512),
    hint = Color(0xFF6E788C), cardBorder = Color(0xFF2A3550),
    hero = Color(0xFF1C2A4A), onHero = Color.White, accent = Marigold, onAccent = Navy,
    chart1 = Color(0xFF3987E5), chart2 = Color(0xFFD95926), chart3 = Color(0xFF199E70),
    chart4 = Color(0xFFC98500), chart5 = Color(0xFFD55181), chart6 = Color(0xFF008300),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3F5),
    onPrimaryContainer = Navy,
    secondary = Marigold,
    onSecondary = Color(0xFF261A00),
    secondaryContainer = Color(0xFFFFEBB8),
    onSecondaryContainer = Color(0xFF3A2A00),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFF0EB),
    onTertiaryContainer = Color(0xFF00201D),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF151A26),
    surface = Color(0xFFF5F6FA),
    onSurface = Color(0xFF151A26),
    surfaceVariant = Color(0xFFE6E9F0),
    onSurfaceVariant = Color(0xFF5A6273),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFF1F6),
    surfaceContainerHigh = Color(0xFFE9ECF2),
    surfaceContainerHighest = Color(0xFFE3E6EE),
    outline = Color(0xFFB4BAC6),
    outlineVariant = Color(0xFFE1E4EB),
    error = Color(0xFFB3261E),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFB7C7F2),
    onPrimary = Navy,
    primaryContainer = Color(0xFF26375E),
    onPrimaryContainer = Color(0xFFDCE3F5),
    secondary = Marigold,
    onSecondary = Color(0xFF261A00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFFFEBB8),
    tertiary = Color(0xFF4FC3B4),
    onTertiary = Color(0xFF00201D),
    background = Color(0xFF0E1525),
    onBackground = Color(0xFFE3E7F1),
    surface = Color(0xFF0E1525),
    onSurface = Color(0xFFE3E7F1),
    surfaceVariant = Color(0xFF283248),
    onSurfaceVariant = Color(0xFFAEB7CA),
    surfaceContainerLowest = Color(0xFF141D30),
    surfaceContainerLow = Color(0xFF162036),
    surfaceContainer = Color(0xFF1A253D),
    surfaceContainerHigh = Color(0xFF202C46),
    surfaceContainerHighest = Color(0xFF26334F),
    outline = Color(0xFF59647C),
    outlineVariant = Color(0xFF2A3550),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontSize = 16.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(darkMode: String = "system", content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

val extra: ExtraColors
    @Composable get() = LocalExtraColors.current

val MoneyStyle = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
