package id.ac.pnm.quizbattleapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
        darkColorScheme(
                primary = AccentDarkPurple,
                secondary = PurpleGrey80,
                tertiary = Pink80,
                background = BackgroundDark,
                surface = BackgroundDark,
                onPrimary = Color.Black,
                onBackground = TextPrimaryDark,
                onSurface = TextPrimaryDark,
        )

private val LightColorScheme =
        lightColorScheme(
                primary = AccentLightPurple,
                secondary = PurpleGrey40,
                tertiary = Pink40,
                background = BackgroundLight,
                surface = BackgroundLight,
                onPrimary = Color.White,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
        )

@Composable
fun QuizBattleAppTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
