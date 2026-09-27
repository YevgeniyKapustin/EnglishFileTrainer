# История этапов

Что сделано по этапам Э0–Э8.5 и ключевые технические решения. Основной план — `../PLAN.md`.

## Этапы работ

Всё ниже сделано и проверено на телефоне; открытое — в `PLAN.md` §0.

- **Э0 — Окружение** (26.09.2026): Studio, SDK, переменные, git — `plans/setup.md`.
- **Э0.5 — Макеты**: `mockups/english-file-trainer-mockups.html` (сборка `python mockups/build_single.py`),
  исходники `mockups/*.html`. Минимум текста, одно главное действие на экран.
- **Э1 — Скелет**: AGP 9.4.1 (встроенный Kotlin), Kotlin 2.4.20, Gradle 9.8.0, Compose BOM 2026.09.00,
  compileSdk/targetSdk 37, сборка на JDK 25. Package `dev.eftrainer`.
- **Э2–Э4 — Модель, проверка, Room + FSRS**: `data/content/`, `domain/check/`, `data/db/`, `domain/learning/`.
  `Fsrs` сверен с py-fsrs 6 (эталонные числа в `FsrsTest`). Схемы Room в `app/schemas/` (коммитить).
- **Э5 — Экраны**: `ui/`, `Navigation.kt` (типизированные маршруты), `core/theme`, `core/ui`. Шрифты вариативные —
  жирность через ось `wght` (`FontVariation`). После проверки поле readOnly, клавиатура прячется.
- **Э6 — File 1A** через конвейер `plans/pipeline.md` (`tools/render.py`, `tools/content/1A.json`, `tools/generate.py`).
- **Э7 — Подпись**: ключ `C:\Users\Yevgeniy\keys\eftrainer.jks`, пароли в `keystore.properties` (в `.gitignore`,
  копия рядом с ключом). Debug подписан **тем же ключом** — debug и release ставятся друг поверх друга без потери
  прогресса. Значок и заставку генерирует `tools/icon.py`.
- **Э7.5 — Дизайн и настройки**: стиль English File, Settings, тёмная тема, ИИ на любой из 5 провайдеров
  (DeepSeek / OpenAI / Gemini / OpenRouter — один OpenAI-совместимый клиент, Claude — свой формат).
- **Э7.6 — Весь урок 1A**:
  - Типы `Speak` (`SpeechRecognizer` en-US; нет распознавателя — клавиатура), `Talk` (разговор с ИИ по сценарию),
    `Write` (письменный ответ, проверяет ИИ: `WritePrompt` → JSON {pass, summary, corrections}, до 6 исправлений);
    `Task.audio`, `SectionKind.SKILLS`. `Talk`/`Write` — `Exercise.needsAi`: без ИИ не показываются, в «освоено» не входят.
  - Мягкая проверка букв у `Speak` из одиночных букв (`Checker.letterGroup`/`spokenLetters`: bee → B, see → C…).
  - Клипы пунктов: `Exercise.clip` (Media3 `ClippingConfiguration`; аудио-плеер — ExoPlayer).
  - `Reference.Table.audio` — трек у таблицы правил (1A: 1.4, 1.5).
  - ИИ: «Why?» и «Explain» + мини-чат (`data/ai/Ai.kt`, `domain/ai/ExplainPrompt.kt`); свежие упражнения
    (Room v2 `GeneratedExercise`, миграция 1→2).
  - Озвучка без ИИ: `core/speech/Tts.kt` (офлайн, en-US, иначе en-GB), один синтезатор на экран сессии
    (`rememberVoice`, `SayIcon`): реплики ИИ, «Listen» у Speak без трека, «Correct»/правильный ответ, строки
    таблиц VOCABULARY. Настройки — Settings → Voice (`plans/screens.md`, п. 8).
  - Счётчик Review = состав сессии: один источник `ReviewSource` (`ReviewInputs.plan` → `TrainingQueue.review`).
  - Аудит 1A (SB PDF 7–8, GB PDF 127–128, WB PDF 3–4, ключ PDF 87): Vocabulary Bank у 1A нет, Writing нет.
    1A — 209 упр. + 40 заготовок.
- **Э8 — Главы**: порядок работы — скилл `.claude/skills/add-chapter/SKILL.md` (опись урока → JSON → заготовки →
  аудио/клипы → генерация → сборка). Главы — по одной, по просьбе.
  - **1B «The perfect date?»**: grammar «present simple», vocabulary «describing people» (с Vocabulary Bank),
    pronunciation «final -s and -es», skills «Reading, Listening & Writing» — 227 упр. + 42 заготовки.
    Треки 1.11–1.22. Скриптов 1.11–1.18 и 1.22 в книге нет — ответы по расшифровке (CONFIRMED).
  - 1C, 2A, 2B, 2C — четыре агента параллельно (27.09.2026), по главе на агента, потом одна сборка:
    1C «The Remake Project» — 263 упр. + 40, в её конец переехал `1A-practical` (id `1A-pe…` сохранены),
    черновик «Can you remember…? 1» стал `1C-review`; 2A «OMG! Where's my passport?» — 234 + 53;
    2B «That's me in the picture!» — 147 + 45; 2C «One dark October evening» — 215 + 40, с `2C-review`
    (Revise and Check 1&2). Треки 1.23–1.29, 2.1–2.23. Что не влезло в типы — список в `PLAN.md` §0.
  - Доделки типов (27.09.2026): `Order` с текстовыми карточками (2C-gba — одно упражнение `2C-gba-01`, 2C — 209 + 40),
    `Task.images` над заданием (1C sb1b, sb3a), `**жирное**` в `prompt` у `Rewrite` (`core/ui/BoldMarkup.kt`, 2A sb1c).
  - Картинки из WB (27.09.2026): `tools/images.py` режет и WB (`WB_CROPS`) — 2A wb3a, 2B wb2c, 1C wb3c; новые
    задания `1C-wb1c` (предлоги по фото спальни) и `1C-wb3d` (ответы по картинке парка). Аудио WB и видео 2C взять
    неоткуда — `speak` с синтезатором и аудиоверсия 2.21.
  - Типы `Stress` (тап по ударному слогу / сильной форме was/were) и `Sort` (тап по колонке), `TapChecker`:
    2C sb2a, rcpd, wb2a; 2A sb3c; 1C sb2b (+ новое `1C-sb2b2`); 2B wb3a, sb4c. На телефоне не проверено.
- **Э8.5 — Practical English с видео** (эпизод 1 сделан):
  - PE-урок в SB — после юнитов 1, 3, 5, 7, 9, 11 (SB PDF 13, 29, 45, 61, 77, 93). Видео 4e:
    `english_file/videos/EF4e_Pre-int_PracticalEng_Ep{1..6}_*.mp4`, 36 роликов, 124 МБ.
  - `SectionKind.PRACTICAL_ENGLISH`, `Task.video` ("ep1_introduction") → `assets/video/ep1_*.mp4` (в `.gitignore`).
    Плеер `core/video/VideoPlayer.kt` (Media3 ExoPlayer + `PlayerSurface`) и `ui/exercise/inputs/VideoBar.kt`.
    Играет что-то одно: ролик ставит на паузу трек и наоборот. `Task.number` снимает префикс `pe`.
  - Episode 1 «Hotel problems» — раздел `1A-practical` в конце 1A (по книге — после 1C, см. `PLAN.md` §0). 56 упражнений;
    ролевая игра → два `Talk`, You say / повтор → `Speak`. Скриптов PE в SB нет — ответы по расшифровке роликов.
  - Content-validation: каждый `Task.video` есть в assets.
