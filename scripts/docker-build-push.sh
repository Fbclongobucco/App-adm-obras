#!/bin/bash
set -euo pipefail

# Docker build and push script
# Reads configuration from environment variables. Never logs secrets.

log() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S')] $*" >&2
}

error() {
  log "ERROR: $*"
  exit 1
}

# Load env if present
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:-}"
DOCKERHUB_TOKEN="${DOCKERHUB_TOKEN:-}"
DOCKERHUB_PASSWORD="${DOCKERHUB_PASSWORD:-}"
DOCKERHUB_REPOSITORY="${DOCKERHUB_REPOSITORY:-${DOCKER_IMAGE_NAME:-}}"
DOCKERHUB_TAGS="${DOCKERHUB_TAGS:-}"
BUILD_CONTEXT="${BUILD_CONTEXT:-.}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-Dockerfile}"

CRED="${DOCKERHUB_TOKEN:-${DOCKERHUB_PASSWORD:-}}"

# Validate
if [ -z "${DOCKERHUB_USERNAME}" ]; then
  error "Missing DOCKERHUB_USERNAME"
fi
if [ -z "${CRED}" ]; then
  error "Missing DOCKERHUB_TOKEN or DOCKERHUB_PASSWORD"
fi
if [ -z "${DOCKERHUB_REPOSITORY}" ]; then
  error "Missing DOCKERHUB_REPOSITORY (or DOCKER_IMAGE_NAME)"
fi
if [ ! -f "${DOCKERFILE_PATH}" ]; then
  error "Missing ${DOCKERFILE_PATH}"
fi

# Build tags
TAGS=()
if [ -n "${DOCKERHUB_TAGS}" ]; then
  # split by space or comma
  for t in ${DOCKERHUB_TAGS//,/ }; do
    [ -n "$t" ] && TAGS+=("$t")
  done
else
  TAGS+=("latest")
  if command -v git >/dev/null 2>&1; then
    sha=$(git rev-parse --short HEAD 2>/dev/null || true)
    [ -n "$sha" ] && TAGS+=("$sha")
  fi
fi

[ ${#TAGS[@]} -eq 0 ] && TAGS+=("latest")

log "Phase: build"
docker build -f "${DOCKERFILE_PATH}" -t "${DOCKERHUB_REPOSITORY}:temp" "${BUILD_CONTEXT}" || error "Build phase failed"

# Tag
for t in "${TAGS[@]}"; do
  docker tag "${DOCKERHUB_REPOSITORY}:temp" "${DOCKERHUB_REPOSITORY}:${t}"
done

# Login
log "Phase: auth"
printf '%s' "${CRED}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin || error "Auth phase failed"

# Push
for t in "${TAGS[@]}"; do
  log "Phase: push ${DOCKERHUB_REPOSITORY}:${t}"
  docker push "${DOCKERHUB_REPOSITORY}:${t}" || error "Push phase failed for ${DOCKERHUB_REPOSITORY}:${t}"
done

# Report
for t in "${TAGS[@]}"; do
  full="${DOCKERHUB_USERNAME}/${DOCKERHUB_REPOSITORY##*/}:${t}"
  # try to use full if repo doesn't include username
  if [[ "${DOCKERHUB_REPOSITORY}" != */* ]]; then
    full="${DOCKERHUB_USERNAME}/${DOCKERHUB_REPOSITORY}:${t}"
    ref="${full}"
  else
    ref="${DOCKERHUB_REPOSITORY}:${t}"
  fi
  echo "PUSHED: ${ref}"
done

# digest
for t in "${TAGS[@]}"; do
  ref="${DOCKERHUB_REPOSITORY}:${t}"
  if [[ "${DOCKERHUB_REPOSITORY}" != */* ]]; then
    ref="${DOCKERHUB_USERNAME}/${DOCKERHUB_REPOSITORY}:${t}"
  fi
  digest=$(docker inspect --format='{{index .RepoDigests 0}}' "${ref}" 2>/dev/null || true)
  if [ -n "$digest" ]; then
    echo "DIGEST: ${digest}"
  else
    echo "DIGEST: unavailable"
  fi
  break
done
