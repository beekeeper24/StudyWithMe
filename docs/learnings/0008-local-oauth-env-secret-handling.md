# 0008. Local OAuth env secret handling

## Context

OAuth provider credentials are required only for real browser login tests, not for normal local/test runs.

## Rule

- Never commit real OAuth client ids or secrets.
- Keep local credentials in `.env`.
- Keep `.env` and `.env.*` ignored, but commit `.env.example` as the template.
- Use `scripts/run-oauth-local.sh` for local OAuth browser verification so secrets are loaded without typing them into every command.

## Why

Spring Boot does not automatically read a repository `.env` file. A small script keeps local execution convenient while preserving the existing environment-variable based production model.
