package app.basayin.android

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StudySettings(
    val language: Language = Language.Hebrew,
    val appearance: Appearance = Appearance.System,
    val textSize: Int = 23,
    val israel: Boolean = Locale.getDefault().country == "IL",
)

data class StudyState(
    val settings: StudySettings,
    val history: List<Int>,
    val favourites: List<Int>,
    val store: BookStore? = null,
    val reading: Reading? = null,
    val loadFailed: Boolean = false,
)

class StudyModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("study", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        StudyState(
            settings = StudySettings(
                language = Language.entries.firstOrNull { it.name == preferences.getString("language", null) }
                    ?: Language.Hebrew,
                appearance = Appearance.entries.firstOrNull { it.name == preferences.getString("appearance", null) }
                    ?: Appearance.System,
                textSize = preferences.getInt("text_size", 23).coerceIn(18, 38),
                israel = preferences.getBoolean("israel", Locale.getDefault().country == "IL"),
            ),
            history = readIds("history"),
            favourites = readIds("favourites"),
        ),
    )
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var readingJob: Job? = null

    init { load() }

    fun load() {
        if (loadJob?.isActive == true || _state.value.store != null) return
        _state.update { it.copy(loadFailed = false) }
        loadJob = viewModelScope.launch {
            try {
                val store = withContext(Dispatchers.IO) { BookStore.open(getApplication()) }
                _state.update { it.copy(store = store) }
                refreshReading()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("BasAyin", "Unable to load the bundled edition", error)
                _state.update { it.copy(loadFailed = true) }
            }
        }
    }

    fun refreshReading() {
        val snapshot = _state.value
        val store = snapshot.store ?: return
        readingJob?.cancel()
        readingJob = viewModelScope.launch {
            try {
                val reading = withContext(Dispatchers.IO) {
                    store.currentReading(LocalDate.now(), snapshot.settings.israel)
                }
                _state.update { it.copy(reading = reading) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("BasAyin", "Unable to load the weekly reading", error)
                _state.update { it.copy(reading = null) }
            }
        }
    }

    fun updateSettings(settings: StudySettings) {
        val regionChanged = settings.israel != _state.value.settings.israel
        preferences.edit {
            putString("language", settings.language.name)
            putString("appearance", settings.appearance.name)
            putInt("text_size", settings.textSize)
            putBoolean("israel", settings.israel)
        }
        _state.update { it.copy(settings = settings, reading = if (regionChanged) null else it.reading) }
        if (regionChanged) refreshReading()
    }

    fun rememberPlace(paragraph: Paragraph) {
        val previous = _state.value.history
        if (previous.firstOrNull() == paragraph.id) return
        val history = (listOf(paragraph.id) + previous.filterNot { it / 1000 == paragraph.sectionId }).take(40)
        preferences.edit { putString("history", history.joinToString(",")) }
        _state.update { it.copy(history = history) }
    }

    fun toggleFavourite(id: Int) {
        val previous = _state.value.favourites
        val next = if (id in previous) previous - id else listOf(id) + previous
        preferences.edit { putString("favourites", next.joinToString(",")) }
        _state.update { it.copy(favourites = next) }
    }

    private fun readIds(key: String): List<Int> =
        preferences.getString(key, "").orEmpty().split(',').mapNotNull(String::toIntOrNull)
}
