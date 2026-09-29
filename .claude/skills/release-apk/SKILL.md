---
name: release-apk
description: Build and deliver a signed English File Trainer release APK to the connected TECNO phone, the gated landing site, or both when the user asks to install or publish the app.
---

# Выпуск APK

Сборка Kotlin и тесты не означают, что актуальный APK собран, установлен или опубликован. Выполняй только запрошенные места доставки; запрос «на сайт и телефон» разрешает обе доставки. Источники адресов и ключевых правил: [PLAN.md](../../../PLAN.md), [plans/key-server.md](../../../plans/key-server.md), [plans/setup.md](../../../plans/setup.md).

1. Проверь `git status`, целевой commit, последний `v*` tag, `adb devices -l` и установленную версию `dev.eftrainer`. Убедись, что `keystore.properties` существует, а `ai.serverUrl` и `ai.appKey` в `local.properties` непустые. Значения ключей, токенов и паролей не печатай.
2. Версия задаётся в `app/build.gradle.kts`: `versionCode` — число коммитов в `HEAD`, `versionName` — `0.<число v-тегов>.<versionCode>`. Для нового выпуска зафиксируй согласованные изменения коммитом и создай следующий локальный тег `vN` до сборки. Не собирай выпуск из незакоммиченного дерева или с прежним номером версии. Не добавляй секреты, PDF и медиа в коммит.
3. Выполни нужные проверки из `PLAN.md`, затем `./gradlew :app:assembleRelease`. Убедись по `app/build/outputs/apk/release/output-metadata.json`, что `app-release.apk` — подписанный release с ожидаемой версией. Зафиксируй локальные размер и SHA-256 именно этого файла.
4. Если запрошен телефон: при одном подключённом устройстве выполни `./gradlew :app:installRelease`. Проверь `adb shell dumpsys package dev.eftrainer`: версия должна совпасть с APK. Открой приложение и убедись, что оно запускается с сохранёнными данными; при ошибке установки не удаляй приложение ради обхода ошибки.
5. Если запрошен сайт: `/var/www/english-file/english-file-trainer.apk` на `209.38.199.119` обслуживает `https://english-file.cow-studio.com/english-file-trainer.apk`. Загрузи APK под временным именем в тот же каталог, сверь на сервере размер и SHA-256 с локальными, затем атомарно замени итоговый файл. nginx и gate не меняй. Повторно сверь итоговый файл; при доступе к gate проверь скачивание по HTTPS без вывода ключа или cookie. Если доступа к gate нет, честно укажи, что HTTPS-проверка не состоялась.
6. В отчёте назови версию, commit/tag, результат сборки и проверок, версию на телефоне, URL сайта и совпадение хешей. Старый APK в `app/build/outputs` и успешный `compileDebugKotlin` не считай доказательством доставки.

Остановись при отсутствии подписи, неоднозначном выборе устройства, несовпадении хешей или невозможности проверить версию. Не удаляй данные приложения и не подменяй опубликованный APK непроверенным файлом.
