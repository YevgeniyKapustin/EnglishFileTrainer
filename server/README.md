# Сервер-ключник для ИИ

Приложение шлёт `POST /v1/chat` с `Authorization: Bearer <app key>`, сервер проверяет ключ, ставит модель
`deepseek-chat`, режет `max_tokens` и проксирует запрос в DeepSeek своим ключом. `GET /healthz` — проверка.

## Локально

В `.env` вписать настоящие ключи, затем:

```sh
cp .env.example .env
set -a; . ./.env; set +a
go run .
go test ./...
```

## Docker

```sh
docker build -t eftrainer-keyserver .
docker run --rm -p 8080:8080 --env-file .env eftrainer-keyserver
```

## Переменные

| Имя | По умолчанию | Что |
|---|---|---|
| `DEEPSEEK_API_KEY` | — (обязательна) | ключ DeepSeek |
| `APP_KEYS` | — (обязательна) | ключи приложения через запятую (ротация) |
| `PORT` | `8080` | порт |
| `RATE_PER_MIN` | `20` | запросов в минуту на ключ приложения |
| `DAILY_TOKEN_LIMIT` | `2000000` | токенов в сутки на весь сервер (UTC), выше — 429 |
| `MAX_BODY_BYTES` | `262144` | потолок тела запроса, выше — 413 |
| `MAX_TOKENS` | `2048` | потолок `max_tokens` |

В приложении: `local.properties` → `ai.serverUrl` (эмулятор — `http://10.0.2.2:8080`, телефон по USB —
`http://localhost:8080` после `adb reverse tcp:8080 tcp:8080`) и `ai.appKey` — один из `APP_KEYS`.
