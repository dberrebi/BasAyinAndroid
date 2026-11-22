package app.basayin.android

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTextTest {
    @Test
    fun searchNormalizesHebrewQuotesAndVowelPoints() {
        assertEquals(listOf("חזל", "תורה"), searchTerms("חז״ל תּוֹרָה"))
        assertEquals(searchTerms("חזל"), searchTerms("חז\"ל"))
        assertEquals("\"חזל\"* \"תורה\"*", ftsQuery(searchTerms("חז״ל | תורה *")))
        assertTrue(searchTerms("NEAR(foo bar) *").isEmpty())
    }

    @Test
    fun passageRetainsEmphasisAndPlainText() {
        val markup = "<b>בראשית</b> ברא <b>אלהים</b>"
        assertEquals("בראשית ברא אלהים", plainText(markup))
        assertEquals(
            listOf(PassagePart("בראשית", true), PassagePart(" ברא "), PassagePart("אלהים", true)),
            passageParts(markup),
        )
        assertTrue(excerpt("${"תחילה ".repeat(50)}<b>חז״ל</b> אמרו", listOf("חזל")).contains("חז״ל אמרו"))
    }

    @Test
    fun hebrewNumeralsUseTraditionalAlternativesForFifteenAndSixteen() {
        assertEquals("א׳", studyNumber(1, Language.Hebrew))
        assertEquals("ט״ו", studyNumber(15, Language.Hebrew))
        assertEquals("ט״ז", studyNumber(16, Language.Hebrew))
        assertEquals("קי״ז", studyNumber(117, Language.Hebrew))
        assertEquals("15", studyNumber(15, Language.English))
    }

    @Test
    fun weekKeepsParashaOrderAndDeduplicatesFestivalDays() {
        val shabbat = LocalDate.of(2026, 5, 23)
        val weekly = Reading(shabbat, Title("נשא", "Nasso"), listOf(47))
        val shavuot = Title("שבועות", "Shavuot")
        val festivals = listOf(Festival("shavuot", shavuot, listOf(46, 45)), Festival("shavuot", shavuot, listOf(46)))
        assertEquals(Reading(shabbat, Title("נשא · שבועות", "Nasso · Shavuot"), listOf(47, 45, 46)),
            combineReading(weekly, festivals))
        assertNull(combineReading(Reading(shabbat, shavuot, emptyList()), emptyList()))
    }
}
