package app.basayin.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookStoreTest {
    private val store by lazy { BookStore.open(InstrumentationRegistry.getInstrumentation().targetContext) }

    @Test
    fun packagedEditionHasItsOwnSectionStructure() {
        assertEquals(5, store.books.size)
        assertEquals(74, store.sections.size)
        assertEquals(2805, store.sections.sumOf { store.passages(it.id).size })
        assertEquals(17, store.festivals().size)
        assertEquals(2, store.appendix().size)
        assertEquals(25, store.section(20)?.redirectId)
        assertEquals(Citation(1, 1, 1), store.paragraph(1001)?.citation)
    }

    @Test
    fun searchUsesThePackagedFtsIndex() {
        val plain = store.search("חזל")
        assertTrue(plain.isNotEmpty())
        assertEquals(plain, store.search("חז״ל"))
        assertEquals(plain, store.search("חַזָ״ל"))
        assertTrue(store.search("123 OR *").isEmpty())
        assertNotNull(store.search("בראשית ברא").firstOrNull()?.paragraph)
    }

    @Test
    fun weeklyReadingFollowsShabbatAndTheSelectedRegion() {
        val ordinary = LocalDate.of(2026, 11, 21)
        assertEquals(listOf(7), store.currentReading(ordinary.minusDays(2), false)?.sectionIds)
        assertEquals(listOf(7), store.currentReading(ordinary, true)?.sectionIds)
        val shavuot = LocalDate.of(2026, 5, 23)
        assertEquals(listOf(47), store.currentReading(shavuot, true)?.sectionIds)
        assertEquals(listOf(46), store.currentReading(shavuot, false)?.sectionIds)
    }
}
