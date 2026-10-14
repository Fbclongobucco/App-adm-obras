# app-adm-obras

Application for construction site administration.

## Quick start

1. Copy `.env.example` to `.env` and fill in required values (especially database credentials and Docker Hub credentials if building/pushing images).
2. Ensure an external PostgreSQL instance is reachable (the project does not provision Postgres via docker-compose).
3. Start the application:
   - With Maven: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
   - With Docker Compose (application only): `docker compose up --build`. The host port comes from `APP_PORT` in `.env` (default `8080`), e.g. `APP_PORT=8081` publishes the API on `http://localhost:8081`.

## Environment variables

All configuration is provided via environment variables. See `.env.example` for the complete list, which includes defaults and which variables are required for different profiles.

Key variables:
- `SPRING_PROFILES_ACTIVE`: Application profile (default: local)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Database connection (required for prod profile)
- `APP_CORS_ALLOWED_ORIGINS`: Origins allowed on `/api/auth/**` (login, refresh, logout), with credentials (explicit list; `*` is rejected at startup). All other `/api/**` routes accept any origin without credentials (Bearer token in the header).
- `APP_JWT_SECRET`: Access token signing secret, Base64 with at least 32 bytes (required for prod profile; generate with `openssl rand -base64 32`)
- `APP_SECURITY_JWT_REFRESH_REUSE_GRACE`: Reuse grace window for refresh tokens (default `30s`; `0` disables it). Must be shorter than the access token lifetime. See Authentication.
- `APP_REFRESH_COOKIE_SAME_SITE`: Refresh token cookie `SameSite` policy: `Strict` (default), `Lax` or `None`
- `APP_REFRESH_COOKIE_SECURE`: Refresh token cookie `Secure` flag (default `true`; always `true` in prod)
- `APP_REFRESH_COOKIE_PARTITIONED`: Adds the `Partitioned` attribute to the refresh token cookie (default `false`)
- `APP_BOOTSTRAP_*`: Admin user bootstrap configuration
- `DOCKERHUB_*`: Used by `scripts/docker-build-push.sh` for publishing images

## Authentication

The API is stateless: no HTTP session is created, and HTTP Basic is not accepted.

| Token | Lifetime | Transport |
|-------|----------|-----------|
| Access token (JWT, HS256) | 15 min | `Authorization: Bearer <token>` header |
| Refresh token (opaque, stored as SHA-256 hash) | 7 days from login, not extended by refresh | `refresh_token` cookie: `HttpOnly`, `Path=/api/auth` |

Flow:
1. `POST /api/auth/login` with `{"email": "...", "password": "..."}`. The body returns `accessToken`, `tokenType`, `expiresIn` (seconds) and `user`. The refresh token comes only in the `Set-Cookie` header.
2. Call `/api/**` with `Authorization: Bearer <accessToken>`.
3. When the access token expires (401), call `POST /api/auth/refresh`. It reads the cookie, returns a new access token and replaces the cookie. Each refresh token works once.
4. `POST /api/auth/logout` revokes the refresh token and clears the cookie (204). The access token remains valid until it expires.
5. `GET /api/auth/me` returns the profile of the token owner.

Frontend requirements:
- Keep the access token in memory only.
- Call `/api/auth/*` with `credentials: 'include'` so the browser sends and stores the cookie.
- `refresh` and `logout` reject an `Origin` header that is not listed in `APP_CORS_ALLOWED_ORIGINS` (403 `auth.origin.forbidden`). Requests without `Origin` are accepted: browsers always send it on cross-site POST, so a request without it is not a browser and cannot carry someone else's cookie. Server-side clients (the Next.js frontend, curl) need no `Origin`, so the frontend can be deployed at any address without matching configuration.
- Prefer serializing refresh calls (one in flight, others wait), but concurrent refreshes are tolerated: a refresh token that was already used, presented again within the reuse grace window (`APP_SECURITY_JWT_REFRESH_REUSE_GRACE`, default 30 s after it was used), gets a new token pair for the same session instead of revoking it. This covers two tabs or two server instances refreshing with the same cookie. The window never reopens a session that ended (logout, expiry, inactive user).
- Presenting a used refresh token after the window is treated as theft and revokes the whole session. Setting the window to `0` restores this for every reuse (rollback option).

Sessions end early when:
- the same refresh token is presented twice (the whole chain from that login is revoked);
- the user's password is changed or the user is deactivated (all of the user's refresh tokens are revoked).

### Frontend and API on different domains

"Site" means the registrable domain (e.g. `company.com`); ports and subdomains do not count.

| Topology | Example | Configuration | Third-party cookie blocking |
|----------|---------|---------------|-----------------------------|
| A. Same site | `app.company.com` → `api.company.com`; `localhost:3000` → `localhost:8080` | defaults (`SameSite=Strict`) | Not affected |
| B. Same-origin proxy in Next.js (recommended for different domains) | browser → `app.company.com/api/...` → internal API | defaults (`SameSite=Strict`) | Not affected |
| C. Different sites, no proxy | `app.foo.com` → `api.bar.com` | `APP_REFRESH_COOKIE_SAME_SITE=None` (requires `Secure`), optionally `APP_REFRESH_COOKIE_PARTITIONED=true` | May drop the cookie |

Topology B: forward `/api` keeping the prefix, so `Path=/api/auth` still matches, and list the frontend origin in `APP_CORS_ALLOWED_ORIGINS` (the API still checks `Origin` on refresh/logout):

```js
// next.config.js
module.exports = {
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${process.env.API_URL}/api/:path*` }];
  },
};
```

Topology C: browsers that block third-party cookies (by default or by user choice) may not send the cookie. Refresh then fails and the user has to log in again whenever the access token expires. `Partitioned` (CHIPS) can keep the cookie available where the browser supports it; support varies by browser and version, so do not rely on it. Prefer topology B.

The application refuses to start with an inconsistent configuration: unknown `SameSite` value, `SameSite=None` without `Secure`, or `*` in `APP_CORS_ALLOWED_ORIGINS`.

## Docker build and push

The repository includes a helper script to build and push the application image to Docker Hub.

Prerequisites:
- Docker installed and running
- Valid Docker Hub credentials (preferably an access token)

Usage:
```bash
cp .env.example .env
# Fill in DOCKERHUB_USERNAME, DOCKERHUB_TOKEN, DOCKERHUB_REPOSITORY
./scripts/docker-build-push.sh
```

Behavior:
- Builds using `Dockerfile` in the repository root
- Tags with `latest` and short git SHA if available (or as specified by `DOCKERHUB_TAGS`)
- Authenticates with Docker Hub using `docker login --password-stdin` (credentials never logged)
- Pushes all tags and reports the full image references and digest when available
- Exits with non-zero code on build/auth/push failure with phase-specific error messages

Security notes:
- Never commit `.env`. Use access tokens instead of passwords where possible.
- The script does not echo secrets. Consider running `docker logout` after pushing if on a shared machine.

## Deploy

Checklist for a production release (verified on 2026-10-12 with PostgreSQL 16 and the Docker image):

1. **Backup** the production database. Migration `V8` drops `accommodation.days` and `total_price`; rollback needs the backup.
2. **Build and push** the image: `./scripts/docker-build-push.sh` (or `docker build -t <user>/app-adm-obras:<tag> .` and `docker push`). The build compiles inside the image (`maven:3.9-eclipse-temurin-25`); tests run before, with `./mvnw test`.
3. **Environment** of the container: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` (new, Base64 >= 32 bytes) and `APP_CORS_ALLOWED_ORIGINS` (frontend URL). Optional: `JAVA_OPTS`, `APP_REFRESH_COOKIE_SAME_SITE`.
4. **Start**: Flyway applies `V1`..`V8` (or the missing ones) before the API serves requests; the log shows `Schema "public" is up to date` or `Successfully applied`.
5. **First admin** (empty database only): start once with `APP_BOOTSTRAP_ENABLED=true`, `APP_BOOTSTRAP_ADMIN_EMAIL` and a strong `APP_BOOTSTRAP_ADMIN_PASSWORD`; the log shows `Usuario bootstrap criado`. Log in, then restart with `APP_BOOTSTRAP_ENABLED=false` and without the password.
6. **Frontend together**: the accommodation and meal contracts changed (BREAKING); deploy the matching frontend branch at the same time.
7. **Smoke test**: `POST /api/auth/login`, then `GET /api/projects` and `GET /api/accommodations` with the token. Swagger (`/v3/api-docs`) answers 404 in prod by design.

The container runs as the non-root user `appuser` (uid 1001) and listens on 8080.

## Database schema documentation

The Entity-Relationship Diagram (ERD) is available at the repository root:
- `app-adm-obras-erd.puml` (source, versioned and reviewable)
- `app-adm-obras-erd.pdf` (generated PDF)

To regenerate the diagram:
```bash
./mvnw -Pdiagrams
```

The diagram reflects the current schema as derived from JPA entities. If schema changes occur, regenerate and commit both files together.

### Schema migrations (Flyway)

The database schema is owned by Flyway. Migrations live in `src/main/resources/db/migration` and run on startup, before the application serves requests. Hibernate never creates or alters tables (`spring.jpa.hibernate.ddl-auto=none` in every profile).

- The same migrations run on PostgreSQL (prod) and on H2 in `MODE=PostgreSQL` (local/test).
- Every schema change is a new `V<n>__<description>.sql` file. Never edit a migration that has already been applied.
- Enum columns are `varchar` without a `CHECK` on the allowed values (see the comment in `V1__baseline_schema.sql`); the values are enforced by the Java enums.

An external database previously used with `ddl-auto` already has tables that `V1` tries to create:
- `prod`: the first boot with Flyway fails on an untracked schema. Recreate the database (or drop its schema) before deploying this version.
- `local`: `spring.flyway.baseline-on-migrate=true` marks an existing schema as the baseline instead of failing.

## API documentation

Interactive documentation (Swagger UI) and the OpenAPI 3 description are generated from the code by springdoc-openapi.

| Profile | Available |
|---------|-----------|
| `local` | yes |
| `test`  | yes |
| `prod`  | no — the endpoints do not exist |

With the application running (default port 8080):
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI description (JSON): `http://localhost:8080/v3/api-docs`

Both paths are public where enabled. Calls to `/api/**` made from the UI follow the regular authorization rules:
1. Run `POST /api/auth/login` from the UI and copy `accessToken` from the response.
2. Click **Authorize** and paste the access token (Bearer).
3. Run the request. Without a token, protected routes return 401. The token expires after 15 minutes; log in again to get a new one (refresh is not available from the UI, because its origin is not a frontend origin).

In the `local` and `test` profiles the admin user `admin@admin.com` / `admin123` is created on startup (configurable with `APP_BOOTSTRAP_ADMIN_*`; set `APP_BOOTSTRAP_ENABLED=false` to skip it). The `prod` profile disables the bootstrap, but environment variables override it: see "First admin" in [Deploy](#deploy).

Documentation is disabled by default (`springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled` in `application.properties`); a new profile only exposes it if it enables both properties explicitly.

## API conventions

### Restaurants and meals (BREAKING)

Every restaurant belongs to exactly one project (`/api/restaurants`, `/api/restaurants/by-project/{projectId}`). A meal references a restaurant and inherits its project.

- **BREAKING:** `POST/PUT /api/meals` no longer accept `restaurantName`, `address` or `projectId`. Send `restaurantId` instead. Responses carry `restaurant` (summary) instead of `restaurantName`/`address`.
- A restaurant with meals cannot move to another project or be deleted (`409 restaurant.in_use`).
- `PUT /api/projects/{id}`: `employeeIds` and `accommodationIds` are full replacements (omitted means removed). Meals are not part of the request: they follow their restaurant and are kept.

### Meal plan per project (BREAKING, migration `V4`)

A meal is the food contracted for a project through a restaurant, without a date.

```
MealType = LUNCH | DINNER | LUNCH_AND_DINNER
POST/PUT /api/meals
{ restaurantId, mealType, isBilled,
  lunchQuantity, lunchPrice,      // required when the type serves lunch; ignored otherwise
  dinnerQuantity, dinnerPrice }   // required when the type serves dinner; ignored otherwise
MealSummary
{ id, restaurant, mealType, isBilled,
  lunchQuantity, lunchPrice, dinnerQuantity, dinnerPrice,   // null when not served
  dailyPrice, workingDays, totalPrice,                     // workingDays/totalPrice null without project endDate
  createdAt, updatedAt }
Meal (GET /api/meals, /api/meals/{id}) = MealSummary + { project }
Project: meals: MealSummary[]  (no lunch/dinner)
ProjectRequest: no lunchId/dinnerId
```

- **BREAKING:** `date`, `quantity` and `price` were removed from meals; `lunch`/`dinner` from the project response; `lunchId`/`dinnerId` from the project request.
- Quantity: integer 1 to 10000; price greater than zero, per served meal. Errors (`400`): `meal.invalid.restaurant`, `meal.invalid.type`, `meal.invalid.lunch_quantity`, `meal.invalid.lunch_price`, `meal.invalid.dinner_quantity`, `meal.invalid.dinner_price`. The codes `meal.invalid.price`, `meal.invalid.quantity`, `meal.invalid.date` and `meal.invalid.date.future` no longer exist.
- `dailyPrice` = sum of quantity × price of each served meal (since `V5` it also includes delivery and soda, see below). `workingDays` = Monday to Friday from the project `startDate` to `endDate`, inclusive, holidays not deducted. `totalPrice` = `dailyPrice × workingDays`. Values are computed on every read from the current project dates; nothing is stored.
- A project has at most one meal serving lunch and one serving dinner; `LUNCH_AND_DINNER` takes both. Overlap responds `409 meal.duplicate.type` (also enforced by the database: `uk_meal_project_lunch`, `uk_meal_project_dinner`).
- The project `totalPrice` is the sum of the meals' `totalPrice` (null counts as zero) and the accommodations' totals.

Migration `V4` moves `LUNCH` rows to `lunch_quantity`/`lunch_price` and `DINNER` rows to `dinner_quantity`/`dinner_price`, and drops `date`, `quantity`, `price` and `total_price`. There is no automatic rollback: back up the database before deploying.

### Delivery fee, soda and billing periods (migration `V5`)

`dailyPrice` now includes delivery and soda. With no delivery fee and no soda price, values are unchanged.

```
POST/PUT /api/restaurants   + deliveryFee (number | null)       // per delivery; null = no fee
RestaurantSummary           + deliveryFee
Restaurant (POST/PUT, GET /api/restaurants, /{id}, /by-project/{projectId})
                            + meals: MealSummary[]
POST/PUT /api/meals         + sodaPrice (number | null)         // moved to the restaurant in V6, see below
MealSummary                 + sodaPrice, deliveriesPerDay,
                              mealsDailyPrice, deliveryDailyPrice, sodaDailyPrice,
                              dailyPrice (now includes delivery and soda),
                              projections: [{ period: "DAY"|"WEEK"|"FORTNIGHT"|"MONTH",
                                              workingDays, lunchMeals, dinnerMeals,
                                              deliveries, sodas, price }]   // in this order
```

- Each served meal is one delivery per day: `deliveriesPerDay` = 1 for `LUNCH`/`DINNER`, 2 for `LUNCH_AND_DINNER`.
- `mealsDailyPrice` = sum of quantity × price (the former `dailyPrice`); `deliveryDailyPrice` = `deliveriesPerDay × restaurant.deliveryFee` (0 without fee); `sodaDailyPrice` = `deliveriesPerDay × sodaPrice` (0 without soda); `dailyPrice` = sum of the three. `totalPrice` = `dailyPrice × workingDays`, so meal and project totals include delivery and soda.
- `projections` use fixed working days: `DAY` = 1, `WEEK` = 5, `FORTNIGHT` = 10, `MONTH` = 22 (not the calendar). `lunchMeals`/`dinnerMeals` = daily quantity × days (0 when not served), `deliveries` = `deliveriesPerDay × days`, `sodas` = `deliveries` when there is a soda price (else 0), `price` = `dailyPrice × days`.
- Example: lunch 10 × 25.00 + dinner 8 × 22.00, fee 8.00, soda 5.00 → `mealsDailyPrice` 426.00, `deliveryDailyPrice` 16.00, `sodaDailyPrice` 10.00, `dailyPrice` 452.00; `WEEK` 2260.00, `FORTNIGHT` 4520.00, `MONTH` 9944.00.
- Errors (`400`): `restaurant.invalid.delivery_fee`, `meal.invalid.soda_price` (zero or negative; replaced by `restaurant.invalid.soda_price` in `V6`). Everything is computed on read; nothing is stored. Migration `V5` adds nullable `restaurant.delivery_fee` and `meal.soda_price`.

### Soda moved to the restaurant (BREAKING on meal write, migration `V6`)

The soda price is a condition of the restaurant, like the delivery fee: one unit price, 1 soda per delivery, for every meal of the restaurant.

```
POST/PUT /api/restaurants   + sodaPrice (number | null)   // null = restaurant does not provide soda
RestaurantSummary           + sodaPrice
Restaurant (response)       + sodaPrice
POST/PUT /api/meals         - sodaPrice                   // no longer read; ignored if sent
MealSummary.sodaPrice       = restaurant.sodaPrice        // read only
```

- `sodaDailyPrice` = `deliveriesPerDay × restaurant.sodaPrice` (0 when the restaurant has no soda); `dailyPrice`, `totalPrice` and `projections` keep their names and formulas and read the restaurant's current price on every read.
- Errors (`400`): `restaurant.invalid.soda_price` (zero or negative). The code `meal.invalid.soda_price` no longer exists.
- Migration `V6` adds nullable `restaurant.soda_price`, copies to each restaurant the highest `meal.soda_price` of its meals and drops `meal.soda_price`. A restaurant that had meals with and without soda now provides soda to all of them. Before deploying, list those restaurants on a copy of the database: `select restaurant_id from meal group by restaurant_id having count(soda_price) between 1 and count(*) - 1`. Rollback requires restoring the backup.

### Date adjustments on meals (migration `V7`)

A meal can record exceptions to its working-day schedule, one per date:

- `EXTRA_DAY`: Saturday or Sunday (holidays included) on which the restaurant delivers, with that day's quantities;
- `NO_SERVICE`: a weekday (typically a holiday) on which the restaurant does not deliver.

```
GET    /api/meals/{id}/adjustments            -> MealAdjustment[]            // ordered by date
POST   /api/meals/{id}/adjustments            { date, type, lunchQuantity, dinnerQuantity, note } -> 201
DELETE /api/meals/{id}/adjustments/{adjId}    -> 204                          // ADMIN only, like other writes

MealAdjustment { id, date, weekday, type, lunchQuantity, dinnerQuantity, note,
                 deliveries, sodas, price, inPeriod }
MealSummary + { serviceDays, noServiceDays, extraDays, extraDaysPrice }       // totalPrice includes adjustments
Meal (GET /api/meals, /{id}, /by-project/{projectId}, POST/PUT) + { adjustments }   // ordered by date
```

- Rules, checked in this order: date within the project period, `>= startDate` and `<= endDate` when there is one (`400 meal.adjustment.invalid.date`); one adjustment per date per meal (`409 meal.adjustment.duplicate.date`); `EXTRA_DAY` only on Saturday or Sunday (`400 meal.adjustment.invalid.extra_day`); `NO_SERVICE` only Monday to Friday (`400 meal.adjustment.invalid.no_service`); `EXTRA_DAY` requires the quantity of each meal served by the type (integer 0 to 10000, at least one greater than zero; the quantity of a meal not served is ignored), and `NO_SERVICE` takes no quantities (`400 meal.adjustment.invalid.quantity`). Missing `type`: `400 meal.adjustment.invalid.type`; `note` longer than 100 characters: `400 meal.adjustment.invalid.note`. Unknown adjustment: `404 meal.adjustment.not_found`.
- `weekday` is `MONDAY` … `SUNDAY`.
- Extra day `price` = sum of quantity × price of the meals with quantity > 0 + `deliveries × restaurant.deliveryFee` + `sodas × restaurant.sodaPrice`, where `deliveries` = meals with quantity > 0 (served by the current meal type) and `sodas` = `deliveries` when the restaurant provides soda. `NO_SERVICE` has `deliveries`, `sodas` and `price` 0.
- `noServiceDays` / `extraDays` count the adjustments of each type inside the current project period; `extraDaysPrice` sums those extra days; `serviceDays` = `workingDays − noServiceDays`; `totalPrice` = `dailyPrice × serviceDays + extraDaysPrice` (`serviceDays` and `totalPrice` stay `null` without project `endDate`). The project `totalPrice` follows.
- Adjustments that fall outside the period after the project dates change are kept, with `inPeriod: false`, and do not count.
- `PUT /api/meals/{id}` keeps the adjustments; deleting the meal deletes them.
- Example: project 2026-10-05 to 2026-10-16, `LUNCH_AND_DINNER` lunch 10 × 25.00 and dinner 8 × 22.00, fee 8.00, soda 5.00 (`dailyPrice` 452.00), `NO_SERVICE` on 2026-10-12 and `EXTRA_DAY` on 2026-10-10 with 6 lunches and 0 dinners (163.00: 1 delivery, 1 soda) → `workingDays` 10, `noServiceDays` 1, `serviceDays` 9, `extraDaysPrice` 163.00, `totalPrice` 4231.00.
- Migration `V7` creates `meal_adjustment` (`fk_meal_adjustment_meal ... on delete cascade`, `uk_meal_adjustment_meal_date unique (meal_id, date)`). Existing data is unchanged.

### Optional email (clients and employees)

`email` of clients and employees is optional: `null` or blank is stored as `null`. When informed it must be valid (`400 client.invalid.email` / `employee.invalid.email`) and unique (`409 client.duplicate.email` / `employee.duplicate.email`); records without email never conflict. User email (login) stays required.

Migration `V3` converts existing meals: one restaurant per (project, restaurant name). It fails on purpose if a meal has no project; check with `select count(*) from meal where project_id is null` before migrating a database with data.

### Audit timestamps and sorting

Business resources return `createdAt` and `updatedAt` (UTC, set by the server; values sent by clients are ignored). `GET /api/projects` accepts `sort=<field>[,asc|desc]` with `updatedAt`, `createdAt`, `os` or `startDate`, e.g. `sort=updatedAt,desc&size=10`. Other values respond `400 page.invalid.sort`.

### Weekly meal closing

Meals are settled with the restaurant weekly, Monday to Sunday.

- `GET /api/meals/{id}/weekly-closing?week=YYYY-MM-DD` and `GET /api/restaurants/{id}/weekly-closing?week=YYYY-MM-DD` (the restaurant sums its meals day by day). `week` is any date of the week; missing or outside the list falls back to the current week, the first one (before the start) or the last one (after the end), returned in `selected`.
- `weeks`: Monday-to-Sunday weeks with at least one day in the project period, `start`/`end` cut to the period, `partial` when cut, `current` when it contains today (UTC). Without an end date, the list goes up to the current week.
- `days`: the 7 days with `status` (`SERVICE`, `NO_SERVICE`, `EXTRA_DAY`, `OFF` for a weekend without adjustment, `OUT_OF_PERIOD`), `lunchMeals`, `dinnerMeals`, `deliveries`, `sodas`, `price` and `adjustmentIds`; on a restaurant, `partialNoService` marks a day where only some meals have no service. `totals.serviceDays` counts `SERVICE` and `EXTRA_DAY`. The weeks add up to the meal `totalPrice`.
- `POST /api/restaurants/{id}/adjustments` `{ date, type, note, quantities: [{ mealId, lunchQuantity, dinnerQuantity }] }` applies a date adjustment to every meal of the restaurant (`NO_SERVICE`: all meals; `EXTRA_DAY`: only meals with a quantity above zero, none → `400 meal.adjustment.invalid.quantity`). Same rules and codes as the meal adjustment; any error writes nothing. `201` with the created adjustments.
- `DELETE /api/restaurants/{id}/adjustments/{date}` removes that date from every meal of the restaurant; `404 meal.adjustment.not_found` when none has it. Writes are `ADMIN` only.

### Accommodation lease

Accommodations are leased by month.

- Requests take `startDate` and `monthlyPrice` (> 0) instead of `days` and `totalPrice`. The first period runs from the start date to the day before the same day next month (or the last day of next month when that day does not exist: 2027-01-31 → 2027-02-28).
- Responses bring `startDate`, `monthlyPrice`, `endDate` (end of the last period), `totalPrice` (sum of the periods) and `periods` (`id`, `startDate`, `endDate`, `days`, `price`); the detail also brings `nextRenewal`, the suggested next period.
- `POST /api/accommodations/{id}/renewals` `{ endDate?, price? }` adds a period starting the day after the current end; missing fields use the suggestion. `400 accommodation.invalid.renewal_end` / `accommodation.invalid.renewal_price`. `201` with the accommodation.
- `DELETE /api/accommodations/{id}/renewals/last` undoes the last renewal (`204`); the first period cannot be removed (`409 accommodation.lease.first_period`).
- Without renewals, `PUT` with a new `startDate` or `monthlyPrice` rebuilds the first period; with renewals, changing `startDate` responds `409 accommodation.lease.renewed` and `monthlyPrice` only changes the suggestion.
- Migration `V8` converts existing rows (test data): start = project start (or creation date), monthly price = old total (minimum 0.01), one first period; `days` and `total_price` are dropped. Back up before applying it to a database with real data.

### Search in listings

The paginated listings accept an optional `q`, e.g. `GET /api/employees?q=jose antonio&page=0&size=20`. Matching ignores case and accents (`TRANSLATE`, portable between H2 and PostgreSQL) and looks for the term anywhere in the searchable fields; CPF and CNPJ are compared by digits only, so `q=529.982` finds `52998224725`. Without `q`, or with a blank `q`, the result is unchanged; `totalElements` and the pages reflect the filtered result. Terms over 100 characters respond `400 page.invalid.query`; `%` and `_` are literal.

| Listing | Searchable fields |
|---|---|
| `/api/projects` | `os`, `description`, client name |
| `/api/clients` | `name`, `email`, address city |
| `/api/employees` | `name`, `cpf` (digits), `email` |
| `/api/cost-centers` | `name`, `cnpj` (digits) |
| `/api/restaurants` | `name`, project `os`, address city |
| `/api/meals` | restaurant name, project `os` |
| `/api/accommodations` | `hostName`, address city, project `os` |
| `/api/users` | `name`, `email` |

### Conflicts (409)

Duplicates and deletions blocked by dependents respond `409` with a specific `errorCode`, and nothing is written:
- `project.duplicate.os`, `client.duplicate.email`, `employee.duplicate.email`, `employee.duplicate.cpf`, `user.duplicate.email`, `meal.duplicate.type`, `meal.adjustment.duplicate.date`;
- `cost_center.in_use`, `client.in_use`, `project.in_use`, `employee.in_use`, `restaurant.in_use` (the message names the blocking link);
- `data.integrity_violation` for any other database constraint.

## License

See project metadata.
