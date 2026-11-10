package pe.edu.upeu.pharmamobilee.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AzulInstitucional = Color(0xFF2457A6)
val AzulNoche = Color(0xFF102A43)
val AzulAcento = Color(0xFF4A78D0)
val AzulClaro = Color(0xFFDCE8FF)
val CelestePanel = Color(0xFFEDF3FF)
val GrisFondo = Color(0xFFF4F7FB)
val Tinta = Color(0xFF172033)
val RojoAlerta = Color(0xFFB42318)

private val LightColors = lightColorScheme(
    primary = AzulInstitucional,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulNoche,
    secondary = Color(0xFF526987),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F5),
    onSecondaryContainer = Color(0xFF102A43),
    tertiary = Color(0xFF006A6A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDEEEE),
    onTertiaryContainer = Color(0xFF003737),
    background = GrisFondo,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFE1E7F0),
    onSurfaceVariant = Color(0xFF4A5568),
    surfaceContainer = Color(0xFFEDF1F7),
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainerHigh = Color(0xFFE5EAF2),
    outline = Color(0xFF6B778C),
    outlineVariant = Color(0xFFC5CEDB),
    error = RojoAlerta,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF164579),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFB9C7DB),
    onSecondary = Color(0xFF243246),
    secondaryContainer = Color(0xFF3A485C),
    onSecondaryContainer = Color(0xFFD5E3F7),
    tertiary = Color(0xFF8ED4D4),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF004F50),
    onTertiaryContainer = Color(0xFFA9F1F1),
    background = Color(0xFF0F141B),
    onBackground = Color(0xFFE1E7F0),
    surface = Color(0xFF0F141B),
    onSurface = Color(0xFFE1E7F0),
    surfaceVariant = Color(0xFF424A56),
    onSurfaceVariant = Color(0xFFC2CAD5),
    surfaceContainer = Color(0xFF1A2029),
    surfaceContainerLow = Color(0xFF151A22),
    surfaceContainerHigh = Color(0xFF242B35),
    outline = Color(0xFF8C96A3),
    outlineVariant = Color(0xFF424A56),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF690005)
)

private val PharmaTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 39.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
)

private val PharmaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PharmaMobilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PharmaTypography,
        shapes = PharmaShapes,
        content = content
    )
}
