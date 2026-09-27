package dev.eftrainer.data.generated

import dev.eftrainer.data.content.AnswerSource
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.db.entities.GeneratedExercise

/** Свежее упражнение ИИ: само упражнение + своя короткая инструкция («Put the words in order.»). */
data class GeneratedItem(val sectionId: String, val instruction: String?, val exercise: Exercise)

/** Id свежих упражнений: "<раздел>-gen-0001". Выданный номер не переиспользуется. */
object GeneratedIds {
    private const val MARK = "-gen-"

    fun of(sectionId: String, number: Int) = "$sectionId$MARK" + number.toString().padStart(4, '0')

    /** "1A-grammar-gen-0007" → "1A-grammar"; у книжного id — null. */
    fun sectionOf(id: String): String? = id.lastIndexOf(MARK).takeIf { it > 0 }?.let { id.substring(0, it) }

    private fun numberOf(id: String): Int? = id.substringAfterLast(MARK, "").toIntOrNull()

    /** [count] новых id раздела после самого большого из [existing]. */
    fun next(sectionId: String, existing: Collection<String>, count: Int): List<String> {
        val max = existing.filter { sectionOf(it) == sectionId }.mapNotNull(::numberOf).maxOrNull() ?: 0
        return (1..count).map { of(sectionId, max + it) }
    }
}

/** Строка Room ↔ упражнение. Payload — JSON, чтобы добавлять поля без миграций. */
object GeneratedCodec {
    const val REWRITE = "rewrite"
    const val BLANK = "blank"

    fun encode(item: GeneratedItem, createdAt: Long): GeneratedExercise {
        val e = item.exercise
        val fields = linkedMapOf<String, Any?>("instruction" to item.instruction, "hint" to e.hint, "explain" to e.explain)
        val type = when (e) {
            is Rewrite -> REWRITE.also { fields += mapOf("prompt" to e.prompt, "answers" to e.answers, "prefill" to e.prefill) }
            is Blank -> BLANK.also { fields += mapOf("text" to e.text, "gaps" to e.gaps) }
            else -> throw IllegalArgumentException("${e.id}: сгенерировать можно только Rewrite/Blank")
        }
        return GeneratedExercise(e.id, item.sectionId, type, Json.write(fields.filterValues { it != null }), createdAt)
    }

    /** Битая строка → null (пропускаем, а не падаем). */
    fun decode(row: GeneratedExercise): GeneratedItem? = try {
        @Suppress("UNCHECKED_CAST")
        val m = Json.parse(row.payload) as Map<String, Any?>
        val hint = m["hint"] as String?
        val explain = m["explain"] as String?
        val exercise: Exercise? = when (row.type) {
            REWRITE -> Rewrite(
                id = row.id,
                prompt = m["prompt"] as String,
                answers = (m["answers"] as List<*>).map { it as String },
                prefill = m["prefill"] as String?,
                hint = hint,
                explain = explain,
                source = AnswerSource.MODEL,
            )
            BLANK -> Blank(
                id = row.id,
                text = m["text"] as String,
                gaps = (m["gaps"] as List<*>).map { g -> (g as List<*>).map { it as String } },
                hint = hint,
                explain = explain,
                source = AnswerSource.MODEL,
            )
            else -> null
        }
        exercise?.let { GeneratedItem(row.sectionId, m["instruction"] as String?, it) }
    } catch (e: Exception) {   // ClassCast / ParseException / require в Blank
        null
    }
}
