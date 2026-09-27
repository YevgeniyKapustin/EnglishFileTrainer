package dev.eftrainer.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DaySettings(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _chosenChapter = MutableStateFlow(prefs.getString(CHOSEN_CHAPTER, null))
    private val _reviewedOn = MutableStateFlow(prefs.getString(REVIEWED_ON, null)?.let(LocalDate::parse))
    val chosenChapter: StateFlow<String?> = _chosenChapter.asStateFlow()
    val reviewedOn: StateFlow<LocalDate?> = _reviewedOn.asStateFlow()

    fun choose(chapterId: String) {
        prefs.edit { putString(CHOSEN_CHAPTER, chapterId) }
        _chosenChapter.value = chapterId
    }

    fun markReviewed(day: LocalDate) {
        prefs.edit { putString(REVIEWED_ON, day.toString()) }
        _reviewedOn.value = day
    }

    private companion object {
        const val CHOSEN_CHAPTER = "chosen_chapter"
        const val REVIEWED_ON = "reviewed_on"
    }
}
