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

private val AzulClinico = Color(0xFF155EEF)
private val AzulClinicoClaro = Color(0xFFAFC6FF)
private val AzulProfundo = Color(0xFF173E72)
private val CelesteSalud = Color(0xFF006D8F)
private val RojoAlerta = Color(0xFFBA1A1A)

private val LightColors = lightColorScheme(
    primary = AzulClinico,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = AzulProfundo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF0D294F),
    tertiary = CelesteSalud,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7EFFF),
    onTertiaryContainer = Color(0xFF001F2A),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF171C25),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF171C25),
    surfaceVariant = Color(0xFFE1E7F2),
    onSurfaceVariant = Color(0xFF424752),
    surfaceContainer = Color(0xFFEEF2FA),
    surfaceContainerHigh = Color(0xFFE8EDF6),
    outline = Color(0xFF737782),
    outlineVariant = Color(0xFFC2C7D2),
    error = RojoAlerta,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = AzulClinicoClaro,
    onPrimary = Color(0xFF002E68),
    primaryContainer = Color(0xFF00449A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFB1C8F1),
    onSecondary = Color(0xFF173154),
    secondaryContainer = Color(0xFF2E486C),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFF7FD2F5),
    onTertiary = Color(0xFF003546),
    tertiaryContainer = Color(0xFF004D65),
    onTertiaryContainer = Color(0xFFC7EFFF),
    background = Color(0xFF0E1420),
    onBackground = Color(0xFFDFE4EF),
    surface = Color(0xFF0E1420),
    onSurface = Color(0xFFDFE4EF),
    surfaceVariant = Color(0xFF424752),
    onSurfaceVariant = Color(0xFFC2C7D2),
    surfaceContainer = Color(0xFF1A202C),
    surfaceContainerHigh = Color(0xFF252B37),
    outline = Color(0xFF8C919C),
    outlineVariant = Color(0xFF424752),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val PharmaTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

private val PharmaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PharmaMobilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = PharmaTypography,
        shapes = PharmaShapes,
        content = content
    )
}
