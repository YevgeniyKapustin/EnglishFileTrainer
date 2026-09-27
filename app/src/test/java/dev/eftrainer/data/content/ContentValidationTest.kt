package dev.eftrainer.data.content

import dev.eftrainer.core.audio.AudioAssets
import dev.eftrainer.core.ui.imageAsset
import dev.eftrainer.domain.check.Checker
import dev.eftrainer.domain.check.Normalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class ContentValidationTest {
    private val exercises = Chapters.all.flatMap { c -> c.sections.flatMap { s -> s.tasks.flatMap { it.exercises } } }
    private val sections = Chapters.all.flatMap { it.sections }
    private val tasks = Chapters.all.flatMap { c -> c.sections.flatMap { it.tasks } }

    @Test fun `ids are unique`() {
        val ids =
            Chapters.all.map { it.id } +
                Chapters.all.flatMap { c -> c.sections.map { it.id } } +
                Chapters.all.flatMap { c -> c.sections.flatMap { s -> s.tasks.map { it.id } } } +
                exercises.map { it.id }
        val duplicates =
            ids
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        assertEquals("повторяются id", emptySet<String>(), duplicates)
    }

    @Test fun `every exercise has non-blank answers`() {
        for (e in exercises) {
            val variants =
                when (e) {
                    is Rewrite -> listOf(e.answers)
                    is Speak -> listOf(e.answers)
                    is Talk -> listOf(e.criteria)
                    is Write -> listOf(e.criteria)
                    is Blank -> e.gaps
                    is Order -> listOf(e.answer, e.pictures.map { it.caption })
                }
            assertTrue("${e.id}: пустой список ответов", variants.all { it.isNotEmpty() })
            assertTrue("${e.id}: пустой вариант ответа", variants.flatten().all { it.isNotBlank() })
        }
    }

    @Test fun `every section and task has content`() {
        for (c in Chapters.all) {
            assertTrue("${c.id}: нет разделов", c.sections.isNotEmpty())
            for (s in c.sections) {
                assertTrue("${s.id}: нет заданий", s.tasks.isNotEmpty())
                for (t in s.tasks) assertTrue("${t.id}: нет упражнений", t.exercises.isNotEmpty())
            }
        }
    }

    private fun words(s: String) =
        s
            .lowercase()
            .split(
                Regex("""[\s/]+"""),
            ).map { it.trim('?', '.', '!', ',', '(', ')') }
            .filter { it.isNotEmpty() }
            .sorted()

    private fun slashed(t: Task) =
        t.exercises.filterIsInstance<Rewrite>().filter {
            it.hint == null &&
                " / " in it.prompt
        }

    @Test fun `put in order answers use exactly the prompt words`() {
        for (e in tasks.filter { "order" in it.instruction.lowercase() }.flatMap(::slashed)) {
            for (a in e.answers) assertEquals("${e.id}: «$a»", words(e.prompt), words(a))
        }
    }

    @Test fun `slashed questions carry the question mark among the words`() {
        for (e in tasks.flatMap(::slashed).filter { it.answers.first().endsWith("?") }) {
            assertTrue("${e.id}: нет «?» среди слов", "?" in e.prompt.split(" / ").map { it.trim() })
        }
    }

    @Test fun `dialogue tasks are blanks with unlabelled lines`() {
        for (t in tasks.filter { it.dialogue }) {
            for (e in t.exercises) {
                assertTrue("${e.id}: диалог только из Blank", e is Blank)
                val lines = (e as Blank).text.lines()
                assertTrue("${e.id}: меньше двух реплик", lines.size >= 2)
                assertTrue("${e.id}: метка говорящего", lines.none { Regex("""^\s*[A-Z]:\s""").containsMatchIn(it) })
            }
        }
    }

    @Test fun `task words are non-blank and distinct`() {
        for (t in tasks) {
            assertTrue("${t.id}: пустое слово", t.words.all { it.isNotBlank() })
            assertEquals("${t.id}: повтор слов", t.words.distinct(), t.words)
        }
    }

    @Test fun `no first letter hints`() {
        val firstLetter = Regex("""(?<![\w'’])[b-hj-z](…|\.\.\.|_{2,})""")
        for (e in exercises) {
            val texts =
                listOfNotNull(
                    e.hint,
                    (e as? Blank)?.text,
                    (e as? Rewrite)?.prompt,
                )
            assertTrue("${e.id}: подсказка первой буквой", texts.none { firstLetter.containsMatchIn(it) })
        }
    }

    @Test fun `speak answers start with the text`() {
        for (e in exercises.filterIsInstance<Speak>()) {
            assertEquals("${e.id}: answers[0] ≠ text", e.text, e.answers.first())
        }
    }

    @Test fun `talks have situation, goal, opener and criteria`() {
        for (e in exercises.filterIsInstance<Talk>()) {
            assertTrue("${e.id}: пустой situation", e.situation.isNotBlank())
            assertTrue("${e.id}: пустой goal", e.goal.isNotBlank())
            assertTrue("${e.id}: пустой opener", e.opener.isNotBlank())
            assertTrue("${e.id}: нет criteria", e.criteria.isNotEmpty())
        }
    }

    @Test fun `writes have prompt and criteria`() {
        for (e in exercises.filterIsInstance<Write>()) {
            assertTrue("${e.id}: пустой prompt", e.prompt.isNotBlank())
            assertTrue("${e.id}: нет criteria", e.criteria.isNotEmpty())
            assertTrue("${e.id}: пустой вопрос", e.questions.all { it.isNotBlank() })
            assertTrue("${e.id}: Write должен быть необязательным", e.optional && sections.none { e in it.exercises })
        }
    }

    @Test fun `letter groups pass their own check`() {
        for (e in exercises.filterIsInstance<Speak>().filter { Checker.letterGroup(it.text) != null }) {
            for (a in e.answers) assertTrue("${e.id}: «$a»", Checker.check(e, a).correct)
        }
    }

    @Test fun `task audio exists in assets`() {
        val dir = File("src/main/assets/audio")
        assumeTrue("нет $dir", dir.isDirectory)
        val tracks =
            Chapters.all.flatMap { c -> c.sections.flatMap { s -> s.tasks.mapNotNull { it.audio } } } +
                exercises.filterIsInstance<Speak>().mapNotNull { it.audio } +
                sections.flatMap { it.reference }.filterIsInstance<Reference.Table>().mapNotNull { it.audio }
        for (track in tracks) {
            val files = AudioAssets.candidates(track).map { File(dir.parentFile, it) }
            assertTrue("нет аудио $track", files.any { it.isFile })
        }
    }

    @Test fun `task video exists in assets`() {
        val dir = File("src/main/assets/video")
        assumeTrue("нет $dir", dir.isDirectory)
        val videos = Chapters.all.flatMap { c -> c.sections.flatMap { s -> s.tasks.mapNotNull { it.video } } }
        for (video in videos) {
            val file = File(dir, "$video.mp4")
            assertTrue("нет видео $file", file.isFile)
        }
    }

    @Test fun `exercise images exist in assets`() {
        val dir = File("src/main/assets")
        assumeTrue("нет ${File(dir, "image")}", File(dir, "image").isDirectory)
        val images =
            exercises.mapNotNull { it.image } +
                exercises.filterIsInstance<Order>().flatMap { o -> o.pictures.map { it.image } }
        for (image in images) {
            assertTrue("нет картинки $image", File(dir, imageAsset(image)).isFile)
        }
    }

    @Test fun `clips are well formed`() {
        for (t in Chapters.all.flatMap { c -> c.sections.flatMap { it.tasks } }) {
            val clipped = t.exercises.filter { it.clip != null }
            if (clipped.isEmpty()) continue
            assertTrue("${t.id}: клипы без audio/video", t.audio != null || t.video != null)
            assertEquals("${t.id}: клип не у всех пунктов", t.exercises.map { it.id }, clipped.map { it.id })
            for (e in clipped) {
                val c = e.clip!!
                assertTrue("${e.id}: клип $c", c.startMs >= 0 && c.endMs - c.startMs in 300..60_000)
            }
        }
    }

    @Test fun `correct place answers add exactly the hint words`() {
        for (t in tasks.filter { "correct place" in it.instruction }) {
            for (e in t.exercises.filterIsInstance<Rewrite>().filter { it.hint != null }) {
                val expected = words(e.prompt + " " + e.hint!!)
                assertTrue("${e.id}", e.answers.any { words(it) == expected })
            }
        }
    }

    @Test fun `presets are typed Rewrite or Blank with explain`() {
        for (s in sections) {
            for (e in s.presets) {
                assertTrue(
                    "${e.id}: заготовки только в Grammar/Vocabulary",
                    s.kind == SectionKind.GRAMMAR || s.kind == SectionKind.VOCABULARY,
                )
                assertTrue("${e.id}: только Rewrite/Blank", e is Rewrite || e is Blank)
                assertEquals("${e.id}: source", AnswerSource.MODEL, e.source)
                assertTrue("${e.id}: нет explain", !e.explain.isNullOrBlank())
                assertTrue("${e.id}: попал в обязательные", e !in s.exercises)
            }
        }
    }

    @Test fun `preset answers pass the checker`() {
        for (e in sections.flatMap { it.presets }) {
            when (e) {
                is Rewrite -> assertRewritePasses(e)
                is Blank -> assertBlankPasses(e)
                else -> Unit
            }
        }
    }

    private fun assertRewritePasses(e: Rewrite) {
        for (a in e.answers) assertTrue("${e.id}: «$a»", Checker.check(e, a).correct)
    }

    private fun assertBlankPasses(e: Blank) {
        assertTrue("${e.id}: первые варианты", Checker.check(e, e.gaps.map { it.first() }).correct)
        for ((i, g) in e.gaps.withIndex()) {
            for (v in g) {
                val inputs = e.gaps.mapIndexed { j, o -> if (j == i) v else o.first() }
                assertTrue("${e.id}: «$v»", Checker.check(e, inputs).correct)
            }
        }
    }

    private fun keys(e: Exercise): List<String> =
        when (e) {
            is Rewrite -> {
                listOf(e.answers.first(), e.prompt + " " + (e.hint ?: ""))
            }

            is Blank -> {
                listOf(e.text + " " + (e.hint ?: ""), Checker.check(e, e.gaps.map { it.first() }).expected)
            }

            else -> {
                emptyList()
            }
        }.map(Normalizer::normalize)

    @Test fun `presets do not repeat the book or each other`() {
        for (s in sections) {
            val seen =
                s.exercises
                    .flatMap(::keys)
                    .associateWith { "книга" }
                    .toMutableMap()
            for (e in s.presets) {
                for (k in keys(e).distinct()) {
                    assertTrue("${e.id}: повтор «$k» (${seen[k]})", k !in seen)
                    seen[k] = e.id
                }
            }
        }
    }
}
