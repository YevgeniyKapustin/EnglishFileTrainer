# Архитектура

Слои, дерево, модель контента, проверка ответов, тесты. Основной план — `../PLAN.md`.

## Архитектура

## Архитектура и дерево

Рекомендованная Google архитектура: слои **UI → domain → data**, однонаправленный
поток данных (UDF), внутри слоя — раскладка **по фичам**. По духу это FSD
(слои, зависимости только вниз, слайсы по фичам), но с названиями из мира
Android, чтобы совпадать с документацией.

| Слой | Аналог FSD | Правило |
| --- | --- | --- |
| `ui/` — Compose-экраны + ViewModel | pages / widgets / features-ui | экран рисует `state`, шлёт события во ViewModel |
| `domain/` — чистая логика | features-model / entities-model | **ни Android, ни Compose, ни Room** — только Kotlin, всё под unit-тестами |
| `data/` — Room, DataStore, контент глав | entities-api / shared-api | репозитории отдают `Flow` |
| `core/` — тема, общие composable, даты | shared | ни от кого не зависит |

Зависимости: `ui → domain → data → core`, никогда вверх.

Стек: Compose, ViewModel + StateFlow, Room, DataStore, Navigation Compose
(типобезопасные маршруты), Coroutines, JUnit.
**Не берём:** Hilt (зависимости руками в `AppContainer`), многомодульность,
UseCase-класс на каждое действие.

```
app/src/main/java/<package>/
  App.kt, AppContainer.kt      ручной DI: база, репозитории, контент
  MainActivity.kt              NavHost
  core/
    theme/  ui/  time/         Clock для «сегодня» (подменяется в тестах)
  data/
    content/
      Model.kt                 Chapter, Section, Task, Exercise, Reference
      Chapters.kt              реестр: val all = listOf(File1A, ...)
      File1A.kt ...            данные глав (генерирует конвейер `plans/pipeline.md`)
    db/
      entities/  dao/  AppDatabase.kt
    prefs/                     DataStore: текущая глава
    ProgressRepository.kt  ChapterRepository.kt
  domain/
    check/     Normalizer.kt  Checker.kt
    learning/  Mastery.kt  Fsrs.kt  ChapterCompletion.kt (+ открытие разделов)
               TrainingQueue.kt  Learning.kt (запись ответа → Room → «глава пройдена»)
  ui/
    current/   CurrentChapterScreen + ViewModel   (стартовый)
    section/   SectionScreen + ViewModel
    exercise/  ExerciseScreen + SessionViewModel + inputs/ (Rewrite, Blank, Speak, Talk, Write, плееры)
    chapters/  AllChaptersScreen + ViewModel
app/src/test/java/<package>/   unit-тесты domain + content-validation (JVM)
tools/                         конвейер `plans/pipeline.md` (Python): рендер PDF, JSON, генерация .kt
```

## Модель контента

```kotlin
sealed interface Exercise {
    val id: String        // "1B-wb2a-03" — стабилен навсегда, см. риски
    val hint: String?
    val explain: String?  // показываем после неверного ответа
    val source: AnswerSource
    val clip: Clip? get() = null   // отрезок записи задания для этого пункта (Blank/Rewrite/Speak)
}

// Отрезок Task.audio / Task.video, мс от начала файла. Файл не режем: плеер (Media3 ExoPlayer,
// MediaItem.ClippingConfiguration) играет только кусок; позиция, длина и «−5 с» — внутри него.
// В JSON: "clip": [12.3, 15.8] (секунды). Клипы у задания — у всех его пунктов или ни у одного.
// Без клипов (весь трек) — задания на понимание целого текста/разговора (1.10, PE 1a/1b/2a/3a/3b).
data class Clip(val startMs: Int, val endMs: Int)

enum class AnswerSource {
    KEY,        // ответ из ключа Workbook
    CONFIRMED,  // вычитан вручную
    MODEL,      // решён нейронкой (SB, сгенерированное); в UI не выделяется
}

// Ввод — ТОЛЬКО с клавиатуры: никаких вариантов на выбор, чипов и сопоставлений
// (решено 26.09.2026). Задания книги с выбором/сопоставлением конвейер
// переделывает в один из двух типов ниже — см. `plans/pipeline.md`.

data class Rewrite(                       // написать предложение целиком
    override val id: String,
    val prompt: String,                   // "What … ? (you / do last night)" или
                                          // "girlfriend / student / is / a / your"
    val prefill: String? = null,          // «исправь ошибку»: исходная фраза уже в поле,
                                          // её правишь (или оставляешь, если верна)
    val answers: List<String>,            // все допустимые варианты ответа
    override val hint: String? = null,
    override val explain: String? = null,
) : Exercise

data class Blank(                          // один или несколько пропусков в тексте
    override val id: String,
    val text: String,                      // "A: {} Louisa {} to marry her boyfriend?"
    val gaps: List<List<String>>,          // по пропуску: допустимые варианты
    override val hint: String? = null,     // "(want)" / "(you / be)" / анаграмма "(nynfu)"
    override val explain: String? = null,
) : Exercise

// Э7.6 (см. Model.kt): Speak (сказать вслух; text из отдельных букв "A K E" — мягкая проверка букв),
// Talk (разговор с ИИ) и Write — письменный ответ своими словами, проверяет ИИ:
data class Write(
    override val id: String,
    val prompt: String,                   // задача ученику
    val questions: List<String> = emptyList(),  // на что отвечать, по порядку (вопросы 2c)
    val criteria: List<String>,           // по чему ИИ ставит зачёт
    override val hint: String? = null,
    override val explain: String? = null,
) : Exercise
// Exercise.needsAi = Talk или Write: без ИИ не показываются, в «освоено» не входят (optional);
// Section.aiExercises — такие упражнения раздела, Task.needsAi — задание целиком из них.

sealed interface Reference {
    data class Table(val title: String, val headers: List<String>, val rows: List<List<String>>,
                     val audio: String? = null) : Reference   // трек Grammar Bank у таблицы: "1.4"
    data class Note(val title: String?, val text: String) : Reference
    data class Mistake(val wrong: String, val right: String, val why: String?) : Reference
}

data class Chapter(
    val id: String,        // "1A"
    val title: String,     // "Where are you from?"
    val sections: List<Section>,
)

data class Section(        // раздел урока — плашки G / V / P в учебнике
    val id: String,        // "1A-grammar" — единица открытия (по порядку книги)
    val kind: SectionKind, // GRAMMAR, VOCABULARY, PRONUNCIATION, PRACTICAL_ENGLISH (Э8.5)
    val title: String,     // "word order in questions"
    val reference: List<Reference>,   // правила из Grammar Bank
    val tasks: List<Task>,
)

data class Task(           // одно задание книги: «2a Complete with the present simple…»
    val id: String,        // "1A-wb2a"
    val origin: Origin,    // WB, SB_GRAMMAR_BANK, GENERATED
    val instruction: String,
    val exercises: List<Exercise>,    // пункты задания, в порядке книги
    val context: String? = null,      // общий текст/диалог, если пункты — его пропуски
    val audio: String? = null,        // трек SB "1.9" → assets/audio/1_9.mp3; плеер над заданием
    val video: String? = null,        // Practical English: ролик "ep1_introduction" (Э8.5)
)
```

## Движок проверки ответов

Нормализация ввода перед сравнением: обрезать края, типографский апостроф `\u2019`
заменить на обычный, свернуть двойные пробелы, снять финальную точку / вопросительный
/ восклицательный знак, привести к нижнему регистру. Регистр и финальная пунктуация
не тренируются в этих упражнениях — штрафовать за них нечестно.

- `Rewrite`: нормализованный ввод совпал с любым из нормализованных `answers`.
  Для `prefill` — то же: сравниваем итоговый текст поля.
- `Blank`: каждый пропуск совпал с одним из своих вариантов; верно, только если
  верны все пропуски (неверные подсвечиваем по отдельности).
- При неверном ответе показываем ожидаемый вариант (первый из `answers`) и
  `explain`, если он задан.

## Тесты и верификация

**Unit (JVM, быстрые, гоняются на каждом шаге):**
- нормализатор: апострофы, пробелы, регистр, финальная пунктуация;
- проверка ответа по обоим типам — верно и неверно;
- FSRS: верный ответ растит интервал, ошибка сбрасывает на ~1 день, потолок 180, расчёт `dueOn`;
- **валидация контента** — механическая, ловит ошибки переноса, но не смысла:
  id уникальны по всем главам, `answers` непусты, в `Blank` число `{}` в `text`
  равно числу `gaps`, и ни один вариант не пуст.

**Ручная проверка на телефоне (после Э5 и Э6):**
- по одному упражнению каждого типа (и `Rewrite` с `prefill`): ответить верно и ответить неверно;
- закрыть приложение и открыть — прогресс на месте;
- перевести дату телефона на 2 дня вперёд — упражнение всплыло в «Повторить»
  (самый дешёвый способ проверить повторение живьём);
- `adb logcat` во время прогона — без исключений.
