package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.AnswerSource
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.generated.GeneratedItem
import dev.eftrainer.data.generated.Json
import dev.eftrainer.domain.check.Normalizer
import dev.eftrainer.domain.check.filled

object GeneratedResponse {
    private const val MAX_INSTRUCTION_LEN = 80
    private const val GAP = "{}"

    fun parse(
        text: String,
        sectionId: String,
        book: List<Exercise>,
        existing: List<Exercise>,
        max: Int,
    ): List<GeneratedItem> {
        val root = runCatching { Json.parse(jsonPart(text)) }.getOrNull()
        val items = (root as? Map<*, *>)?.get("items") as? List<*> ?: root as? List<*> ?: return emptyList()
        val seen = (book + existing).flatMap { keys(it) }.toMutableSet()
        return items
            .asSequence()
            .mapNotNull { raw -> (raw as? Map<*, *>)?.let { toItem(it, sectionId) } }
            .filter { seen.claimAll(keys(it.exercise)) }
            .take(max)
            .toList()
    }

    private fun MutableSet<String>.claimAll(keys: List<String>): Boolean {
        val fresh = keys.none { it in this }
        if (fresh) addAll(keys)
        return fresh
    }

    private fun jsonPart(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0 until end) text.substring(start, end + 1) else text
    }

    private fun toItem(
        m: Map<*, *>,
        sectionId: String,
    ): GeneratedItem? {
        val instruction =
            (m["instruction"] as? String)?.trim()?.takeIf {
                it.isNotEmpty() &&
                    it.length <= MAX_INSTRUCTION_LEN
            }
        val hint = (m["hint"] as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= GeneratePrompt.MAX_LEN }
        val exercise =
            when (m["type"]) {
                "blank" -> toBlank(m, hint)
                "rewrite" -> toRewrite(m, hint)
                else -> null
            }
        return exercise?.let { GeneratedItem(sectionId, instruction, it) }
    }

    private fun toBlank(
        m: Map<*, *>,
        hint: String?,
    ): Blank? {
        val text = (m["text"] as? String)?.trim()?.takeIf(::fits)
        val gaps = strings2(m["gaps"]).orEmpty()
        val valid = text != null && gaps.size in 1..GeneratePrompt.MAX_GAPS && text.split(GAP).size - 1 == gaps.size
        return if (valid) Blank("", text, gaps, hint = hint, source = AnswerSource.MODEL) else null
    }

    private fun toRewrite(
        m: Map<*, *>,
        hint: String?,
    ): Rewrite? {
        val prompt = (m["prompt"] as? String)?.trim()?.takeIf(::fits)
        val answers = strings(m["answers"]).orEmpty()
        if (prompt == null || answers.isEmpty()) return null
        val unchanged = Normalizer.normalize(answers.first()) == Normalizer.normalize(prompt)
        return if (unchanged) null else Rewrite("", prompt, answers, hint = hint, source = AnswerSource.MODEL)
    }

    private fun fits(s: String) = s.isNotBlank() && s.length <= GeneratePrompt.MAX_LEN

    private fun strings(v: Any?): List<String>? {
        val list = v as? List<*> ?: return null
        val out = list.map { (it as? String)?.trim()?.takeIf(::fits) ?: return null }
        return out.distinct()
    }

    private fun strings2(v: Any?): List<List<String>>? {
        val list = v as? List<*> ?: return null
        return list.map { g -> strings(g)?.takeIf { it.isNotEmpty() } ?: return null }
    }

    private fun keys(e: Exercise): List<String> =
        when (e) {
            is Rewrite -> listOfNotNull(e.prompt, e.answers.firstOrNull())
            is Blank -> listOf(e.text, e.filled())
            else -> emptyList()
        }.map(Normalizer::normalize)
}
