# Copilot Project Instructions

Use these repository conventions as defaults for this project.

## Architecture

- Keep layered architecture: api -> application -> domain <- infrastructure.
- Controllers must depend on application ports (interfaces), not concrete infrastructure classes.
- Prefer constructor injection.

## Backend/JPA

- Keep Task entity persistence model in infrastructure.
- Use Lombok in entities for boilerplate reduction when appropriate.
- Preserve existing API contracts unless explicitly requested.

## Liquibase

- Use XML changelog format with tag-based style (<createTable> with <column> children).
- Keep DDL/DML split:
  - backend/src/main/resources/db/changelog/ddl
  - backend/src/main/resources/db/changelog/dml
- Keep one DDL file per table.
- Master changelog is backend/src/main/resources/db/changelog/db.changelog-master.xml.
- When renaming/moving already executed changesets, preserve logicalFilePath compatibility.
- Do not reintroduce YAML changelog format unless explicitly requested.

## Local execution

- Prefer docker compose with infra/docker-compose.yml.
- Validate backend via /actuator/health.
- Keep frontend healthcheck robust against false negatives.

## Working style

- Prefer minimal, targeted changes.
- Validate edits with rebuild/health checks when affecting runtime configuration.