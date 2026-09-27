# Окружение

Машина, SDK, телефон. Основной план — `../PLAN.md`.

## Состояние машины (проверено 26.09.2026)

- **Android Studio Quail 4 (2026.1.4)** — из zip, без админа:
  `C:\Users\Yevgeniy\Android\android-studio`, ярлык в Пуске. Битый остаток в
  `C:\Program Files\Android\Android Studio` не удалён (нужен админ), не мешает.
- **SDK:** `%LOCALAPPDATA%\Android\Sdk` — Platform 37.2, Build-Tools 37.0.0,
  Platform-Tools, cmdline-tools. Новый `sdkmanager` пишет пакеты через `/`
  (`platforms/android-37.2`), а не через `;`.
- **Переменные (User):** `ANDROID_HOME` = SDK, `JAVA_HOME` = JBR из Studio
  (Java 25.0.3), `platform-tools` в PATH. Если Gradle/AGP не заведётся на 25 —
  откат на Temurin 17 (`jdk-17.0.20.101-hotspot`, установлен).
- **Системного `gradle` нет** — это нормально, проект пойдёт через wrapper `gradlew`.
- **Телефон** TECNO KI5m (Android 12 / API 31) по USB-отладке; adb —
  `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. Эмулятор не нужен.
- **git:** инициализирован, `.gitignore` Android + `*.pdf` (учебники не коммитим).
