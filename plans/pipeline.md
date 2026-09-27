# Конвейер контента

PDF → JSON → Kotlin, аудио, клипы. Порядок работы — скилл `.claude/skills/add-chapter`. Основной план — `../PLAN.md`.

## Конвейер контента

Источник — PDF-сканы в `english_file/` (не в git): `EF4 Pre-Int WB.pdf` и
`EF4 Pre-Int SB.pdf` — **4-е издание**. Аудио SB 4e в `english_file/audio/`:
`EF4e_PreintSB_<юнит>.<дорожка>.mp3`, 280 дорожек, юниты 1–12 без пропусков
(юнит 1 — с 1.2), 162 МБ. Имя = номер из учебника («🔊 1.9» → `..._1.9.mp3`),
сопоставление не нужно. Аудио Workbook нет, и скриптов к нему в WB нет (есть только ключ, PDF 87–94):
«Listen and check / repeat» WB → `speak` по словам/фразам задания, образец читает синтезатор.
Видео Practical English в `english_file/videos/` (`EF4e_*`) — **4-е издание**,
подходит (`plans/history.md`, Э8.5). Других роликов нет (Video Listening 2C — только его аудиоверсия 2.21/2.22).
Текстового слоя нет, распознаёт vision-модель (Claude) по картинкам страниц.

**Карта страниц (индекс PDF, с 1):**
- WB: PDF = печатная − 1. Уроки nA/nB/nC: 1 → 3/5/7, 2 → 11/13/15, …
  (полная таблица — в разведке 26.09, легко пересобрать по содержанию на PDF 2).
  **Ключ ответов: PDF 87–94** — все уроки 1A–12C, по упражнениям; пункт 1
  в упражнениях — пример, в ключе ответы начинаются со 2. Ключ читать при ~200 dpi.
- SB: PDF = печатная + 1. Grammar Bank: PDF 127–150, юнит n — правила на
  125+2n, упражнения на 126+2n. **Ключа к SB нет нигде.**

**Шаги на главу:**
1. Скрипт рендерит страницы урока WB (+ страницу Grammar Bank, если её задали)
   и нужный кусок ключа в PNG.
2. Модель выдаёт JSON по схеме `plans/architecture.md`: упражнения, `Reference` из правил GB,
   ответы. Первый пункт каждого упражнения (напечатанный пример) отбрасываем.
   Ответ из ключа → `KEY`, решённый моделью (SB, упражнения без ключа) → `MODEL`.
   **Выбор превращаем во ввод** (в приложении нет вариантов на выбор):
   - «circle the correct form» / выбрать a–c → `Blank`, подсказка в скобках —
     исходная форма слов: `Where {} born? (you / be)`;
   - «put the words in order» → `Rewrite`, слова вразброс в `prompt`, ответ —
     фраза целиком;
   - «right or wrong? correct the mistakes» → `Rewrite`: фраза в `prompt`, поле пустое, пишешь целиком;
     A/B → обе фразы в `prompt` строками, ответ — верная; T/F → ответ — утверждение, исправленное на верное;
   - список слов на задание → `"words"` у задания; диалог A/B → `"dialogue": true`, реплики через `
` без меток;
   - «match 1–n with a–n» → по `Blank` на пару: `{} a film` → `watch`;
   - «match the sentences and the photos» → `Blank`/`Rewrite` с `"image"` у пункта (фото из книги);
   - «number the pictures / events in order» → `"type": "order"`: `pictures` `[{label, image, caption}]`
     и `answer` — метки в верном порядке. Порядок фраз — `[{label, text}]`.
   - картинки книги над заданием → `"images": ["1c_sb_painting", …]`; «correct the information in bold» →
     `rewrite`, неверное в `prompt` — `**жирным**`.
   - «underline the stressed syllable» → `"type": "stress"`: `text` `"a|cross"`, `stressed` [2] (с 1);
     фраза с was/were → `"It [was] great. [Was] it?"`, `stressed` — номера сильных среди `[…]`, может быть [];
   - «write the words in the chart» (звуки, -ed, ударение) → `"type": "sort"`: `columns` у задания,
     у пункта `word` (`ask[ed]` — розовые буквы, `hu|rry` — слоги) и `column` (с 1).
     Order, Stress, Sort — единственный ввод не с клавиатуры.
3. Content-validation (`plans/architecture.md`, «Тесты») на JSON.
4. Сверка: скан рядом с распознанным, правлю ошибки. Ответы SB, которые
   учитель поправил на уроке, правлю здесь же → `CONFIRMED`.
5. Из JSON генерируется `FileNX.kt` — руками Kotlin не пишем.
5a. **Клипы пунктов** (задания с `audio`/`video`, где пункт = своя фраза записи: listen and repeat,
   listen and check, диктовка букв/слов, реплики «You hear»/«You say»/Social English, отдельные
   отрывки 1.9). Пословные таймкоды — faster-whisper (`uv run --no-project --with faster-whisper`,
   модель `small.en`, `word_timestamps=True`) по каждому треку/ролику задания; текст/ответ пункта
   выравниваем по транскрипту, запас ~0,3 с до и после (не залезая в соседнюю фразу), пишем
   `"clip": [начало, конец]` в секундах. Слышать нужно всю запись — задание на понимание целого
   (какой это разговор, T/F по сюжету, порядок картинок) — клипов нет. Таблицы правил (`Reference`) —
   всегда весь трек. `generate.py` проверяет клип (0 ≤ начало < конец, тип пункта, есть запись,
   внутри файла — с `--with mutagen`), ContentValidationTest — форму и «у всех пунктов или ни у одного».
   Инструмент — `tools/clips.py` (transcribe / dump / find; кеш расшифровок и участков звука в `tools/out/tr/`).
6. Свежие-заготовки для разделов Grammar и Vocabulary (~20 на раздел): модель сочиняет по правилам
   раздела и заданиям книги как образцу, только лексика/грамматика пройденного; задания
   `"origin": "GENERATED"`, id `"<раздел>-x<N>"`, `source: MODEL`, у каждого `explain`.
   `generate.py` отсеивает дубли с книгой и между собой.

5b. **Картинки** — только со страницы SB/WB PDF, где их печатает книга (не кадры видео). `tools/images.py`:
   запись в `CROPS` — `"<глава>_<задание>_<n>": (страница PDF, (x0, y0, x1, y1))` в пикселях скана 96 dpi
   (страница SB — один скан 1536×1936). Режет с отступом 3 px, ужимает до 480 px по ширине, webp q80 →
   `assets/image/<name>.webp` (папка в `.gitignore`, как аудио). Пример книги (пункт 1) не режем.
   `generate.py` проверяет имя и наличие файла, ContentValidationTest — что файл есть.

**Переносим весь урок:** SB (включая говорение, чтение, аудирование), Grammar/Vocabulary Bank, WB.
Не переносим только кроссворды и то, что без картинок книги не имеет смысла (в отчёте главы).

**Порядок:** главы открываются в приложении по одной, по мере прогресса, поэтому
конвейер гоняем по одной главе вперёд, не всю книгу разом.

## Аудио SB

**Аудио SB** — `english_file/audio/EF4e_PreintSB_<u>.<n>.mp3`, 280 шт.; вшиты в APK пережатыми (`assets/audio/<u>_<n>.ogg`,
Opus моно 32 кбит/с; папка в `.gitignore`). Видео — x264 CRF 28 + AAC моно 64 кбит/с. Пережимает `tools/media.py`
(ffmpeg из пакета `imageio-ffmpeg`, ставить ничего не надо). Content-validation: у каждого трека задания/таблицы есть файл.

## Скрипты `tools/`

Запуск из корня, `PYTHONIOENCODING=utf-8`. Комментариев и docstring в коде нет — всё «как и почему» здесь.
- `render.py` — шаг 1: `uv run --no-project --with pymupdf python tools/render.py wb 3 4 --dpi 150` (ключ — `--dpi 200`).
  Номера — индекс страницы PDF с 1; PNG в `tools/out/` (не в git: сканы книги).
- `generate.py` — шаг 5: `uv run --no-project --with mutagen python tools/generate.py 1A` (без mutagen длина файла
  для клипов не проверяется — только предупреждение). Правка — в JSON, Kotlin не трогаем. У `speak` `answers` можно
  не писать (= `[text]`), иначе `answers[0] == text`. У `rewrite` `prompt` обязателен;
  `prefill` и подсказки первой буквой (`b…`) — ошибка. Дубль заготовки ищется по нормализованному (как `Normalizer.kt`) тексту+подсказке
  и полному первому ответу; шаблон вида «What … ?» без подсказки — не ключ.
- `clips.py` — шаг 5a: `uv run --no-project --with faster-whisper python tools/clips.py transcribe|dump|find …`,
  пути от `app/src/main/assets`. `find` подтягивает границы к участкам звука (whisper после паузы ставит начало на
  1–2 с раньше), запас 0,3 с, но не дальше середины паузы до соседнего звука. Под музыкой карта громкости
  ненадёжна → `--words`. Если `find` промахнулся (цифры: whisper «07700», в книге «oh seven…») — искать глазами
  в `dump`. Результат прослушать: выравнивание — подсказка.
- `images.py` — шаг 5b: `uv run --no-project --with pymupdf --with pillow python tools/images.py [имена]`
  (без имён — все); границы искать по рендеру `render.py sb|wb … --dpi 96`. SB — `CROPS` (скан 96 dpi, 1 px = 1 px
  скана), WB — `WB_CROPS` (границы тоже по рендеру 96 dpi, режется с 200 dpi — скан WB ~205 dpi).
- `icon.py` — `uv run --no-project --with pymupdf python tools/icon.py`: пишет `drawable/ic_launcher_foreground.xml`,
  `drawable/splash_icon.xml` и превью `tools/out/icon.png`. Форму маски выбирает лаунчер (у TECNO — скруглённый
  квадрат), поэтому фон значка прозрачный, а белый круг видимой зоны (72 из 108) и веер по нему — в foreground.
  Заставка Android 12+: тот же веер 288dp с кругом 192dp (пропорция 108/72), лучи стартуют повёрнутыми к
  горизонтали и раскрываются вверх за 600 мс. pymupdf не умеет `clipPath` — превью маскируется толстой обводкой.
- Проверки: `ruff format tools`, `ruff check`, `mypy` (strict, настройки в `pyproject.toml`),
  `python tools/check_no_comments.py` (комментарии и docstring → exit 1).
