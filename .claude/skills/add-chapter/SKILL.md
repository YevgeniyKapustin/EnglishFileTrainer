---
name: add-chapter
description: Перенести урок English File Pre-Intermediate 4e (глава вида 1C, 2A) из PDF учебника и рабочей тетради в приложение — tools/content/<ID>.json, аудио/видео в assets, клипы, заготовки, генерация Kotlin, регистрация, сборка. Использовать, когда пользователь просит добавить/перенести главу или урок.
---

# Добавить главу из PDF

Одна глава за вызов, только по явной просьбе пользователя (не тиражировать на несколько глав сразу).
Источник правды — `PLAN.md` (§0 — что открыто) и `plans/`: перед работой прочитать `plans/pipeline.md` (конвейер),
`plans/architecture.md` «Модель контента», `plans/history.md` (Э7.6 «Аудит 1A», Э8.5).
Эталон формата и качества — `tools/content/1A.json` (и `1B.json`). Отвечать пользователю по-русски.

Аргумент: id главы, например `1C`. Ниже `<ID>` = `1C`, `<n>` = номер юнита (`1`), `<L>` = буква (`C`).

## 0. Где что лежит (не в git)

- `english_file/EF4 Pre-Int SB.pdf`, `english_file/EF4 Pre-Int WB.pdf` — сканы без текстового слоя, читать глазами по PNG.
- `english_file/audio/EF4e_PreintSB_<n>.<k>.mp3` — треки SB («🔊 1.9» → `EF4e_PreintSB_1.9.mp3`). Аудио WB нет.
- `english_file/videos/EF4e_Pre-int_PracticalEng_Ep<k>_*.mp4` — Practical English.
- Карта страниц (индекс PDF с 1): SB — PDF = печатная + 1; WB — PDF = печатная − 1.
  Точные страницы урока брать из **содержания** (SB PDF 2–3, WB PDF 2), а не угадывать.
  Grammar Bank юнита n: правила PDF 125+2n, упражнения 126+2n. Ключ WB — PDF 87–94.
  Vocabulary Bank, Sound Bank, Communication, Writing, скрипты Listening — в конце SB, ищи по ссылкам со страниц урока.

Рендер: `uv run --no-project --with pymupdf python tools/render.py sb 11 12 --dpi 150` (ключ — `--dpi 200`),
PNG → `tools/out/`.

## 1. Опись урока — до JSON

Сначала составить список **всех** заданий урока, по источникам, и только потом переносить:
- SB: страницы урока (все упражнения, включая говорение, чтение, аудирование);
- SB: Grammar Bank <n><L> (правила → `reference`, упражнения a/b → задания `gba`, `gbb`);
- SB: Vocabulary Bank / Sound Bank / Communication / Writing — если урок на них ссылается;
- WB: страницы урока + ответы в ключе.
Каждый пункт описи в итоге — либо задание в JSON, либо строка в отчёте «не перенесено, почему».

Где что стоит в книге (проверено на File 1):
- юнит = уроки nA, nB, nC; **Practical English Episode k идёт после nC** (эпизоды после юнитов 1, 3, 5, 7, 9, 11);
- WB «Can you remember…? n» — после nC; SB «Revise and Check» — после каждого второго юнита.
Раздел, который по книге относится к ещё не перенесённой главе, не придумывать заранее — отметить в отчёте.
Черновики таких разделов лежат в `tools/content/drafts/` (например `can-you-remember-1.json` — для конца 1C):
при переносе своей главы забрать оттуда, поменяв id на id этой главы.
Раздел `1A-practical` (PE Episode 1) временно в 1A — при переносе 1C переезжает в конец 1C (id `1A-pe…` сохранить
или мигрировать прогресс — решить с пользователем).

## 2. JSON `tools/content/<ID>.json`

Верхний уровень как в 1A: `id`, `title`, `bubbles`, `sources` (строка с печатными и PDF-страницами), `sections`.

Разделы — по плашкам урока: `<ID>-grammar` (GRAMMAR), `<ID>-vocabulary` (VOCABULARY), `<ID>-pronunciation`
(PRONUNCIATION), `<ID>-skills` (SKILLS), `<ID>-practical` (PRACTICAL_ENGLISH), `<ID>-review` (REVIEW — «Can you remember…?»,
«Revise and Check»; Explain берёт правила всего юнита). Разделы идут по номеру первого своего упражнения в книге.
Например, в 1A порядок такой: 1 Vocabulary → 2 Grammar → 3 Pronunciation → 4 Skills → Practical English.
Если раздел собран из разных номеров, как Skills в 1B (1d–1g, 3c, 4, 5), он идёт после Pronunciation.
У каждого раздела непустой `reference` (таблицы, заметки, типичные ошибки из GB/VB).

Задания:
- id `<ID>-<источник><номер как в книге>`: `sb2a`, `gba`, `wb2a`, `vb1a`, `wr1a` (Writing), `pe2c`. Номера — как в книге.
- `origin`: `SB`, `SB_GRAMMAR_BANK`, `WB`, `GENERATED` (заготовки). `note` — откуда и как переделано.
- id упражнения = `<task id>-<n, 2 цифры>` и **не меняется никогда** (на нём прогресс). `n` — номер пункта в книге.
- Первый пункт WB/GB — напечатанный пример: не переносить.
- `source`: `KEY` — из ключа WB; `CONFIRMED` — из напечатанного скрипта или дословно по аудио;
  `MODEL` — решено моделью (у SB ключа нет). Сомнительное — в отчёт.
- `instruction` — по-английски, как в книге, но под формат ввода.

Ввод только с клавиатуры/голосом — **никаких вариантов на выбор**. Переделка (`plans/pipeline.md`):
- выбрать форму / a–c → `blank`, варианты или исходная форма в `hint`: `Where {} born? (you / be)`;
- слова по порядку → `rewrite`, слова вразброс в `prompt`;
- «right or wrong, correct the mistakes» и «put the word in the right place» → `rewrite`: исходная фраза в `prompt`,
  слово в `hint`, поле пустое, фраза пишется целиком. Заранее вписанного текста в поле (`prefill`) не делать;
- A/B («which is right?») → `rewrite`: обе фразы в `prompt`, ответ — верная фраза целиком (не буква);
- True/False → `rewrite`: утверждение в `prompt`, ответ — утверждение, исправленное так, чтобы стало верным
  (верное — переписать как есть);
- никаких подсказок с первой буквой ответа («b…») в `hint`;
- список слов «from the list / in the box» — один раз в `"words"` задания (перед `exercises`), не в `hint` пунктов;
- диалог из реплик A/B → `"dialogue": true` у задания, `text` пункта — реплики через `
` без меток `A:`/`B:`;
- знаки препинания, нужные в ответе при «слова по порядку», — среди слов `prompt`: `Anna / like music / ?`;
- сопоставить 1–n с a–n → по `blank` на пару;
- фото/картинки книги («match the sentences and the photos») → настоящая картинка, вырезанная из страницы SB/WB PDF
  (шаг 4, «Картинки»), у пункта `blank`/`rewrite` поле `"image": "1b_vb1a_2"`; ответ вписывается. Описывать
  картинку словами в `text` и брать кадры из видео — нельзя;
- расставить картинки или события по порядку («number the pictures 1–6») → одно упражнение `"type": "order"`:
  `"pictures": [{"label": "A", "image": "1a_pe1a_a", "caption": "что на картинке"}, …]`, `"answer": ["B", "D", …]`
  (метки в верном порядке). Это список: перетащить или ↑/↓, потом Check — единственный ввод не с клавиатуры;
  `caption` не показывается (для ИИ и озвучки);
- listen and repeat / скажи вслух → `speak` (группы букв «A K E» — мягкая проверка букв);
- говорение с партнёром, Communication, вопросы «about you» устно → `talk` (situation, goal, opener, criteria;
  разговор по картинкам книги — `pictures` как у `order`, `caption` видит только ИИ);
- письмо своими словами (Writing, «write about…», ответы на вопросы о себе) → `write` (prompt, questions, criteria;
  «listen to the sounds and write» — по пункту на звук с `clip` и скрытым `scene`: что слышно, для ИИ).
`talk`/`write` нужны ИИ: без ключа их не видно, в «освоено» не входят — поэтому весь книжный материал,
который можно проверить офлайн, делать `blank`/`rewrite`/`speak`, а не `write`.

Общий текст (статья, диалог) — в `context` задания; трек — `"audio": "1.19"` у задания или у таблицы правил
(значок 🔊 у таблицы GB/VB); ролик — `"video": "ep2_…"`.

## 3. Заготовки (Grammar и Vocabulary)

~20 на раздел: задания `<раздел>-x1..xN`, `"origin": "GENERATED"`, `source: MODEL`, только `blank`/`rewrite`,
у каждого `explain`. Лексика и грамматика — только пройденные (эта глава и раньше). Разные форматы, как в 1A
(слова по порядку, пропуски, исправь ошибку, напиши вопрос). `generate.py` отсеет дубли с книгой.

## 4. Аудио, видео, клипы, картинки

- Треки: `cp "english_file/audio/EF4e_PreintSB_<n>.<k>.mp3" app/src/main/assets/audio/<n>_<k>.mp3` для каждого
  трека из JSON. Видео — `app/src/main/assets/video/ep<k>_<snake_name>.mp4`. Обе папки не в git.
- Расшифровка и отрезки — `tools/clips.py` (пути от `app/src/main/assets`, кеш в `tools/out/tr/`):
  ```
  uv run --no-project --with faster-whisper python tools/clips.py transcribe audio/1_13.mp3 audio/1_14.mp3
  uv run --no-project --with faster-whisper python tools/clips.py dump audio/1_13.mp3
  uv run --no-project --with faster-whisper python tools/clips.py find audio/1_13.mp3 "He's bald." --after 40
  ```
  `dump` — текст с таймкодами; `find` печатает готовое `"clip": [начало, конец]` (подтянуто к участкам звука,
  запас 0,3 с). В роликах с музыкой — `find --words`. Кеш-JSON содержит `regions` (участки звука): у треков
  «слово — пауза — слово» (repeat, check) отрезки удобнее брать прямо по ним, как сделано в 1B.
- Скриптов многих треков в книге нет → ответ по расшифровке: `CONFIRMED`, если слышно дословно; иначе `MODEL` и в отчёт.
- Клипы (`plans/pipeline.md` шаг 5a): у заданий, где пункт = своя фраза записи (repeat, check, диктовка, You hear / You say),
  каждому пункту `"clip"`. Не включать в отрезок номер пункта, который диктор говорит перед фразой.
  У задания клипы у всех пунктов или ни у одного. На понимание целого текста — без клипов. Одинаковые
  отрезки у пары заданий на один трек (вписать → сказать) — нормально.
- Когда клипы готовы — пережать новые файлы (MP3 → Opus `.ogg`, MP4 → x264 + AAC моно; уже пережатые не трогает):
  `PYTHONIOENCODING=utf-8 uv run --no-project --with imageio-ffmpeg python tools/media.py`

**Картинки** — только со страницы SB/WB PDF, где их печатает книга, даже если задание к видео:
- найти границы: `uv run --no-project --with pymupdf python tools/render.py sb 13 --dpi 96` — в этом PNG
  1 px = 1 px скана (страница SB — 1536×1936), координаты фото снять с него;
- добавить в `CROPS` в `tools/images.py`: `"<id главы строчными>_<задание>_<n>": (страница PDF, (x0, y0, x1, y1))`,
  пример книги (пункт 1) не резать;
- `PYTHONIOENCODING=utf-8 uv run --no-project --with pymupdf --with pillow python tools/images.py <имена>` →
  `app/src/main/assets/image/<name>.webp` (≤ 480 px по ширине, webp q80; папка не в git). Открыть файлы и
  убедиться, что не залезли соседние фото и подписи.

## 5. Генерация и подключение

```
PYTHONIOENCODING=utf-8 uv run --no-project --with mutagen python tools/generate.py <ID>
```
- Ошибки валидатора исправлять в JSON; `File<ID>.kt` руками не править.
- Добавить `File<ID>` в `Chapters.all` (`app/src/main/java/dev/eftrainer/data/content/Chapters.kt`) по порядку книги.
- Сборка и тесты: `./gradlew assembleDebug testDebugUnitTest -q` (ContentValidationTest проверит треки, клипы, картинки, id).
  Упавший тест — смотреть `app/build/test-results/testDebugUnitTest/*.xml` (`<failure message=…>`), править JSON.
  Частое: `rewrite` с `" / "` в `prompt` без `hint` в задании со словом «order» — каждый ответ обязан состоять
  ровно из слов `prompt` (без сокращений `Sam’s` при `Sam / is`); если ответ — вопрос, `?` стоит среди слов.
- Установка на телефон и **короткая** выборочная проверка (адб: `MSYS_NO_PATHCONV=1`, команды через `timeout`);
  полный проход пользователь делает сам.

## 6. Закрыть

- PLAN.md: статус и §0; строка про главу в `plans/history.md` (разделы, число упражнений, что не перенесено, что MODEL/сомнительно).
- Отчёт пользователю по-русски, коротко: разделы и число упражнений; какие ответы не из ключа и сомнительны;
  что не перенесено и почему; что по книге относится к другим главам; нужен ли новый тип/фича.
- Не коммитить без просьбы. PDF, аудио, видео, картинки, рендеры — никогда в git.

## Если делает субагент

Черновик JSON можно отдать одному агенту на главу. Дать ему этот файл, `1A.json` как эталон и запрет трогать
Kotlin/`generate.py`, если параллельно кто-то меняет модель. Подключение (шаги 4–6) — после, одним исполнителем.
