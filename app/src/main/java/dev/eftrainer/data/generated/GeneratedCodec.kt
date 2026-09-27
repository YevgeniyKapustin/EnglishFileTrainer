package dev.eftrainer.data.generated

import dev.eftrainer.data.content.AnswerSource
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.db.entities.GeneratedExercise

data class GeneratedItem(
    val sectionId: String,
    val instruction: String?,
    val exercise: Exercise,
)

object GeneratedIds {
    private const val MARK = "-gen-"
    private const val NUMBER_DIGITS = 4

    fun idFor(
        sectionId: String,
        number: Int,
    ) = "$sectionId$MARK" + number.toString().padStart(NUMBER_DIGITS, '0')

    fun sectionOf(id: String): String? = id.lastIndexOf(MARK).takeIf { it > 0 }?.let { id.substring(0, it) }

    private fun numberOf(id: String): Int? = id.substringAfterLast(MARK, "").toIntOrNull()

    fun next(
        sectionId: String,
        existing: Collection<String>,
        count: Int,
    ): List<String> {
        val max = existing.filter { sectionOf(it) == sectionId }.mapNotNull(::numberOf).maxOrNull() ?: 0
        return (1..count).map { idFor(sectionId, max + it) }
    }
}

object GeneratedCodec {
    const val REWRITE = "rewrite"
    const val BLANK = "blank"

    fun encode(
        item: GeneratedItem,
        createdAt: Long,
    ): GeneratedExercise {
        val e = item.exercise
        val fields =
            linkedMapOf<String, Any?>(
                "instruction" to item.instruction,
                "hint" to e.hint,
                "explain" to e.explain,
            )
        val type =
            when (e) {
                is Rewrite -> {
                    REWRITE.also {
                        fields +=
                            mapOf(
                                "prompt" to e.prompt,
                                "answers" to e.answers,
                                "prefill" to e.prefill,
                            )
                    }
                }

                is Blank -> {
                    BLANK.also { fields += mapOf("text" to e.text, "gaps" to e.gaps) }
                }

                else -> {
                    throw IllegalArgumentException("${e.id}: сгенерировать можно только Rewrite/Blank")
                }
            }
        return GeneratedExercise(e.id, item.sectionId, type, Json.write(fields.filterValues { it != null }), createdAt)
    }

    fun decode(row: GeneratedExercise): GeneratedItem? =
        runCatching {
            val m = Json.parse(row.payload) as Map<*, *>
            val hint = m["hint"] as String?
            val explain = m["explain"] as String?
            val exercise: Exercise? =
                when (row.type) {
                    REWRITE -> {
                        Rewrite(
                            id = row.id,
                            prompt = m["prompt"] as String,
                            answers = (m["answers"] as List<*>).map { it as String },
                            prefill = m["prefill"] as String?,
                            hint = hint,
                            explain = explain,
                            source = AnswerSource.MODEL,
                        )
                    }

                    BLANK -> {
                        Blank(
                            id = row.id,
                            text = m["text"] as String,
                            gaps = (m["gaps"] as List<*>).map { g -> (g as List<*>).map { it as String } },
                            hint = hint,
                            explain = explain,
                            source = AnswerSource.MODEL,
                        )
                    }

                    else -> {
                        null
                    }
                }
            exercise?.let { GeneratedItem(row.sectionId, m["instruction"] as String?, it) }
        }.getOrNull()
}
