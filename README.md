# app-adm-obras

Application for construction site administration.

## Quick start

1. Copy `.env.example` to `.env` and fill in required values (especially database credentials and Docker Hub credentials if building/pushing images).
2. Ensure an external PostgreSQL instance is reachable (the project does not provision Postgres via docker-compose).
3. Start the application:
   - With Maven: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
   - With Docker Compose (application only): `docker compose up --build`

## Environment variables

All configuration is provided via environment variables. See `.env.example` for the complete list, which includes defaults and which variables are required for different profiles.

Key variables:
- `SPRING_PROFILES_ACTIVE`: Application profile (default: local)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Database connection (required for prod profile)
- `APP_CORS_ALLOWED_ORIGINS`: Allowed CORS origins (explicit list; `*` is rejected at startup)
- `APP_JWT_SECRET`: Access token signing secret, Base64 with at least 32 bytes (required for prod profile; generate with `openssl rand -base64 32`)
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
- `refresh` and `logout` require an `Origin` header listed in `APP_CORS_ALLOWED_ORIGINS`; otherwise they return 403. Browsers send it automatically; other clients (curl, scripts) must send it explicitly.
- Serialize refresh calls: only one refresh in flight at a time, other requests wait for it. Presenting a refresh token that was already used is treated as theft and revokes the whole session, so two tabs refreshing with the same cookie log the user out.

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

In the `local` and `test` profiles the admin user `admin@admin.com` / `admin123` is created on startup (configurable with `APP_BOOTSTRAP_ADMIN_*`; set `APP_BOOTSTRAP_ENABLED=false` to skip it). It is never created in `prod`.

Documentation is disabled by default (`springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled` in `application.properties`); a new profile only exposes it if it enables both properties explicitly.

## API conventions

### Restaurants and meals (BREAKING)

Every restaurant belongs to exactly one project (`/api/restaurants`, `/api/restaurants/by-project/{projectId}`). A meal references a restaurant and inherits its project.

- **BREAKING:** `POST/PUT /api/meals` no longer accept `restaurantName`, `address` or `projectId`. Send `restaurantId` instead. Responses carry `restaurant` (summary) instead of `restaurantName`/`address`.
- A project keeps at most one meal of each type; a second one responds `409 meal.duplicate.type`.
- A restaurant with meals cannot move to another project or be deleted (`409 restaurant.in_use`).
- `PUT /api/projects/{id}`: `employeeIds` and `accommodationIds` are full replacements (omitted means removed). `lunchId`/`dinnerId` only confirm a meal that already belongs to the project; they never move meals, and omitting them keeps the current meals.

Migration `V3` converts existing meals: one restaurant per (project, restaurant name). It fails on purpose if a meal has no project; check with `select count(*) from meal where project_id is null` before migrating a database with data.

### Audit timestamps and sorting

Business resources return `createdAt` and `updatedAt` (UTC, set by the server; values sent by clients are ignored). `GET /api/projects` accepts `sort=<field>[,asc|desc]` with `updatedAt`, `createdAt`, `os` or `startDate`, e.g. `sort=updatedAt,desc&size=10`. Other values respond `400 page.invalid.sort`.

### Conflicts (409)

Duplicates and deletions blocked by dependents respond `409` with a specific `errorCode`, and nothing is written:
- `project.duplicate.os`, `client.duplicate.email`, `employee.duplicate.email`, `employee.duplicate.cpf`, `user.duplicate.email`, `meal.duplicate.type`;
- `cost_center.in_use`, `client.in_use`, `project.in_use`, `employee.in_use`, `restaurant.in_use` (the message names the blocking link);
- `data.integrity_violation` for any other database constraint.

## License

See project metadata.
