package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.AnswerSource
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Reference
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.generated.GeneratedItem
import dev.eftrainer.data.generated.Json
import dev.eftrainer.domain.check.Normalizer

/**
 * Свежие упражнения на правило раздела (plans/learning.md, «Свежие упражнения на правило»): ИИ получает
 * правила раздела, образцы из книги с ответами и список пройденных уроков, отвечает строгим JSON.
 * Ответ разбирается и проверяется здесь же: битое и дубли книги отбрасываются молча.
 */
object GeneratePrompt {
    const val MAX_LEN = 160          // потолок длины текста/ответа — длиннее не бывает в WB
    const val MAX_GAPS = 3
    const val SAMPLE_SIZE = 12

    private val SYSTEM = """
        You write practice exercises for a student of the textbook English File Pre-Intermediate (4th edition).
        Write NEW exercises on exactly the same grammar rule or vocabulary as the lesson section given below,
        in the same style and at the same level (A2-B1) as the examples from the book.
        Use only grammar and vocabulary from the lessons the student has studied. Do not copy the examples.
        Every exercise must have exactly one clearly correct answer (list real alternatives, e.g. contractions, in the answers).
        Answer with strict JSON only, no markdown, no comments, in this format:
        {"items":[
          {"type":"blank","instruction":"Complete the questions.","text":"Where {} born?","gaps":[["were you"]],"hint":"(you / be)"},
          {"type":"rewrite","instruction":"Put the words in the right order.","prompt":"live / where / you / do","answers":["Where do you live?"]}
        ]}
        "blank": text with one or more {} gaps, "gaps" has one list of accepted answers per {} (same count).
        "rewrite": the student writes the whole sentence; "answers" are all accepted full sentences.
        "hint" is optional. Keep every text shorter than 120 characters.
    """.trimIndent()

    fun build(
        chapter: Chapter,
        section: Section,
        studied: List<Chapter>,
        book: List<Exercise>,
        avoid: List<Exercise>,
        count: Int,
    ): List<Pair<String, String>> {
        val user = buildString {
            appendLine("Lesson ${chapter.id} \"${chapter.title}\", section: ${section.title}.")
            appendLine("Studied lessons: " + studied.joinToString("; ") { "${it.id} ${it.title}" } + ".")
            if (section.reference.isNotEmpty()) {
                appendLine()
                appendLine("Rules of this section:")
                section.reference.forEach { appendLine(it.asText()) }
            }
            appendLine()
            appendLine("Examples from the book (with answers):")
            sample(book, SAMPLE_SIZE).forEach { appendLine("- " + it.asExample()) }
            if (avoid.isNotEmpty()) {
                appendLine()
                appendLine("Already used, do not repeat:")
                avoid.takeLast(20).forEach { appendLine("- " + it.asExample()) }
            }
            appendLine()
            append("Write $count new exercises.")
        }
        return listOf("system" to SYSTEM, "user" to user)
    }

    /** Равномерная выборка по книге: образцы из всех заданий раздела, а не только первого. */
    fun <T> sample(items: List<T>, n: Int): List<T> =
        if (items.size <= n) items else List(n) { items[it * items.size / n] }

    /**
     * Ответ ИИ → проверенные упражнения (id пустые — их выдаёт репозиторий).
     * Отсев: не тот формат, {} не совпало с gaps, пустые ответы, длинное, дубли книги,
     * уже сгенерированного [existing] и внутри самой пачки. Не JSON — пустой список.
     */
    fun parse(text: String, sectionId: String, book: List<Exercise>, existing: List<Exercise>, max: Int): List<GeneratedItem> {
        val root = try {
            Json.parse(jsonPart(text))
        } catch (e: Json.ParseException) {
            return emptyList()
        }
        val items = (root as? Map<*, *>)?.get("items") as? List<*> ?: (root as? List<*>) ?: return emptyList()
        val seen = (book + existing).flatMap { keys(it) }.toMutableSet()
        val result = mutableListOf<GeneratedItem>()
        for (raw in items) {
            if (result.size >= max) break
            val item = toItem(raw as? Map<*, *> ?: continue, sectionId) ?: continue
            val k = keys(item.exercise)
            if (k.any { it in seen }) continue
            seen += k
            result += item
        }
        return result
    }

    /** Модели любят оборачивать JSON в ```json … ``` или добавлять фразу — берём от первой { до последней }. */
    private fun jsonPart(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0 until end) text.substring(start, end + 1) else text
    }

    private fun toItem(m: Map<*, *>, sectionId: String): GeneratedItem? {
        val instruction = (m["instruction"] as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= 80 }
        val hint = (m["hint"] as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_LEN }
        val exercise: Exercise = when (m["type"]) {
            "blank" -> {
                val text = (m["text"] as? String)?.trim()?.takeIf(::fits) ?: return null
                val gaps = strings2(m["gaps"]) ?: return null
                if (gaps.isEmpty() || gaps.size > MAX_GAPS) return null
                if (text.split("{}").size - 1 != gaps.size) return null
                Blank("", text, gaps, hint = hint, source = AnswerSource.MODEL)
            }
            "rewrite" -> {
                val prompt = (m["prompt"] as? String)?.trim()?.takeIf(::fits) ?: return null
                val answers = strings(m["answers"])?.takeIf { it.isNotEmpty() } ?: return null
                val prefill = (m["prefill"] as? String)?.trim()?.takeIf(::fits)
                // Ответ уже написан в задании — упражнения нет (кроме «исправь ошибку», где фраза может быть верной).
                if (prefill == null && Normalizer.normalize(answers.first()) == Normalizer.normalize(prompt)) return null
                Rewrite("", prompt, answers, prefill = prefill, hint = hint, source = AnswerSource.MODEL)
            }
            else -> return null
        }
        return GeneratedItem(sectionId, instruction, exercise)
    }

    private fun fits(s: String) = s.isNotBlank() && s.length <= MAX_LEN

    /** Непустые строки без повторов; хоть одна битая — null. */
    private fun strings(v: Any?): List<String>? {
        val list = v as? List<*> ?: return null
        val out = list.map { (it as? String)?.trim()?.takeIf(::fits) ?: return null }
        return out.distinct()
    }

    private fun strings2(v: Any?): List<List<String>>? {
        val list = v as? List<*> ?: return null
        return list.map { g -> strings(g)?.takeIf { it.isNotEmpty() } ?: return null }
    }

    /** По чему ловим дубль: текст задания и полный первый ответ, нормализованные. */
    private fun keys(e: Exercise): List<String> = when (e) {
        is Rewrite -> listOfNotNull(e.prompt, e.prefill, e.answers.firstOrNull())
        is Blank -> listOf(e.text, fill(e))
        else -> emptyList()
    }.map(Normalizer::normalize)

    private fun fill(b: Blank): String {
        val parts = b.text.split("{}")
        return parts.mapIndexed { i, p -> p + (b.gaps.getOrNull(i)?.firstOrNull() ?: "") }.joinToString("")
    }

    private fun Exercise.asExample(): String = when (this) {
        is Rewrite -> (prefill?.let { "correct if needed: $it" } ?: prompt) + " -> " + answers.first() +
            (hint?.let { " $it" } ?: "")
        is Blank -> text.replace("{}", "___") + (hint?.let { " $it" } ?: "") + " -> " + fill(this)
        else -> toString()
    }

    // Копия форматирования из ExplainPrompt (тот файл ведёт другой этап).
    private fun Reference.asText(): String = when (this) {
        is Reference.Table -> buildString {
            appendLine("$title:")
            rows.forEach { appendLine("  " + it.filter(String::isNotBlank).joinToString(" | ")) }
        }.trimEnd()
        is Reference.Note -> listOfNotNull(title, text).joinToString(": ")
        is Reference.Mistake -> "Typical mistake: \"$wrong\" -> \"$right\"" + (why?.let { " ($it)" } ?: "")
    }
}
