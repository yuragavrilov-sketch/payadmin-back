# pay-admin-back

Бэкенд админ-панели «Платёж». Spring Boot 3.4 / Java 21 / PostgreSQL / Keycloak.

Фронт — [pay-admin-front](../payadmin-front).

## Что внутри

- REST: `GET /api/me`, `GET /api/audit`, CRUD `/api/notes`, CRUD `/api/views`
- Auth: JWT resource server, валидация через Keycloak (issuer-uri)
- БД админки: только операторские данные (audit log, заметки, сохранённые виды). Бизнес-данные (транзакции, мерчанты, споры) живут в core-БД платёжки и читаются через её API.
- Конфиг: локальные `application-{profile}.yml`, значения приходят из env (helm values), секрет БД — через `secretKeyRef` (k8s secret).

## Локальный запуск

### 1. Поднять Postgres + Keycloak

```bash
docker compose up -d
```

- Postgres админки: `localhost:5432`, db/user/pass = `pay_admin`
- Keycloak: http://localhost:8080 (admin / admin)
- Realm `pay-admin` импортируется автоматически из `keycloak/pay-admin-realm.json`

Тестовый пользователь: `operator` / `operator` (роль `operator`).

### 2. Запустить бэк

```bash
mvn spring-boot:run
```

Поднимется на `http://localhost:8081`. Flyway применит миграции из `V1__init.sql`.

### 3. Проверить

```bash
# Токен
TOKEN=$(curl -sS -X POST http://localhost:8080/realms/pay-admin/protocol/openid-connect/token \
  -d "client_id=pay-admin-web" \
  -d "grant_type=password" \
  -d "username=operator" \
  -d "password=operator" | jq -r .access_token)

# Защищённый endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/me
```

## Структура

```
src/main/java/ru/copperside/admin/
├── PayAdminApplication.java
├── config/SecurityConfig.java       JWT resource server + CORS + role mapping
├── common/
│   ├── CurrentOperator.java         резолвер оператора из SecurityContext
│   └── GlobalExceptionHandler.java  404 / 403 → JSON
├── operator/                         Operator + Repository + Service + Controller
├── audit/                            AuditEvent + Repository + Service + Controller
├── note/                             InternalNote + Repository + Controller
└── view/                             SavedView + Repository + Controller

src/main/resources/
├── application.yml                   общие дефолты (jpa, flyway, server, management)
├── application-local.yml             local defaults (DB localhost, Keycloak localhost)
├── application-test.yml              test: datasource + issuer-uri через ${DB_*}, ${KEYCLOAK_ISSUER}
├── application-prod.yml              prod: то же
└── db/migration/V1__init.sql

keycloak/
└── pay-admin-realm.json              авто-импорт realm для dev
```

## Схема БД (`V1__init.sql`)

- `operator` — кэш профиля из Keycloak (sub → uuid)
- `audit_event` — журнал действий, append-only, payload в jsonb
- `internal_note` — операторские заметки к сущностям
- `saved_view` — сохранённые фильтры на страницах списков

## Конфигурация

### Local (env-переменные с дефолтами)

| Переменная | Default | Описание |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/pay_admin` | |
| `DB_USER` | `pay_admin` | |
| `DB_PASSWORD` | `pay_admin` | |
| `KEYCLOAK_ISSUER` | `http://localhost:8080/realms/pay-admin` | issuer-uri для JWT |
| `SERVER_PORT` | `8081` | |
| `DB_SCHEMA` | `pay_admin` | схема БД, создаётся Flyway |

### Test / Prod

Все значения задаются через env в `helm/values-{test,prod}.yaml`:

| Переменная | Откуда |
|---|---|
| `DB_URL`, `DB_USER`, `KEYCLOAK_ISSUER`, `DB_SCHEMA`, `APP_PROF`, `SPRING_PROFILES_ACTIVE` | inline в values.yaml |
| `DB_PASSWORD` | `secretKeyRef: payadmin-db-secret.password` |

Перед первым деплоем в test/prod DevOps должны создать k8s secret
`payadmin-db-secret` с ключом `password` в соответствующем namespace.
