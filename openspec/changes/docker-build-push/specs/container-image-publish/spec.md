## Purpose

Define the observable behavior of building the application container image from source and publishing it to Docker Hub, without specifying implementation details of how the build/push is invoked.

## ADDED Requirements

### Requirement: The system can build the application image from source

The build process SHALL produce a container image for the application using the `Dockerfile` present at the repository root. The build SHALL succeed when the source tree is clean and all required build dependencies are available.

#### Scenario: Building from a clean checkout succeeds

- **WHEN** the repository is checked out and the build is invoked against a clean working tree
- **THEN** the build completes successfully
- **AND** an image is produced

#### Scenario: Building when Dockerfile is missing fails clearly

- **WHEN** the `Dockerfile` is not present at the repository root
- **THEN** the build process fails with an error that identifies the missing `Dockerfile`

### Requirement: Images are deterministically tagged

The publish process SHALL tag the produced image with at least one Docker Hub repository reference. Tags SHALL be derivable from explicit inputs (environment variables or documented inputs) and include at least one immutable tag when requested. The default behavior SHALL produce a tag that identifies the build source.

#### Scenario: Image is tagged with the configured repository

- **WHEN** the build-and-push process runs with repository configuration provided
- **THEN** the produced image is tagged with the configured Docker Hub repository reference
- **AND** that tag is used when pushing to Docker Hub

#### Scenario: Explicit tags are applied

- **WHEN** explicit tag values are provided as inputs
- **THEN** the image is tagged with each provided tag before pushing
- **AND** all provided tags are pushed to the registry

#### Scenario: Default tag is applied when none specified

- **WHEN** no explicit tag is provided
- **THEN** at least one default tag is applied to the image
- **AND** that default tag is pushed to Docker Hub

### Requirement: The system authenticates with Docker Hub before pushing

The publish process SHALL authenticate to Docker Hub using credentials supplied via environment variables before attempting to push any image. Pushing SHALL NOT proceed if authentication credentials are missing or invalid.

#### Scenario: Push succeeds with valid credentials

- **WHEN** valid Docker Hub credentials are provided
- **AND** an image has been built and tagged
- **THEN** the image is pushed successfully to Docker Hub
- **AND** the pushed reference is reported

#### Scenario: Push fails with missing credentials

- **WHEN** Docker Hub credentials are not provided
- **THEN** the publish process fails before pushing
- **AND** the error indicates missing credentials

#### Scenario: Push fails with invalid credentials

- **WHEN** invalid Docker Hub credentials are provided
- **THEN** the publish process fails during authentication or before completing the push
- **AND** the error indicates authentication failure

### Requirement: The full image reference is reported on success

On successful push, the process SHALL report the full Docker Hub image reference(s) (including repository and tag(s)) and, when available, the image digest. The output SHALL be sufficient to pull the image from Docker Hub.

#### Scenario: Success reports pushed reference

- **WHEN** the image is pushed successfully
- **THEN** the output includes the full Docker Hub reference used for the push
- **AND** the reported reference is usable to pull the image

#### Scenario: Success reports digest when available

- **WHEN** the registry returns an image digest on push
- **THEN** the digest is included in the success output

### Requirement: Build and push are atomic in reporting failures

If any step fails (build, tag, authentication, or push), the process SHALL fail with a clear error message indicating which phase failed. The process SHALL NOT report a partial success state.

#### Scenario: Build failure is reported clearly

- **WHEN** the image build fails
- **THEN** the process exits with failure
- **AND** the error message indicates the build phase failed

#### Scenario: Push failure is reported clearly

- **WHEN** authentication succeeds but the push fails
- **THEN** the process exits with failure
- **AND** the error message indicates the push phase failed

### Requirement: Credentials and sensitive values are never logged

The publish process SHALL NOT write credentials, tokens, or other sensitive values to stdout, stderr, or logs. Only non-sensitive references (repository, tags, digests) may be reported.

#### Scenario: Credentials are not exposed in logs

- **WHEN** credentials are provided to the process
- **THEN** no credential value appears in any output or logs

#### Scenario: Only non-sensitive data is reported

- **WHEN** the process reports success or failure
- **THEN** only repository names, tags, digests, phase names, and generic error messages appear in output

### Requirement: Configuration is documented via environment variables

The publish process SHALL read required configuration (Docker Hub credentials, target repository, and tags) from documented environment variables. All required variables SHALL be documented with example values, and the behavior when they are omitted SHALL match the credential/tag requirements above.

#### Scenario: Configuration is discoverable via documentation

- **WHEN** configuration requirements are consulted
- **THEN** all required environment variables are documented with descriptions and example values
- **AND** optional variables are documented with their defaults

#### Scenario: Omitted optional variables use documented defaults

- **WHEN** an optional configuration variable is omitted
- **THEN** the process uses the documented default behavior for that variable