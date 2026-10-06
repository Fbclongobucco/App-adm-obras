## Context

See `proposal.md` for motivation. The relevant constraints:

- A `Dockerfile` is planned by `infra-docker-compose-e-der` at the repository root. The build process defined here uses that `Dockerfile` as input; this change does not modify its contents.
- No external CI/CD system is assumed. The behavior is defined as a local/executable process (script or documented command) that can be run in a controlled environment with Docker available.
- Credentials must never be logged. The process runs in environments where logs may be captured.
- Docker Hub is the target registry (hub.docker.com). Authentication uses standard `docker login` semantics via environment variables.

## Goals / Non-Goals

**Goals:**
- Define a clear, scriptable process to build and push the application image to Docker Hub using environment variables for configuration.
- Ensure tagging is deterministic and reports the full pushed reference(s) and digest on success.
- Enforce fail-fast behavior with phase-specific error messages and no credential leakage.
- Keep the process independent of application code changes.

**Non-Goals:**
- Creating or modifying the `Dockerfile` contents (owned by the other change).
- Implementing a full CI pipeline (GitHub Actions, etc.) — only the behavior/process definition is needed.
- Building multi-platform images by default (single-platform is sufficient unless specified otherwise).
- Managing registry credentials beyond environment variables and documented inputs.

## Decisions

### D1. Use a shell script as the executable entry point

**Choice:** Provide a `scripts/docker-build-push.sh` script (executable) that performs build → tag → login → push in order, with clear phase messages and exit codes. The script reads configuration from environment variables.

**Rationale:**
- Explicit, reviewable, and runnable locally without introducing a new build tool dependency beyond Docker.
- Easy to enforce credential scrubbing (never `echo` secrets).
- Maps cleanly to the "phases" required by the spec (build failure, push failure, auth failure).
- Can be invoked directly in any environment with Docker CLI available.

**Alternatives considered:**
- **Makefile target** (`make docker-push`): also scriptable, but shell script is more explicit for error handling and secret hygiene.
- **Maven plugin**: couples infrastructure process to Java build tool unnecessarily and makes secret handling less obvious.
- **Docker Buildx bake**: powerful but adds abstraction; shell script is sufficient for the stated requirements.

### D2. Configuration via environment variables only

**Choice:** All inputs come from environment variables:
- `DOCKERHUB_USERNAME` (required)
- `DOCKERHUB_TOKEN` or `DOCKERHUB_PASSWORD` (required; prefer token)
- `DOCKERHUB_REPOSITORY` (required) — e.g. `username/app-adm-obras` or just image name depending on convention; full repo path
- `DOCKERHUB_TAGS` (optional) — space/comma-separated list of tags; if omitted, derive default tag
- `DOCKER_IMAGE_NAME` (optional) — fallback for repository naming
- `BUILD_CONTEXT` (optional, default `.`)
- `DOCKERFILE_PATH` (optional, default `Dockerfile`)

**Rationale:**
- Matches 12-factor/CI-friendly approach; no interactive prompts.
- Easy to document in `.env.example` and validate early.
- Prevents accidental logging of values in command lines when handled properly.

**Alternatives considered:**
- **Command-line flags**: also viable, but env vars are standard for registry credentials in CI and local `.env` files.
- **Config file**: introduces another artifact and risks committing sensitive defaults.

**Default tag derivation:** If `DOCKERHUB_TAGS` is not set, use `latest` and also include `$(git rev-parse --short HEAD)` when git is available (deterministic, traceable). If git is unavailable, fall back to `latest` only. This satisfies "default tag" requirement while being deterministic.

### D3. Fail-fast with phase-specific errors

**Choice:** Validate required env vars first (fail with "missing credentials/config" before docker operations). Check for `Dockerfile` existence. Run `docker build`, capture result; on failure exit with message indicating build phase. After build, `docker login` using non-echoing credential passing (`--password-stdin` or equivalent), failing with auth error if unsuccessful. Tag all requested tags, then `docker push` each tag; on any push failure, report push phase. Only report success after all requested pushes complete.

**Rationale:**
- Matches spec scenarios exactly (build failure, auth failure, push failure).
- Prevents partial success reporting.
- Uses `docker login --password-stdin` to avoid credentials in process args (prevents leakage via `ps`).

**Credential handling:** Read token/password from stdin for login. Never print the token. Redirect docker command output where appropriate, but preserve error messages. Strip any lines that might contain secrets before echoing.

### D4. Report full references and digest on success

**Choice:** After successful pushes, collect the full reference for each tag (e.g., `docker.io/username/repo:tag`) and attempt to retrieve the image digest (via `docker inspect --format='{{index .RepoDigests 0}}'` or `docker manifest inspect`/registry API as available). Include both in structured output on success.

**Rationale:**
- Satisfies "full image reference(s)" and "digest when available" requirements.
- Provides pullable reference for downstream consumers.

**Note:** Digest retrieval depends on registry behavior; if not available immediately after push in the local daemon, report references and note digest unavailable rather than failing.

### D5. No application code changes; script and docs only

**Choice:** The change introduces only:
- `scripts/docker-build-push.sh` (executable)
- Documentation updates (`README.md` or new section) describing required env vars and usage
- Updates to `.env.example` to include Docker Hub variables (non-sensitive placeholders)
- No modifications to application source, `pom.xml` (beyond any already in other change), or runtime behavior

**Rationale:**
- Respects planning boundary and scope; purely infrastructure behavior.
- Avoids coupling build/push to application runtime.

## Risks / Trade-offs

**[Credentials in environment]** → Risk of exposure if env is dumped. Mitigation: never log env values; script validates presence but never echoes secrets; prefer tokens over passwords.

**[docker login state persists on host]** → Running login modifies local docker config. In CI/local ephemeral environments this is fine; in shared environments, credentials may remain cached. Document recommendation to logout after push (`docker logout`) in script or docs.

**[Partial push on multi-tag failure]** → If pushing multiple tags and later tag fails, earlier tags may already be pushed. This is a real partial state; the script reports push phase failure and exits non-zero, but does not attempt to roll back registry state (not feasible generically). This is an acceptable trade-off; users should inspect which tags succeeded if failure occurs.

**[Digest not always retrievable]** → Depending on Docker version/registry, `RepoDigests` may be empty locally immediately after push. Reporting references plus digest when available matches "digest when available" requirement without failing the process.

**[Git availability]** → Default tag derivation depends on git; fallback to `latest` only prevents hard dependency. Deterministic behavior is preserved when git is available.

**[No multi-platform build]** → Single-platform build by default is simpler and matches stated scope. Multi-platform would require buildx setup; can be added later if needed.

**[Script assumes docker CLI is logged in context]** → Script performs `docker login` explicitly; does not rely on existing credentials except as overridden. This ensures deterministic auth behavior.

## Migration Plan

1. **Create script**: Add `scripts/docker-build-push.sh` with the behavior above, make executable.
2. **Update `.env.example`**: Add Docker Hub variables with placeholder values and descriptions.
3. **Update documentation**: Document usage, required env vars, examples, and security notes (prefer tokens, logout after use).
4. **Verification**: Validate script syntax, dry-run behavior with missing vars, and error messages.
5. **Integration check**: Confirm script reads from env correctly and produces expected output format.

No application code or infrastructure services are modified.