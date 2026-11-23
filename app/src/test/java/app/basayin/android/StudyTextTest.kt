package app.basayin.android

import java.time.LocalDate
import java.time.ZoneOffset
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
    fun privacyPolicyLinkStaysOnThePublishedHost() {
        assertEquals(
            "https://bas-ayin-android-privacy.glad-horse-8503.chatgpt.site/",
            privacyPolicyUrl,
        )
    }

    @Test
    fun homeShareUsesThisWeeksReadingOrTheAppTitle() {
        assertEquals("בת עין", homeShareText(Language.Hebrew, null))
        assertEquals("Bas Ayin", homeShareText(Language.English, "  "))
        assertEquals("בת עין\nהלימוד השבועי: נשא", homeShareText(Language.Hebrew, "נשא"))
        assertEquals("Bas Ayin\nThis week: Nasso", homeShareText(Language.English, "Nasso"))
    }

    @Test
    fun aboutNamesVersionBuildAndTheBundledTextDate() {
        assertEquals(
            "Version 1.0 (12). Reading calendar 2025–2045. Updated Nov 21, 2026.",
            aboutEditionLine(Language.English, "1.0", 12, "2025–2045", "Nov 21, 2026"),
        )
        assertEquals(
            "גרסה 1.0 (12). לוח הלימוד 2025–2045. עודכן 21 בנוב׳ 2026.",
            aboutEditionLine(Language.Hebrew, "1.0", 12, "2025–2045", "21 בנוב׳ 2026"),
        )
        assertEquals(
            "Nov 21, 2026",
            formatGeneratedDate("2026-11-21T20:16:08Z", Language.English, ZoneOffset.UTC),
        )
        assertTrue(formatGeneratedDate("2026-11-21T20:16:08Z", Language.Hebrew, ZoneOffset.UTC).contains("2026"))
        assertEquals("not-a-date", formatGeneratedDate("not-a-date", Language.English, ZoneOffset.UTC))
    }

    @Test
    fun readingFooterCreditsSefariaAndTheNoncommercialLicence() {
        assertEquals(
            "בת עין, מהדורת ירושלים 2001, מתוך ספריא.\nרישיון CC BY-NC 4.0 · שימוש לא מסחרי בלבד",
            readingFooter(Language.Hebrew),
        )
        assertEquals(
            "Bat Ayin, Jerusalem 2001 edition, from Sefaria.\nLicence CC BY-NC 4.0 · non-commercial use only",
            readingFooter(Language.English),
        )
    }

    @Test
    fun continueReadingExplainsAnEmptyOrMissingPlace() {
        assertEquals("עדיין אין מיקום שמור", resumePlaceholder(Language.Hebrew, savedPlaceMissing = false))
        assertEquals("No saved place yet", resumePlaceholder(Language.English, savedPlaceMissing = false))
        assertEquals(
            "The saved place is not in this edition",
            resumePlaceholder(Language.English, savedPlaceMissing = true),
        )
    }

    @Test
    fun portionShareUsesTheSectionOrTheCombinedWeek() {
        assertEquals("בת עין\nבראשית", portionShareText(Language.Hebrew, "בראשית"))
        assertEquals("Bas Ayin\nBereshit", portionShareText(Language.English, "Bereshit"))
        val bereshit = Section(1, 1, Title("בראשית", "Bereshit"), "parasha", null, null)
        val nasso = Section(47, 4, Title("נשא", "Nasso"), "parasha", null, null)
        val shavuot = Section(45, 4, Title("שבועות", "Shavuot"), "festival", 1, null)
        val week = Reading(
            LocalDate.of(2026, 5, 23),
            Title("נשא · שבועות", "Nasso and Shavuot"),
            listOf(47, 45),
        )
        assertEquals("Bereshit", readerPortionTitle(Language.English, listOf(bereshit), week))
        assertEquals("Nasso and Shavuot", readerPortionTitle(Language.English, listOf(nasso, shavuot), week))
        assertEquals("נשא · שבועות", readerPortionTitle(Language.Hebrew, listOf(nasso, shavuot), null))
        assertEquals("קריאת השבוע", readerPortionTitle(Language.Hebrew, emptyList(), null))
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
