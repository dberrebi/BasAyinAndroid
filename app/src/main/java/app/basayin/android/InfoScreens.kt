package app.basayin.android

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settings: StudySettings, onChange: (StudySettings) -> Unit) {
    var size by remember(settings.textSize) { mutableFloatStateOf(settings.textSize.toFloat()) }
    StudyList {
        item {
            ListHeading(string("שפת הממשק", "Interface language"))
            SettingChoices(
                listOf(Language.Hebrew to "עברית", Language.English to "English"), settings.language,
            ) { onChange(settings.copy(language = it)) }
            SettingsText(string("טקסט הספר מופיע בעברית.", "The book text is in Hebrew."))
            HorizontalDivider()
        }
        item {
            ListHeading(string("לוח קריאות", "Reading calendar"))
            SettingChoices(listOf(false to string("חוץ לארץ", "Diaspora"), true to string("ישראל", "Israel")), settings.israel) {
                onChange(settings.copy(israel = it))
            }
            HorizontalDivider()
        }
        item {
            ListHeading(string("מראה", "Appearance"))
            SettingChoices(listOf(
                Appearance.System to string("לפי המכשיר", "System default"),
                Appearance.Light to string("בהיר", "Light"),
                Appearance.Dark to string("כהה", "Dark"),
            ), settings.appearance) { onChange(settings.copy(appearance = it)) }
            HorizontalDivider()
        }
        item {
            ListHeading(string("פרטיות", "Privacy"))
            SourceLink(string("מדיניות הפרטיות", "Privacy policy"), privacyPolicyUrl)
            SettingsText(string(
                "היישום אינו אוסף מידע. הקישור נפתח בדפדפן.",
                "The app collects no data. The link opens in the browser.",
            ))
        }
        item {
            ListHeading(string("גודל טקסט הקריאה", "Reading text size"))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                Slider(value = size, onValueChange = { size = it }, valueRange = 18f..38f, steps = 19,
                    onValueChangeFinished = { onChange(settings.copy(textSize = size.roundToInt())) })
                Text("בראשית ברא אלהים את השמים ואת הארץ", Modifier.fillMaxWidth(), style = TextStyle(
                    textDirection = TextDirection.Rtl, textAlign = TextAlign.Right,
                    fontFamily = HebrewFont, fontSize = size.sp, lineHeight = (size * 1.5f).sp,
                ))
            }
        }
    }
}

@Composable
private fun <T> SettingChoices(choices: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        for ((choice, label) in choices) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(selected = choice == selected, role = Role.RadioButton, onClick = { onSelect(choice) })
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = choice == selected, onClick = null)
                Text(label)
            }
        }
    }
}

@Composable
private fun SettingsText(text: String) {
    Text(text, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun AboutScreen(years: String) {
    StudyList {
        item {
            ListHeading(string("הספר ומחברו", "The book and its author"))
            InfoText(string(
                "בת עין הוא ספר דרשות חסידי על פרשות התורה, המועדים ושבתות מיוחדות, מאת רבי אברהם דב מאווריטש, שחי ופעל בצפת.",
                "Bas Ayin is a collection of Hasidic discourses on the weekly Torah portions, festivals, and special Shabbatot by Rabbi Avraham Dov of Ovruch, who settled in Safed.",
            ))
        }
        item {
            ListHeading(string("מהדורת הטקסט", "Text edition"))
            InfoText(string(
                "בת עין, ירושלים תשס״א (2001), באדיבות ספריא. לשימוש לא מסחרי ברישיון CC BY-NC 4.0. היישום מעבד את העיצוב ומוסיף מראי מקום וכותרות ניווט.",
                "Bat Ayin, Jerusalem, 2001, via Sefaria, for noncommercial use under CC BY-NC 4.0. The app adapts formatting and adds reading references and navigation labels.",
            ))
            SourceLink(string("בת עין בספריא", "Bat Ayin on Sefaria"), "https://www.sefaria.org/Bat_Ayin")
            SourceLink(string("המהדורה בספרייה הלאומית", "National Library of Israel edition"),
                "https://www.nli.org.il/he/books/NNL_ALEPH002047217/NLI")
            SourceLink("CC BY-NC 4.0", "https://creativecommons.org/licenses/by-nc/4.0/")
        }
        item {
            ListHeading(string("לוח קריאות וגופן", "Reading calendar and typeface"))
            InfoText(string(
                "לוח פרשות התורה והמועדים לשנים $years באדיבות Hebcal.com, ברישיון CC BY 4.0. הגופן Noto Serif Hebrew מאת The Noto Project Authors, ברישיון SIL Open Font License 1.1.",
                "Torah reading and holiday schedule for $years from Hebcal.com, CC BY 4.0. Noto Serif Hebrew, © 2022 The Noto Project Authors, under the SIL Open Font License 1.1.",
            ))
            SourceLink("Hebcal.com", "https://www.hebcal.com")
            SourceLink("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/")
            SourceLink("SIL Open Font License 1.1", "https://openfontlicense.org/open-font-license-official-text/")
        }
        item {
            ListHeading(string("פרטיות", "Privacy"))
            InfoText(string(
                "הקריאה והחיפוש זמינים ללא חיבור לאינטרנט. היסטוריית הקריאה, המועדפים וההגדרות נשמרים במכשיר בלבד. קישורי המקורות נפתחים בדפדפן.",
                "Reading and search work offline. Reading history, favourites, and settings are stored on this device. Source links open in your browser.",
            ))
            SourceLink(string("מדיניות הפרטיות", "Privacy policy"), privacyPolicyUrl)
        }
    }
}

@Composable
fun HelpScreen() {
    StudyList {
        item {
            ListHeading(string("מציאת דרוש", "Find a discourse"))
            InfoText(string(
                "בחרו חומש מתוכן העניינים כדי לעיין בדרשות לפי סדר הספר. אפשר למצוא את דרשות המועדים גם ברשימת המועדים והשבתות המיוחדות.",
                "Choose a book under Contents to browse in the edition's order. Festival discourses are also listed under Festivals & special Shabbatot.",
            ))
            ListHeading(string("המשך קריאה ומועדפים", "Resume and save passages"))
            InfoText(string(
                "המקום האחרון נשמר אחרי עצירה קצרה בקריאה. לחצו על הכוכב ליד פסקה כדי לשמור אותה. מתפריט הפסקה ניתן להעתיק או לשתף אותה עם מקורה.",
                "Your place is saved after a short pause. Tap the star by a paragraph to save it. Use the paragraph menu to copy or share it with its source.",
            ))
            ListHeading(string("חיפוש בעברית", "Search in Hebrew"))
            InfoText(string(
                "חפשו לפי מילים בעברית. אפשר להקליד עם ניקוד וגרשיים או בלעדיהם, למשל חז״ל או חזל.",
                "Search with Hebrew words, with or without vowel points or quotation marks. For example, חז״ל and חזל give the same results.",
            ))
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun SourceLink(label: String, url: String) {
    val handler = LocalUriHandler.current
    val context = LocalContext.current
    val unavailable = string("לא נמצא דפדפן לפתיחת הקישור", "No browser available to open the link")
    TextButton(onClick = {
        try {
            handler.openUri(url)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
        }
    }, modifier = Modifier.padding(horizontal = 12.dp)) { Text(label) }
}
