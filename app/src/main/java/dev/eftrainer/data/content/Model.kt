package dev.eftrainer.data.content

// Модель контента — см. plans/architecture.md.
// Ввод — ТОЛЬКО с клавиатуры: задания книги с выбором/сопоставлением
// конвейер (plans/pipeline.md) переделывает в Rewrite или Blank.

sealed interface Exercise {
    val id: String        // "1A-wb2a-03" — стабилен навсегда: к нему привязан прогресс
    val hint: String?
    val explain: String?  // показываем после неверного ответа
    val source: AnswerSource
    /** Отрезок трека/ролика задания, который относится к этому пункту; null — играет всё целиком. */
    val clip: Clip? get() = null
}

/**
 * Отрезок записи задания ([Task.audio] или [Task.video]) для одного пункта, мс от начала файла.
 * Файл не режем: плеер играет кусок сам (Media3 ClippingConfiguration). Границы посчитаны
 * по пословным таймкодам распознавания (faster-whisper) с запасом ~0,3 с — см. plans/pipeline.md.
 */
data class Clip(val startMs: Int, val endMs: Int) {
    init {
        require(startMs >= 0 && endMs > startMs) { "клип $startMs..$endMs" }
    }
}

enum class AnswerSource {
    KEY,        // ответ из ключа Workbook
    CONFIRMED,  // вычитан вручную
    MODEL,      // решён нейронкой; в UI не выделяется
}

/** Написать предложение целиком. */
data class Rewrite(
    override val id: String,
    val prompt: String,                   // "What … ?" или "girlfriend / student / is / a / your"
    val answers: List<String>,            // все допустимые варианты, первый — показываем при ошибке
    val prefill: String? = null,          // «исправь ошибку»: исходная фраза уже в поле
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.KEY,
    override val clip: Clip? = null,
) : Exercise

/** Один или несколько пропусков `{}` в тексте. */
data class Blank(
    override val id: String,
    val text: String,                     // "Where {} born?"
    val gaps: List<List<String>>,         // по пропуску: допустимые варианты
    override val hint: String? = null,    // "(you / be)"
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.KEY,
    override val clip: Clip? = null,
) : Exercise {
    init {
        require(text.split("{}").size - 1 == gaps.size) { "$id: число {} не совпадает с gaps" }
    }
}

/**
 * Сказать фразу вслух (Pronunciation, «listen and repeat»). Проверка — распознаванием речи;
 * нет распознавателя или микрофона — та же фраза вводится с клавиатуры.
 */
data class Speak(
    override val id: String,
    val text: String,                     // что сказать: "Where do you live?"
    val answers: List<String>,            // допустимые варианты распознанного текста, первый = text
    val audio: String? = null,            // образец: трек Student's Book "1.3" (assets/audio/1.3.mp3)
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.KEY,
    override val clip: Clip? = null,
) : Exercise

/**
 * Разговор с ИИ по сценарию урока (Speaking: «ask and answer with a partner»).
 * Зачёт ставит ИИ. Без ИИ (нет ключа/сети) не показывается и в прогресс главы не входит.
 */
data class Talk(
    override val id: String,
    val situation: String,                // для ИИ: кого он играет и о чём разговор
    val goal: String,                     // задача ученику — видна на экране
    val opener: String,                   // первая реплика ИИ
    val criteria: List<String>,           // что ИИ проверяет для зачёта
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.CONFIRMED,
) : Exercise

/**
 * Письменный ответ своими словами («Answer the questions about you»): поле на несколько строк,
 * проверяет ИИ по [criteria] — зачёт и список исправлений. Как [Talk]: без ИИ не показывается
 * и в «освоено» не входит.
 */
data class Write(
    override val id: String,
    val prompt: String,                   // задача ученику: "Answer the questions about you in full sentences."
    val questions: List<String> = emptyList(),  // на что отвечать (вопросы 2c), по порядку; пусто — свободный текст
    val criteria: List<String>,           // что ИИ проверяет для зачёта
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.CONFIRMED,
) : Exercise

/** Упражнение проверяет только ИИ (разговор, письменный ответ): без ключа и сети его нет. */
val Exercise.needsAi: Boolean get() = this is Talk || this is Write

/** Необязательное упражнение: не считается в «освоено»/«глава пройдена». */
val Exercise.optional: Boolean get() = needsAi

sealed interface Reference {
    /** [audio] — трек Student's Book у таблицы Grammar Bank ("1.4" → assets/audio/1_4.mp3). */
    data class Table(val title: String, val headers: List<String>, val rows: List<List<String>>, val audio: String? = null) : Reference
    data class Note(val title: String?, val text: String) : Reference
    data class Mistake(val wrong: String, val right: String, val why: String?) : Reference
}

data class Chapter(
    val id: String,        // "1A"
    val title: String,
    val sections: List<Section>,
    val bubbles: Bubbles? = null,     // реплики для облачков в шапке главы
)

/** Две реплики из урока — диалог в облачках: вопрос и ответ. */
data class Bubbles(val first: String, val second: String)

enum class SectionKind { GRAMMAR, VOCABULARY, PRONUNCIATION, SKILLS, PRACTICAL_ENGLISH }  // SKILLS — «Listening & Speaking»

/** Раздел урока — плашки G / V / P в учебнике; единица открытия. */
data class Section(
    val id: String,        // "1A-grammar"
    val kind: SectionKind,
    val title: String,     // "word order in questions"
    val reference: List<Reference>,   // правила из Grammar Bank
    val tasks: List<Task>,
)

/**
 * SB — задания урока Student's Book. GENERATED — свежие упражнения на правило: заготовки из JSON
 * ("1A-grammar-x1") и досочинённые ИИ (Room, синтетическое задание "<раздел>-gen").
 */
enum class Origin { WB, SB, SB_GRAMMAR_BANK, GENERATED }

/** Одно задание книги: «2a Complete the questions…». */
data class Task(
    val id: String,        // "1A-wb2a"
    val origin: Origin,
    val instruction: String,
    val exercises: List<Exercise>,    // пункты задания, в порядке книги
    val context: String? = null,      // общий текст, если пункты — его пропуски
    val audio: String? = null,        // Listening: трек Student's Book "1.9" — плеер над заданием
    val video: String? = null,        // Practical English: ролик "ep1_introduction" → assets/video/ep1_introduction.mp4
)

/** Номер задания в своей книге: "1A-sb2a" → "2a", "1A-gbb" → "b", "1A-pe1a" → "1a"; заготовка "1A-grammar-x2" → "2". */
val Task.number: String get() =
    if (origin == Origin.GENERATED) id.substringAfterLast('-').removePrefix("x")
    else id.substringAfter('-').removePrefix("sb").removePrefix("wb").removePrefix("gb").removePrefix("pe")

/** Задание проверяет только ИИ (все пункты — Talk или Write). */
val Task.needsAi: Boolean get() = exercises.all { it.needsAi }

/**
 * Обязательные упражнения раздела (книга) — по ним первый проход, «освоено» и открытие.
 * Упражнения только с ИИ — [aiExercises], свежие заготовки — [presets]: ни те, ни другие сюда не входят.
 */
val Section.exercises: List<Exercise> get() =
    tasks.filter { it.origin != Origin.GENERATED }.flatMap { it.exercises }.filterNot { it.optional }

/** Свежие упражнения-заготовки раздела (вшиты в приложение): в Review после освоения книги, раньше ИИ. */
val Section.presets: List<Exercise> get() = tasks.filter { it.origin == Origin.GENERATED }.flatMap { it.exercises }
/** Упражнения раздела, которые проверяет ИИ (Talk, Write): в первый проход и Review — только при ИИ. */
val Section.aiExercises: List<Exercise> get() = tasks.flatMap { it.exercises }.filter { it.needsAi }
val Chapter.exercises: List<Exercise> get() = sections.flatMap { it.exercises }
