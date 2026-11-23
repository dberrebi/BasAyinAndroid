package app.basayin.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private sealed interface ReaderRow {
    val key: String

    data class Heading(val section: Section) : ReaderRow { override val key = "heading-${section.id}" }
    data class Passage(val paragraph: Paragraph) : ReaderRow { override val key = "passage-${paragraph.id}" }
    data class Pointer(val target: Section) : ReaderRow { override val key = "pointer-${target.id}" }
}

@Composable
fun ReaderScreen(
    store: BookStore,
    sections: List<Section>,
    startAt: Int,
    textSize: Int,
    favourites: List<Int>,
    onRemember: (Paragraph) -> Unit,
    toggleFavourite: (Int) -> Unit,
    openSection: (Section) -> Unit,
) {
    val language = LocalStudyLanguage.current
    val rows = remember(store, sections) {
        buildList {
            for (section in sections) {
                if (sections.size > 1) add(ReaderRow.Heading(section))
                addAll(store.passages(section.id).map(ReaderRow::Passage))
                section.redirectId?.let(store::section)?.let { add(ReaderRow.Pointer(it)) }
            }
        }
    }
    if (rows.isEmpty()) {
        EmptyMessage(string("הקטע אינו זמין במהדורה זו", "This passage is unavailable in this edition"))
        return
    }
    val first = rows.indexOfFirst { it is ReaderRow.Passage && it.paragraph.id == startAt }.coerceAtLeast(0)
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = first)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val rememberPlace by rememberUpdatedState(onRemember)
    var jump by rememberSaveable { mutableStateOf(false) }
    val saved = remember(favourites) { favourites.toSet() }
    val passages = remember(rows) { rows.filterIsInstance<ReaderRow.Passage>() }

    LaunchedEffect(rows, scroll, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { scroll.firstVisibleItemIndex to scroll.isScrollInProgress }.collectLatest { (index, scrolling) ->
                if (!scrolling) {
                    val paragraph = rows.asSequence().drop(index).filterIsInstance<ReaderRow.Passage>().firstOrNull()?.paragraph
                        ?: passages.lastOrNull()?.paragraph
                    delay(450)
                    if (paragraph != null) rememberPlace(paragraph)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
            if (passages.size > 1) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { jump = true }) { Text(string("מעבר לפסקה", "Jump to paragraph")) }
                }
                HorizontalDivider()
            }
            LazyColumn(state = scroll, modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(rows, key = ReaderRow::key) { row ->
                    when (row) {
                        is ReaderRow.Heading -> ListHeading(row.section.name.inLanguage(language))
                        is ReaderRow.Passage -> PassageView(store, row.paragraph, textSize, row.paragraph.id in saved) {
                            toggleFavourite(row.paragraph.id)
                        }
                        is ReaderRow.Pointer -> Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = { openSection(row.target) }) {
                                Text(string("לדרוש ב${row.target.name.hebrew}", "Read in ${row.target.name.english}"), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                item(key = "reading-footer") {
                    Text(
                        readingFooter(language),
                        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 28.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (jump) {
        AlertDialog(
            onDismissRequest = { jump = false },
            title = { Text(string("מעבר לפסקה", "Jump to paragraph"), fontFamily = menuFont()) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(rows.filterNot { it is ReaderRow.Pointer }, key = ReaderRow::key) { row ->
                        when (row) {
                            is ReaderRow.Heading -> ListHeading(row.section.name.inLanguage(language))
                            is ReaderRow.Passage -> MenuRow(paragraphLabel(row.paragraph.index, language)) {
                                jump = false
                                scope.launch { scroll.scrollToItem(rows.indexOf(row)) }
                            }
                            is ReaderRow.Pointer -> Unit
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { jump = false }) { Text(string("סגירה", "Close")) } },
        )
    }
}

@Composable
private fun PassageView(store: BookStore, paragraph: Paragraph, textSize: Int, saved: Boolean, onSave: () -> Unit) {
    val language = LocalStudyLanguage.current
    val context = LocalContext.current
    val emphasis = MaterialTheme.colorScheme.primary
    val text = remember(paragraph.body, emphasis) {
        buildAnnotatedString {
            for (part in passageParts(paragraph.body)) {
                if (part.bold) withStyle(SpanStyle(color = emphasis, fontWeight = FontWeight.Bold)) { append(part.text) }
                else append(part.text)
            }
        }
    }
    var actions by remember { mutableStateOf(false) }
    val reference = paragraph.citation?.let { citationLabel(store, it, language) }
    val passageLabel = paragraphLabel(paragraph.index, language)
    val copyLabel = string("העתקה", "Copy passage")
    val shareLabel = string("שיתוף", "Share passage")
    val copiedLabel = string("הקטע הועתק", "Passage copied")
    val actionsLabel = string("אפשרויות לפסקה", "Passage actions")

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 5.dp, bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                        .semantics { onLongClick(label = actionsLabel) { actions = true; true } }
                        .pointerInput(paragraph.id) { detectTapGestures(onLongPress = { actions = true }) },
                ) {
                    Text(passageLabel, fontFamily = menuFont(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge)
                    if (reference != null) Text(reference, fontFamily = menuFont(), color = emphasis,
                        style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = onSave) {
                    if (saved) {
                        Icon(Icons.Filled.Star, string("הסרה מהמועדפים", "Remove from favourites"), tint = MaterialTheme.colorScheme.secondary)
                    } else {
                        Icon(painterResource(R.drawable.ic_star_outline), string("שמירה למועדפים", "Save to favourites"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box {
                    IconButton(onClick = { actions = true }) {
                        Icon(Icons.Filled.MoreVert, string("אפשרויות לפסקה", "Passage actions"))
                    }
                    DropdownMenu(expanded = actions, onDismissRequest = { actions = false }) {
                        DropdownMenuItem(text = { Text(copyLabel) }, onClick = {
                            actions = false
                            context.getSystemService(ClipboardManager::class.java)
                                .setPrimaryClip(ClipData.newPlainText(passageLabel, exportedPassage(store, paragraph)))
                            if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, copiedLabel, Toast.LENGTH_SHORT).show()
                        })
                        DropdownMenuItem(text = { Text(shareLabel) }, onClick = {
                            actions = false
                            sharePlainText(context, exportedPassage(store, paragraph), shareLabel)
                        })
                    }
                }
            }
            SelectionContainer {
                Text(text, Modifier.fillMaxWidth(), style = TextStyle(
                    textDirection = TextDirection.Rtl, textAlign = TextAlign.Right,
                    fontFamily = HebrewFont, fontSize = textSize.sp, lineHeight = (textSize * 1.56f).sp,
                ))
            }
        }
        HorizontalDivider()
    }
}

private fun citationLabel(store: BookStore, citation: Citation, language: Language): String {
    val book = store.books.firstOrNull { it.id == citation.bookId }?.name?.inLanguage(language).orEmpty()
    return if (language == Language.Hebrew) {
        "$book ${studyNumber(citation.chapter, language)}, ${studyNumber(citation.verse, language)}"
    } else "$book ${citation.chapter}:${citation.verse}"
}

private fun exportedPassage(store: BookStore, paragraph: Paragraph): String {
    val section = store.section(paragraph.sectionId)?.name?.hebrew.orEmpty()
    val label = paragraphLabel(paragraph.index, Language.Hebrew)
    return "בת עין · $section · $label\n\n${plainText(paragraph.body)}\n\nSefaria · Jerusalem, 2001 · CC BY-NC 4.0\n" +
        "https://www.sefaria.org/Bat_Ayin\nhttps://creativecommons.org/licenses/by-nc/4.0/"
}
