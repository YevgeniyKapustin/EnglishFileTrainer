package dev.eftrainer.data.settings

import android.content.Context
import androidx.core.content.edit
import dev.eftrainer.data.generated.Json
import dev.eftrainer.domain.learning.DayRun
import java.time.LocalDate

class DayRuns(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("day_run", Context.MODE_PRIVATE)

    fun load(): DayRun? = prefs.getString(RUN, null)?.let(DayRunCodec::decode)

    fun save(run: DayRun) = prefs.edit { putString(RUN, DayRunCodec.encode(run)) }

    fun clear() = prefs.edit { remove(RUN) }

    private companion object {
        const val RUN = "run"
    }
}

object DayRunCodec {
    private const val KEY = "key"
    private const val DATE = "date"
    private const val ORDER = "order"
    private const val TALLY = "tally"
    private const val ARRIVALS = "arrivals"

    fun encode(run: DayRun): String =
        Json.write(
            mapOf(
                KEY to run.key,
                DATE to run.date.toString(),
                ORDER to run.order,
                TALLY to TaskRunCodec.encode(run.run),
                ARRIVALS to run.arrivals,
            ),
        )

    fun decode(text: String): DayRun? {
        val fields = runCatching { Json.parse(text) }.getOrNull() as? Map<*, *> ?: return null
        val key = fields[KEY] as? String ?: return null
        val date = (fields[DATE] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val order = (fields[ORDER] as? List<*>).orEmpty().map { it.toString() }
        val tally = (fields[TALLY] as? String)?.let(TaskRunCodec::decode) ?: return null
        val arrivals = (fields[ARRIVALS] as? Number)?.toInt() ?: 0
        return DayRun(key, date, order, tally, arrivals)
    }
}
