package app.basayin.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(store: BookStore, state: StudyState, onOpen: (String) -> Unit, onResume: (Paragraph) -> Unit) {
    val language = LocalStudyLanguage.current
    val resume = state.history.firstNotNullOfOrNull(store::paragraph)
    val resumeDetail = resume?.let { location(store, it) }
        ?: resumePlaceholder(language, savedPlaceMissing = state.history.isNotEmpty())
    StudyList {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    string("רבי אברהם דב מאווריטש", "Rabbi Avraham Dov of Ovruch"),
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = menuFont(), textAlign = TextAlign.Center, fontSize = 16.sp,
                )
                HomeCard(
                    string("קריאת השבוע", "This week"),
                    state.reading?.title?.inLanguage(language),
                ) { onOpen("weekly") }
                HomeCard(
                    string("המשך קריאה", "Continue reading"),
                    resumeDetail,
                    enabled = resume != null,
                ) { if (resume != null) onResume(resume) }
            }
        }
        item { MenuRow(string("חיפוש בספר", "Search the book")) { onOpen("search") } }
        item { MenuRow(string("היסטוריית קריאה", "Reading history")) { onOpen("history") } }
        item { ListHeading(string("תוכן העניינים", "Contents")) }
        items(store.books, key = { "book-${it.id}" }) { book ->
            MenuRow(book.name.inLanguage(language)) { onOpen("book/${book.id}") }
        }
        item { MenuRow(string("מועדים ושבתות מיוחדות", "Festivals & special Shabbatot")) { onOpen("festivals") } }
        item { MenuRow(string("ליקוטים ושו״ת", "Likkutim & responsa")) { onOpen("appendix") } }
        item { ListHeading(string("כלים ומידע", "Tools & information")) }
        item { MenuRow(string("הגדרות", "Settings")) { onOpen("settings") } }
        item { MenuRow(string("עזרה", "Help")) { onOpen("help") } }
        item { MenuRow(string("אודות", "About")) { onOpen("about") } }
    }
}

@Composable
private fun HomeCard(title: String, subtitle: String?, enabled: Boolean = true, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontFamily = menuFont(), fontSize = 20.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun SectionList(sections: List<Section>, onOpen: (Section) -> Unit) {
    val language = LocalStudyLanguage.current
    StudyList {
        items(sections, key = Section::id) { section -> MenuRow(section.name.inLanguage(language)) { onOpen(section) } }
    }
}

@Composable
fun WeeklyScreen(store: BookStore, reading: Reading?, onRead: (List<Int>) -> Unit, onOpen: (Section) -> Unit) {
    if (reading == null) {
        EmptyMessage(string("לא נמצאה קריאה לשבוע זה. לוח הקריאות מכסה את השנים ${store.calendarYears}.",
            "No reading is available for this week. The calendar covers ${store.calendarYears}."))
        return
    }
    val language = LocalStudyLanguage.current
    val locale = if (language == Language.Hebrew) Locale.forLanguageTag("he-IL") else Locale.ENGLISH
    val date = reading.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    StudyList {
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(reading.title.inLanguage(language), fontFamily = menuFont(), fontSize = 24.sp, textAlign = TextAlign.Center)
                Button(onClick = { onRead(reading.sectionIds) }) { Text(string("לקריאה", "Read this week")) }
            }
            HorizontalDivider()
        }
        items(reading.sectionIds.mapNotNull(store::section), key = Section::id) { section ->
            MenuRow(section.name.inLanguage(language)) { onOpen(section) }
        }
    }
}

@Composable
fun SavedPassages(store: BookStore, ids: List<Int>, empty: String, onOpen: (Paragraph) -> Unit) {
    val paragraphs = ids.mapNotNull(store::paragraph)
    if (paragraphs.isEmpty()) {
        EmptyMessage(empty)
        return
    }
    StudyList {
        items(paragraphs, key = Paragraph::id) { passage ->
            PassageRow(store, passage, excerpt(passage.body, emptyList())) { onOpen(passage) }
        }
    }
}

@Composable
fun SearchScreen(store: BookStore, onOpen: (Paragraph) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val terms = searchTerms(query)
    LaunchedEffect(terms) {
        result = null
        if (terms.isEmpty()) return@LaunchedEffect
        delay(180)
        result = try {
            SearchResult(terms, withContext(Dispatchers.IO) { store.search(query) })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e("BasAyin", "Unable to search the bundled edition", error)
            SearchResult(terms, null)
        }
    }
    val current = result?.takeIf { it.terms == terms }
    StudyList {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(256) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                label = { Text(string("מילה או ביטוי בעברית", "Hebrew word or phrase")) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (query.isEmpty()) null else {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, string("ניקוי", "Clear")) } }
                },
                textStyle = TextStyle(textDirection = TextDirection.Rtl, fontFamily = HebrewFont, fontSize = 19.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
        }
        when {
            terms.isEmpty() -> item { EmptyMessage(string("יש להקליד מילה בעברית לחיפוש בטקסט", "Enter Hebrew to search the full text")) }
            current == null -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            current.hits == null -> item { EmptyMessage(string("החיפוש נכשל. נסו שוב.", "Search failed. Try another query.")) }
            current.hits.isEmpty() -> item { EmptyMessage(string("לא נמצאו תוצאות", "No matches")) }
            else -> items(current.hits, key = { it.paragraph.id }) { hit ->
                PassageRow(store, hit.paragraph, hit.excerpt) {
                    keyboard?.hide()
                    onOpen(hit.paragraph)
                }
            }
        }
    }
}

private data class SearchResult(val terms: List<String>, val hits: List<SearchHit>?)

@Composable
private fun PassageRow(store: BookStore, paragraph: Paragraph, preview: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(location(store, paragraph), fontFamily = menuFont(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(preview, style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Right,
                fontFamily = HebrewFont, fontSize = 17.sp, lineHeight = 26.sp), maxLines = 3,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        }
        HorizontalDivider()
    }
}

@Composable
fun location(store: BookStore, paragraph: Paragraph): String {
    val language = LocalStudyLanguage.current
    val section = store.section(paragraph.sectionId)?.name?.inLanguage(language).orEmpty()
    return "$section · ${paragraphLabel(paragraph.index, language)}"
}

fun paragraphLabel(index: Int, language: Language): String =
    Title("פסקה ${studyNumber(index, Language.Hebrew)}", "Paragraph $index").inLanguage(language)

@Composable
fun StudyList(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp), content = content)
    }
}

@Composable
fun MenuRow(title: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(title, fontFamily = menuFont(), fontSize = 20.sp, textAlign = TextAlign.Center)
        }
        HorizontalDivider()
    }
}

@Composable
fun ListHeading(title: String) {
    Text(title, Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp).semantics { heading() },
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall,
        fontFamily = menuFont(), fontWeight = FontWeight.Bold)
}

@Composable
fun EmptyMessage(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(28.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun menuFont(): FontFamily = if (LocalStudyLanguage.current == Language.Hebrew) HebrewFont else FontFamily.Default
