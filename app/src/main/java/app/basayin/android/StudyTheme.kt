package app.basayin.android

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val StudyBarBlue = Color(0xFF111D33)
val StudyGold = Color(0xFFC6A15B)

private val lightPalette = lightColorScheme(
    primary = Color(0xFF234A7E), onPrimary = Color.White,
    secondary = Color(0xFF765923), onSecondary = Color.White,
    background = Color(0xFFF6F1E7), onBackground = Color(0xFF191D28),
    surface = Color(0xFFF6F1E7), onSurface = Color(0xFF191D28),
    surfaceVariant = Color(0xFFECE4D4), onSurfaceVariant = Color(0xFF535763),
    surfaceContainer = Color(0xFFF0E8DA), surfaceContainerHigh = Color(0xFFECE4D4),
    outline = Color(0xFF756E64), outlineVariant = Color(0xFFD8D0C2),
)

private val darkPalette = darkColorScheme(
    primary = Color(0xFFE2BD74), onPrimary = Color(0xFF342405),
    secondary = Color(0xFFE2BD74), onSecondary = Color(0xFF342405),
    background = Color(0xFF0D121C), onBackground = Color(0xFFF1EDE4),
    surface = Color(0xFF0D121C), onSurface = Color(0xFFF1EDE4),
    surfaceVariant = Color(0xFF212A3A), onSurfaceVariant = Color(0xFFBEC2CD),
    surfaceContainer = Color(0xFF141C2A), surfaceContainerHigh = Color(0xFF212A3A),
    outline = Color(0xFF9A9DA8), outlineVariant = Color(0xFF343C4A),
)

val LocalStudyLanguage = staticCompositionLocalOf { Language.Hebrew }

@OptIn(ExperimentalTextApi::class)
private fun hebrewFace(weight: FontWeight) = Font(
    R.font.noto_serif_hebrew, weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val HebrewFont = FontFamily(hebrewFace(FontWeight.Normal), hebrewFace(FontWeight.Bold))

@Composable
fun string(hebrew: String, english: String): String = Title(hebrew, english).inLanguage(LocalStudyLanguage.current)

@Composable
fun StudyTheme(settings: StudySettings, content: @Composable () -> Unit) {
    val dark = when (settings.appearance) {
        Appearance.System -> isSystemInDarkTheme()
        Appearance.Light -> false
        Appearance.Dark -> true
    }
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val direction = if (settings.language == Language.Hebrew) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalStudyLanguage provides settings.language, LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = if (dark) darkPalette else lightPalette, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyToolbar(
    title: String,
    goBack: (() -> Unit)?,
    openFavourites: (() -> Unit)?,
    onShare: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                fontFamily = if (LocalStudyLanguage.current == Language.Hebrew) HebrewFont else FontFamily.Default,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (goBack == null) {
                Image(painterResource(R.drawable.bas_ayin_mark), null, modifier = Modifier.size(48.dp))
            } else {
                IconButton(onClick = goBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, string("חזרה", "Back"))
                }
            }
        },
        actions = {
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, string("שיתוף", "Share"))
                }
            }
            if (openFavourites != null) {
                IconButton(onClick = openFavourites) {
                    Icon(Icons.Filled.Star, string("מועדפים", "Favourites"), tint = StudyGold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = StudyBarBlue, titleContentColor = Color.White,
            navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
        ),
    )
}
