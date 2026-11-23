package app.basayin.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val model: StudyModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            StudyTheme(state.settings) { BasAyin(state, model) }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refreshReading()
    }
}

@Composable
private fun BasAyin(state: StudyState, model: StudyModel) {
    val store = state.store
    if (store == null) {
        StudyPage(string("בת עין", "Bas Ayin")) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loadFailed) {
                    Column(
                        Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(string("טעינת הספר נכשלה", "The book could not be loaded"), textAlign = TextAlign.Center)
                        OutlinedButton(onClick = model::load) { Text(string("נסו שוב", "Try again")) }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
        return
    }

    val nav = rememberNavController()
    val context = LocalContext.current
    val language = LocalStudyLanguage.current
    val currentState by rememberUpdatedState(state)
    val back: () -> Unit = remember(nav) { { nav.popBackStack() } }
    val openReader: (List<Int>, Int) -> Unit = remember(nav) {
        { ids, passage -> if (ids.isNotEmpty()) nav.navigate("reader/${ids.joinToString(",")}/$passage") }
    }
    val openPassage: (Paragraph) -> Unit = remember(openReader) { { openReader(listOf(it.sectionId), it.id) } }
    val openSection: (Section) -> Unit = remember(openReader) { { openReader(listOf(it.id), 0) } }

    NavHost(nav, startDestination = "home") {
        composable("home") {
            val weeklyTitle = currentState.reading?.title?.inLanguage(language)
            val shareLabel = string("שיתוף", "Share")
            StudyPage(
                string("בת עין", "Bas Ayin"),
                favourites = { nav.navigate("favourites") },
                onShare = { sharePlainText(context, homeShareText(language, weeklyTitle), shareLabel) },
            ) {
                HomeScreen(store, currentState, onOpen = { nav.navigate(it) }, onResume = openPassage)
            }
        }
        composable("book/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
            val book = store.books.firstOrNull { it.id == entry.arguments?.getInt("id") }
            StudyPage(book?.name?.inLanguage(LocalStudyLanguage.current) ?: string("תוכן העניינים", "Contents"), back) {
                SectionList(book?.let { store.bookSections(it.id) }.orEmpty(), openSection)
            }
        }
        composable("festivals") {
            StudyPage(string("מועדים ושבתות מיוחדות", "Festivals & special Shabbatot"), back) {
                SectionList(store.festivals(), openSection)
            }
        }
        composable("appendix") {
            StudyPage(string("ליקוטים ושו״ת", "Likkutim & responsa"), back) {
                SectionList(store.appendix(), openSection)
            }
        }
        composable("weekly") {
            StudyPage(string("קריאת השבוע", "This week"), back) {
                WeeklyScreen(store, currentState.reading, onRead = { openReader(it, 0) }, onOpen = openSection)
            }
        }
        composable("search") {
            StudyPage(string("חיפוש", "Search"), back) { SearchScreen(store, openPassage) }
        }
        composable("history") {
            StudyPage(string("היסטוריית קריאה", "Reading history"), back) {
                SavedPassages(store, currentState.history, string("טרם נקראו קטעים", "No passages read yet"), openPassage)
            }
        }
        composable("favourites") {
            StudyPage(string("מועדפים", "Favourites"), back) {
                SavedPassages(store, currentState.favourites, string("טרם נשמרו מועדפים", "No favourites yet"), openPassage)
            }
        }
        composable("settings") {
            StudyPage(string("הגדרות", "Settings"), back) { SettingsScreen(currentState.settings, model::updateSettings) }
        }
        composable("about") {
            StudyPage(string("אודות", "About"), back) { AboutScreen(store.calendarYears, store.generatedUtc) }
        }
        composable("help") { StudyPage(string("עזרה", "Help"), back) { HelpScreen() } }
        composable(
            "reader/{sections}/{passage}",
            arguments = listOf(navArgument("sections") { type = NavType.StringType }, navArgument("passage") { type = NavType.IntType }),
        ) { entry ->
            val sections = entry.arguments?.getString("sections").orEmpty().split(',')
                .mapNotNull { it.toIntOrNull()?.let(store::section) }
            val title = sections.singleOrNull()?.name?.inLanguage(language)
                ?: string("קריאת השבוע", "This week")
            val portion = readerPortionTitle(language, sections, currentState.reading)
            val shareLabel = string("שיתוף", "Share")
            StudyPage(title, back, onShare = {
                sharePlainText(context, portionShareText(language, portion), shareLabel)
            }) {
                ReaderScreen(
                    store, sections, entry.arguments?.getInt("passage") ?: 0,
                    currentState.settings.textSize, currentState.favourites, model::rememberPlace, model::toggleFavourite, openSection,
                )
            }
        }
    }
}

@Composable
private fun StudyPage(
    title: String,
    back: (() -> Unit)? = null,
    favourites: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(topBar = { StudyToolbar(title, back, favourites, onShare) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) { content() }
    }
}
