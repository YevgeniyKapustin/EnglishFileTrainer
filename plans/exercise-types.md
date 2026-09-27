# Виды упражнений

Каталог по тому, что реально есть в приложении: 1A + 1B (+ PE Episode 1), `tools/content/1A.json`, `1B.json`.
Книгу целиком не разбирали (дорого). Вид упражнения определяем по тому, **что делает ученик**. Считаем только
задания книги (79 заданий: SB, GB, WB, PE), заготовки `-x…` учтены отдельно. Основной план — `../PLAN.md`.
Решения PLAN.md: ввод только с клавиатуры, вариантов на выбор нет, UI минимальный.

## Каталог

| # | Вид | Типичная инструкция | Примеры (id) | Заданий / пунктов | Разделы | Как в приложении |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Пропуск, слово без подсказки | Complete with a verb / the missing word | 1A-sb1a, 1A-wb1b, 1B-wb1c (`b…`) | 7 / 56 | V, PE, G (таблица 1B-sb2a) | `Blank` ✓ |
| 2 | Пропуск, форма слова из скобок | Complete … with the present simple form of the verbs in brackets | 1A-wb2a, 1B-wb2a, 1B-sb3b | 4 / 35 | G, P | `Blank` + `hint` `(want)` ✓ |
| 3 | Пропуск, слово из списка | Complete … with an adjective from the list / verbs in the box | 1B-vb2a, 1B-wb2c, 1A-pe3e | 5 / 40 | V, G, PE, P | `Blank`, весь список в `hint` **каждого** пункта — неуклюже |
| 4 | Выбор варианта (A/B, T/F, J/S, 1–3, номер разговора) | Which sentence is correct, A or B? / Mark T or F | 1B-sb2b, 1A-pe3a, 1B-sb4c, 1A-sb4b | 7 / 28 | G, PE, аудирование | `Blank`, варианты в `hint`, вписываешь букву/слово; угадывается |
| 5 | Лишнее слово / другой звук | Which word can't be used…? / has a different sound? Write it | 1A-wb3a, 1B-wb1a, 1B-wb3a | 3 / 14 | V, P, Revise | `Blank`, варианты в `text` ✓ |
| 6 | Анаграмма | Order the letters to make adjectives | 1B-wb1b | 1 / 8 | V | `Blank` + `hint` `(nynfu)` ✓ |
| 7 | Порядок слов | Put the words in order to make questions | 1A-gbb, 1A-wb2b, 1B-gbb | 3 / 25 | G | `Rewrite`, слова через ` / ` в `prompt` ✓ |
| 8 | Вставить слово на место | Put the word in the correct place / Rewrite with the words in brackets | 1A-gba, 1B-wb2b | 2 / 16 | G | `Rewrite`: в 1A через `prefill`, в 1B через `prompt` — разнобой |
| 9 | Построить/преобразовать фразу | Write questions… / Write sentences (+ − ?) | 1A-wb2c, 1B-gba, 1B-wb2d | 3 / 24 | G | `Rewrite` + `hint` `(?)`, `(you / do last night)` ✓ |
| 10 | Исправь ошибки | Correct the questions / The computer has found ten mistakes | 1A-sb2a, 1B-wr1b | 2 / 14 | G, Writing | `Rewrite` + `prefill` ✓ |
| 11 | Сопоставление | Match the sentences and the photos / Write the topic of each paragraph | 1B-vb1a, 1B-wr1c | 2 / 20 | V, Writing | `Blank`; у 1B-vb1a над пунктом фото из книги (`image`) ✓ |
| 12 | Порядок событий/картинок | Number the pictures 1–6 in the order… | 1A-pe1a | 1 / 1 | PE | `Order`: картинки из книги списком, перетащить или ↑/↓, потом Check — единственный ввод не с клавиатуры ✓ |
| 13 | Диктант (записать услышанное) | Write the letters and numbers you hear / Listen and write the words | 1A-sb4a, 1A-sb3a, 1A-sb3b | 3 / 18 | P, аудирование | `Blank` + `clip` ✓ |
| 14 | Понимание на слух / видео | Listen/Watch again and answer the questions | 1A-pe1b, 1B-sb4a, 1B-sb4e | 8 / 30 | аудирование, PE | `Blank` с `audio`/`video`, ответ втиснут в пропуск; много вариантов в `gaps` |
| 15 | Понимание текста | Read the article… Find the information / answer the questions | 1B-sb1d, 1B-wr1a | 2 / 13 | чтение, Writing | `Blank` + `context` ✓ |
| 16 | Повтори вслух | Listen and repeat / Say the words / Test yourself | 1A-sb1c, 1B-vb1c, 1B-sb3b2 | 10 / 73 | P, V, PE | `Speak` (+ `clip`) ✓ |
| 17 | Разговор, ролевая игра, Communication | Ask and answer… / Role-play… / Interview another student | 1A-sb2d, 1A-pe2e, 1B-sb3c | 9 / 9 | G, V, говорение, PE | `Talk`, только с ИИ, optional |
| 18 | Письмо / свой ответ / мнение | Write a paragraph… / Do you think…? Why? / Plan and write your profile | 1A-wb2d, 1B-sb1g, 1B-wr1e | 7 / 7 | G, чтение, Writing | `Write`, только с ИИ, optional |
| — | Неправильные глаголы | (SB p.164) Past simple + Past participle | `iv-go` | 68 глаголов | Review | свой ввод из двух полей ✓ |

Заготовки (`GENERATED`, 14 заданий, 82 пункта) повторяют виды 1, 2, 7, 8, 9, 10 — других видов нет.
Итого по пунктам: вписывание (`Blank`) ≈ 60 %, `Rewrite` ≈ 18 %, `Speak` ≈ 17 %, ИИ (`Talk`/`Write`) — 16 заданий.

## Где слабо

- **Список слов (вид 3)** повторяется в `hint` каждого пункта; в книге он один на задание, слова «вычёркиваются».
- **Выбор (вид 4)** — вписать `A`/`T`/`S` почти ничего не тренирует и угадывается с шансом 1/2–1/3.
  По решению варианты остаются в `hint`; лучше, где можно, спрашивать саму форму/слово, а не букву
  (1B-sb2b: переписать верное предложение `Rewrite`, а не писать `A`).
- **Картинки (виды 11, 12)** — фото вырезаны из страниц SB/WB (`tools/images.py`); на телефоне мелкие фото книги слегка мыльные.
- **Свободный ответ в пропуске (вид 14, 15)** — риск ложного «неверно»: в `gaps` копятся варианты
  (`52`, `52 years old`, `fifty-two`, `he's 52`).
- **Вид 8** оформлен двумя способами — выбрать один (`prefill`).

## Что стоит сделать

1. `Task.wordBox: List<String>` — список слов один раз над заданием (виды 3, 4, 5); использованные слова
   приглушать. Малая правка модели + `BlankInput`, `hint` у пунктов чистится конвейером.
2. Конвейер: выбор A/B и «correct or wrong» → `Rewrite` с верной фразой целиком, T/F → «исправь неверное»
   (как PE 3b «Say why the F sentences are false»). Без нового UI.
3. Для видов 14–15 — мягкая проверка ИИ при неверном ответе (если ключ есть): «по смыслу верно?». Без ИИ — как сейчас.
4. Вид 8 — везде `prefill` с исходной фразой (единообразие, правка JSON 1B-wb2b).

## Ещё не встречалось, но будет (по содержанию SB)

Ударение в слове и фразе (2C, 6B, 9A–9C, 10A: «underline the stressed syllable») — подойдёт `Blank`
с ударным слогом заглавными (`reMEMber`) или `Speak`; linking, weak forms — `Speak`; кроссворды WB — не переносим.
