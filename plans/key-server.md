# Сервер-ключник для ИИ

Сделан и запущен 29.09.2026. Основной план — `../PLAN.md`, ИИ в приложении — `ai.md`, запуск — `../server/README.md`.

## Суть

Приложение ходит не к провайдеру, а к нашему тонкому бэку по внутреннему ключу приложения. Бэк проверяет его
и проксирует запрос в DeepSeek своим ключом. Пользователь ничего не вписывает, «AI key» из Settings убран.

## Сервер — сделано

- `server/`: Go, стандартная библиотека + `golang.org/x/time/rate`; Docker — multi-stage → distroless static, non-root.
- `POST /v1/chat` — тело как у OpenAI chat completions (`messages`, `temperature`, `max_tokens`); сервер ставит
  `deepseek-chat`, режет `max_tokens` до `MAX_TOKENS` и отдаёт JSON DeepSeek как есть. `GET /healthz`.
- Env: `DEEPSEEK_API_KEY`, `APP_KEYS` (через запятую, для ротации) обязательны; `PORT`, `RATE_PER_MIN` (на ключ),
  `DAILY_TOKEN_LIMIT` (весь сервер, по `usage.total_tokens`, сброс в полночь UTC), `MAX_BODY_BYTES`, `MAX_TOKENS`.
  В git — только `.env.example`.
- Коды: нет/чужой ключ → 401 (сравнение за постоянное время), лимиты → 429, большое тело → 413, не тот метод → 405,
  таймаут/сеть/ошибка DeepSeek → 502 без текста провайдера. Логи — время, код, задержка, токены; текста нет.
  SIGTERM — мягкая остановка. Тесты — `httptest` с фейковым апстримом (`UPSTREAM_URL`).

## Приложение — сделано

- `ai.serverUrl` и `ai.appKey` из `local.properties` → `BuildConfig`; пусто — ИИ выключен. В debug разрешён http
  только для `localhost` и `10.0.2.2` (телефон: `adb reverse tcp:8080 tcp:8080`).
- `data/ai/Ai.kt`: один OpenAI-совместимый клиент; 401 → «ИИ недоступен», 429 → «лимит», прочее — ошибка сервера.
  `Provider`, определение по ключу, Claude-формат и экран ключа удалены.

## Открыто

- Хостинг: VPS DigitalOcean `209.38.199.119`, `https://ai-teacher.cow-studio.com` (Cloudflare proxy → nginx + certbot → `127.0.0.1:8090`). Бинарник `/opt/ai-teacher/keyserver`, секреты `/opt/ai-teacher/.env`, служба systemd `ai-teacher`. Обновить: собрать `GOOS=linux GOARCH=amd64 CGO_ENABLED=0`, залить, `systemctl restart ai-teacher`.
- Лендинг отдельно: `https://english-file.cow-studio.com`, nginx-сайт `english-file`, файлы `landing/` и APK (`english-file-trainer.apk`) в `/var/www/english-file`. Вход — страница `gate.html` с ключом: он пишется в cookie `ef_key`, nginx сверяет её через `map` (ключ у владельца, в git его нет); плюс `noindex` и `robots.txt`. Обновить: залить файлы через `ssh … "cat > …"`, nginx не трогать.
- Потолок расходов в кабинете DeepSeek.

## Безопасность

Внутренний ключ лежит в APK и достаётся декомпиляцией — он защищает только от случайных запросов. Настоящая
защита — лимиты на сервере и потолок расходов в кабинете DeepSeek. Для релиза другим — вход пользователя и
подписка (`release.md`) вместо общего ключа.
