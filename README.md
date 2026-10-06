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
- `APP_CORS_ALLOWED_ORIGINS`: Allowed CORS origins
- `APP_BOOTSTRAP_*`: Admin user bootstrap configuration
- `DOCKERHUB_*`: Used by `scripts/docker-build-push.sh` for publishing images

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

## License

See project metadata.
