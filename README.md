# pay-admin-back

Бэкенд админ-панели «Платёж». Spring Boot 3.4 / Java 21 / PostgreSQL / Keycloak.

Фронт — [pay-admin-front](../payadmin-front).

## Что внутри

- REST: `GET /api/me`, `GET /api/audit`, CRUD `/api/notes`, CRUD `/api/views`
- Auth: JWT resource server, валидация через Keycloak (issuer-uri)
- БД админки: только операторские данные (audit log, заметки, сохранённые виды). Бизнес-данные (транзакции, мерчанты, споры) живут в core-БД платёжки и читаются через её API.
- Конфиг: Spring Cloud Config (`spring-cloud-starter-config`), секреты из Vault через config-server по пути `pay/app/payadmin/{profile}`. Локально конфиг-сервер выключен (`bootstrap-local.yml`).

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
├── bootstrap.yml                     config-server URIs (prod/test/pcidss)
├── bootstrap-local.yml               config-server disabled для local
├── application.yml                   общие дефолты (jpa, flyway, server, management)
├── application-local.yml             local defaults (DB localhost, Keycloak localhost)
├── application-test.yml              пусто, вся конфигурация из Vault
├── application-prod.yml              пусто, вся конфигурация из Vault
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

В helm values задаются только маркеры окружения: `APP_PROF`, `SPRING_PROFILES_ACTIVE`, `DB_SCHEMA`.

Вся остальная конфигурация — из Vault через config-server по пути
`pay/app/payadmin/{profile}`. Ключи должны быть в формате **spring-свойств**
(не env-style), чтобы корректно перебивать значения из `common/{profile}`
в том же config-server (common содержит Oracle-дефолты JDBC, которые нужно
переопределить для Postgres).

#### Что положить в Vault

**`pay/app/payadmin/test`:**

```properties
spring.datasource.url=jdbc:postgresql://payadmin-db.test.svc.cluster.local:5432/pay_admin
spring.datasource.username=pay_admin
spring.datasource.password=<реальный пароль>
spring.datasource.driver-class-name=org.postgresql.Driver
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak.prod.transcapital.com/realms/svcmgr
```

**`pay/app/payadmin/prod`:**

```properties
spring.datasource.url=jdbc:postgresql://payadmin-db.pay-service.svc.cluster.local:5432/pay_admin
spring.datasource.username=pay_admin
spring.datasource.password=<реальный пароль>
spring.datasource.driver-class-name=org.postgresql.Driver
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak.prod.transcapital.com/realms/svcmgr
```

> URL-ы для БД и Keycloak пока взяты как предположение — уточнить у DevOps
> реальные DNS-имена k8s-сервисов и realm Keycloak перед заливкой.

Если `common/{profile}` содержит ещё какие-то JDBC/Hibernate ключи, которые
ломают Postgres-сервис (`spring.jpa.database-platform`, `hibernate.dialect`
и т.п.) — посмотреть ответ config-server и добавить их в `pay/app/payadmin/{profile}`:
```bash
curl http://j-srv-config:8080/payadmin/test/main | jq
```
