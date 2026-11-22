package app.basayin.android

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.json.JSONObject

class BookStore private constructor(private val file: File) {
    private val driver = BundledSQLiteDriver()

    val books: List<Book>
    val sections: List<Section>
    private val paragraphs: Map<Int, List<Paragraph>>
    private val byId: Map<Int, Paragraph>
    val calendarYears: String

    init {
        val catalog = read { db ->
            val books = db.rows("SELECT id, he_name, en_name FROM books ORDER BY sort_order") {
                Book(it.getInt(0), it.title(1))
            }
            val sections = db.rows(
                "SELECT id, book_id, he_name, en_name, kind, moed_order, redirect_id FROM sections ORDER BY sort_order",
            ) {
                Section(it.getInt(0), it.nullableInt(1), it.title(2), it.getText(4), it.nullableInt(5), it.nullableInt(6))
            }
            val passages = db.rows(
                """SELECT id, section_id, paragraph_index, body, cite_book_id, cite_chapter, cite_verse
                    FROM paragraphs ORDER BY section_id, paragraph_index""",
            ) {
                Paragraph(it.getInt(0), it.getInt(1), it.getInt(2), it.getText(3), it.nullableInt(4)?.let { book ->
                    Citation(book, it.getInt(5), it.getInt(6))
                })
            }
            val metadata = db.rows("SELECT key, value FROM meta") { it.getText(0) to it.getText(1) }.toMap()
            check(books.size == 5 && sections.size == 74 && passages.size == 2805) { "Incomplete book database" }
            db.rows("SELECT paragraph_id FROM paragraphs_fts WHERE paragraphs_fts MATCH 'תורה' LIMIT 1") {
                it.getInt(0)
            }.also { check(it.isNotEmpty()) { "Missing text search index" } }
            Catalog(books, sections, passages, "${metadata.getValue("year_start")}–${metadata.getValue("year_end")}")
        }
        books = catalog.books
        sections = catalog.sections
        paragraphs = catalog.passages.groupBy(Paragraph::sectionId)
        byId = catalog.passages.associateBy(Paragraph::id)
        calendarYears = catalog.calendarYears
    }

    fun section(id: Int): Section? = sections.firstOrNull { it.id == id }
    fun paragraph(id: Int): Paragraph? = byId[id]
    fun passages(id: Int): List<Paragraph> = paragraphs[id].orEmpty()
    fun bookSections(id: Int): List<Section> = sections.filter { it.bookId == id && it.kind != "appendix" }
    fun festivals(): List<Section> = sections.filter { it.festivalOrder != null }.sortedBy { it.festivalOrder }
    fun appendix(): List<Section> = sections.filter { it.kind == "appendix" }

    fun search(raw: String): List<SearchHit> {
        val terms = searchTerms(raw)
        if (terms.isEmpty()) return emptyList()
        return read { db ->
            db.prepare(
                "SELECT paragraph_id FROM paragraphs_fts WHERE paragraphs_fts MATCH ? ORDER BY rank, paragraph_id LIMIT 60",
            ).use { statement ->
                statement.bindText(1, ftsQuery(terms))
                buildList {
                    while (statement.step()) {
                        paragraph(statement.getInt(0))?.let { add(SearchHit(it, excerpt(it.body, terms))) }
                    }
                }
            }
        }
    }

    fun currentReading(today: LocalDate, israel: Boolean): Reading? = read { db ->
        val shabbat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        val region = if (israel) 1 else 0
        val weekly = db.prepare("SELECT title_he, title_en, section_ids FROM weekly WHERE israel = ? AND gregorian_date = ?")
            .use {
                it.bindInt(1, region)
                it.bindText(2, shabbat.toString())
                if (it.step()) Reading(shabbat, it.title(0), ids(it.getText(2))) else null
            } ?: return@read null
        val festivals = db.prepare(
            """SELECT label, title_he, title_en, section_ids FROM festival_days
                WHERE israel = ? AND gregorian_date BETWEEN ? AND ? ORDER BY gregorian_date, label""",
        ).use {
            it.bindInt(1, region)
            it.bindText(2, today.toString())
            it.bindText(3, shabbat.toString())
            buildList {
                while (it.step()) add(Festival(it.getText(0), it.title(1), ids(it.getText(3))))
            }
        }
        combineReading(weekly, festivals)
    }

    private inline fun <T> read(block: (SQLiteConnection) -> T): T =
        driver.open(file.absolutePath, SQLITE_OPEN_READONLY).use(block)

    private data class Catalog(
        val books: List<Book>, val sections: List<Section>, val passages: List<Paragraph>, val calendarYears: String,
    )

    companion object {
        fun open(context: Context): BookStore {
            val assets = context.assets
            val manifest = assets.open("manifest.json").bufferedReader().use { JSONObject(it.readText()) }
            val expected = manifest.getString("sha256")
            check(expected.matches(Regex("[0-9a-f]{64}"))) { "Invalid book checksum" }
            val directory = File(context.noBackupFilesDir, "edition").apply { mkdirs() }
            val database = File(directory, "bas-ayin-$expected.sqlite")
            if (!database.isFile || database.inputStream().use { it.sha256() } != expected) {
                val temp = File.createTempFile("bas-ayin-", ".tmp", directory)
                try {
                    assets.open("bas_ayin.sqlite").use { input -> temp.outputStream().use { input.copyTo(it) } }
                    check(temp.inputStream().use { it.sha256() } == expected) { "Book checksum mismatch" }
                    Files.move(temp.toPath(), database.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } finally {
                    temp.delete()
                }
            }
            return BookStore(database)
        }
    }
}

private fun SQLiteStatement.title(column: Int) = Title(getText(column), getText(column + 1))
private fun SQLiteStatement.nullableInt(column: Int) = if (isNull(column)) null else getInt(column)
private fun ids(value: String) = value.split(',').mapNotNull(String::toIntOrNull)

private inline fun <T> SQLiteConnection.rows(sql: String, read: (SQLiteStatement) -> T): List<T> =
    prepare(sql).use { statement -> buildList { while (statement.step()) add(read(statement)) } }

private fun InputStream.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val count = read(buffer)
        if (count == -1) break
        digest.update(buffer, 0, count)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
