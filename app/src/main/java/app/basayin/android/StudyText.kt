package app.basayin.android

import java.time.LocalDate

enum class Language { Hebrew, English }
enum class Appearance { System, Light, Dark }

data class Title(val hebrew: String, val english: String) {
    fun inLanguage(language: Language) = if (language == Language.Hebrew) hebrew else english
}

data class Book(val id: Int, val name: Title)

data class Section(
    val id: Int,
    val bookId: Int?,
    val name: Title,
    val kind: String,
    val festivalOrder: Int?,
    val redirectId: Int?,
)

data class Citation(val bookId: Int, val chapter: Int, val verse: Int)

data class Paragraph(
    val id: Int,
    val sectionId: Int,
    val index: Int,
    val body: String,
    val citation: Citation?,
)

data class SearchHit(val paragraph: Paragraph, val excerpt: String)

data class Reading(val date: LocalDate, val title: Title, val sectionIds: List<Int>)
data class Festival(val label: String, val title: Title, val sectionIds: List<Int>)

/** Festival rows are supplied from today through the coming Shabbat, in date order. */
fun combineReading(weekly: Reading, festivals: List<Festival>): Reading? {
    val sectionIds = weekly.sectionIds.toMutableSet()
    val hebrew = weekly.title.hebrew.split(" · ").filter(String::isNotBlank).toMutableList()
    val english = weekly.title.english.split(" · ").filter(String::isNotBlank).toMutableList()
    for (days in festivals.groupBy(Festival::label).values) {
        val title = days.first().title
        if (title.hebrew !in hebrew) {
            hebrew += title.hebrew
            english += title.english
        }
        sectionIds.addAll(days.flatMap(Festival::sectionIds).sorted())
    }
    return if (sectionIds.isEmpty()) null else Reading(
        weekly.date, Title(hebrew.joinToString(" · "), english.joinToString(" · ")), sectionIds.toList(),
    )
}

private val points = Regex("[\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]")
private val quotes = Regex("[\"'׳״`’‘“”]")
private val words = Regex("[א-ת]+")
private val displayWords = Regex("\\S+")
private val tags = Regex("</?b>")
private val emphasis = Regex("<b>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)

fun searchTerms(query: String): List<String> = words.findAll(
    query.take(256).replace(points, "").replace(quotes, ""),
).take(12).map { it.value }.toList()

/** Only Hebrew tokens can enter the FTS expression. SQLite receives it as a bound parameter. */
fun ftsQuery(terms: List<String>): String = terms.joinToString(" ") { "\"$it\"*" }

fun plainText(markup: String): String = markup.replace(tags, "").trim()

data class PassagePart(val text: String, val bold: Boolean = false)

fun passageParts(markup: String): List<PassagePart> = buildList {
    var start = 0
    for (match in emphasis.findAll(markup)) {
        if (start < match.range.first) add(PassagePart(markup.substring(start, match.range.first)))
        add(PassagePart(match.groupValues[1], bold = true))
        start = match.range.last + 1
    }
    if (start < markup.length) add(PassagePart(markup.substring(start)))
}

fun excerpt(markup: String, terms: List<String>): String {
    val text = plainText(markup)
    val hit = displayWords.findAll(text).firstOrNull { word ->
        searchTerms(word.value).any { token -> terms.any(token::startsWith) }
    }?.range?.first ?: 0
    val start = displayWords.find(text, (hit - 40).coerceAtLeast(0))?.range?.first ?: 0
    val end = (hit + 120).coerceAtMost(text.length)
    val space = if (end == text.length) end else text.lastIndexOf(' ', end).takeIf { it > hit } ?: end
    return (if (start > 0) "…" else "") + text.substring(start, space).trim() +
        (if (space < text.length) "…" else "")
}

fun studyNumber(value: Int, language: Language): String {
    if (language == Language.English || value !in 1..999) return value.toString()
    val ones = " אבגדהוזחט"
    val tens = " יכלמנסעפצ"
    val hundreds = listOf("", "ק", "ר", "ש", "ת", "תק", "תר", "תש", "תת", "תתק")
    val rest = value % 100
    val letters = hundreds[value / 100] + when (rest) {
        15 -> "טו"
        16 -> "טז"
        else -> "${tens[rest / 10]}${ones[rest % 10]}".trim()
    }
    return if (letters.length == 1) "$letters׳" else "${letters.dropLast(1)}״${letters.last()}"
}
