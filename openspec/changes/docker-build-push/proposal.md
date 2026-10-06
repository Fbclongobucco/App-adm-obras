## Why

The repository already has planning to containerize the application (`infra-docker-compose-e-der`). That change creates a `Dockerfile` and a Compose that runs the app, but it does not define how to build a distributable image and push it to a registry. Without a repeatable build-and-publish pipeline, every environment that needs the image must build locally, secrets are harder to manage, and deployment consistency suffers.

The ask is explicit: "crie o Dockerfile pra criar a imagem, e mande a imagem pro docker hub". The `Dockerfile` itself is already planned in the other change, but the push workflow is not. This change captures the behavior of building the image from the repository and publishing it to Docker Hub, in a way that can be executed in a controlled environment.

## What Changes

- Define the behavior of building and tagging the application image from the `Dockerfile`.
- Define how the image is authenticated and pushed to Docker Hub.
- Capture required configuration (registry credentials, repository name, tags) via environment variables or documented inputs.
- Document the expected outputs (image digest/reference pushed) and failure modes.

**BREAKING**: No breaking change to the application API. This is infrastructure/process behavior only.

## Capabilities

### New Capabilities

- `container-image-publish`: the system can build the application container image from source and publish it to Docker Hub with deterministic tagging.

### Modified Capabilities

None.

## Impact

**Artifacts**
- New specs under `specs/container-image-publish/`
- Design and tasks describing the build-and-push process (no application code changes)

**Configuration**
- Documentation of required environment variables for Docker Hub credentials and image naming
- No secrets committed to the repository

**External dependencies**
- Docker Engine (buildx/docker) and access to Docker Hub registry (hub.docker.com)

**Interactions with existing changes**
- The `Dockerfile` created by `infra-docker-compose-e-der` is the input for the build step. The publish process assumes that `Dockerfile` exists at the repository root. If that change is not applied yet, the build will fail as described in failure modes, but this change only defines the behavior of build-and-push itself, not the creation of the Dockerfile content.