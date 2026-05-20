# 0001 Project Decisions

## Decisions

- Main backend: Java + Spring Boot.
- Java version: 21.
- Spring Boot version: 3.5.14.
- API prefix: `/api/v1`.
- OAuth providers for MVP: Kakao and Google.
- Database: PostgreSQL.
- Repository: `beekeeper24/StudyWithMe`.
- Local workspace: `/home/beekeeper24/projects/StudyWithMe`.
- Branch strategy: Git Flow. `develop` is the integration branch and `main` is reserved for stable releases.
- FastAPI is reserved for future side services, not the MVP main API server.
- Gradle uses the Java 21 toolchain.
- The first test baseline uses H2 for `contextLoads`; PostgreSQL remains the application database.
- API responses use a common success/error envelope.
- Error codes use the `DOMAIN-XXX` format. Use `GLOBAL-XXX` only for cross-cutting framework-level errors.

## Rationale

Spring Boot fits the core MVP because StudyWithMe depends heavily on OAuth2, role-based authorization, study-membership access control, transactional notification creation, admin APIs, and chat security.

FastAPI remains useful later for recommendation, AI-assisted features, place data crawling, search assistance, or analytics where Python has an advantage.

## Reminder

Keep the implementation beginner-friendly: conventional package boundaries, explicit naming, focused tests, and short explanations for major backend concepts.

When a domain is first implemented, add its internal packages only as needed. Prefer `controller`, `service`, `domain`, `repository`, and `dto` over premature layered abstractions.

Do not push routine work directly to `main`. Work through `develop` and feature/fix/chore branches.

Common API response and error handling should stay in `global.common` and `global.exception`. Domain-specific errors should implement the shared `ErrorCode` interface from their own domain package when the domain is implemented.
