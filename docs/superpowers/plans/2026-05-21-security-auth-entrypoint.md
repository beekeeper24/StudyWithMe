# Security Auth Entrypoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect existing OAuth/JWT token code to real HTTP requests with Spring Security, OAuth success token issuing, JWT bearer authentication, and `GET /api/v1/auth/me`.

**Architecture:** Keep Spring Security wiring conventional and small. A `SecurityFilterChain` configures JWT-only API authentication, delegates OAuth user loading to `CustomOAuth2UserService`, writes a token JSON response in the OAuth success handler, and runs one JWT filter before username/password auth. The filter creates a lightweight authenticated principal from JWT claims; `/auth/me` reads that principal and returns member profile data from the database.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Security, OAuth2 Client, JPA, MockMvc, H2/Flyway tests.

---

## File Structure

- Create `src/main/java/com/studywithme/global/security/AuthenticatedMemberPrincipal.java`
  - Lightweight principal containing `memberId` and role names from access-token claims.
- Create `src/main/java/com/studywithme/global/security/JwtAuthenticationFilter.java`
  - Reads `Authorization: Bearer <token>`, validates with `JwtTokenProvider`, and populates `SecurityContext`.
- Create `src/main/java/com/studywithme/global/security/JwtAuthenticationEntryPoint.java`
  - Returns the existing `ErrorResponse` shape for unauthenticated requests.
- Create `src/main/java/com/studywithme/global/security/SecurityConfig.java`
  - Defines `SecurityFilterChain`, JWT-only API security, permit-list, deny-by-default behavior, OAuth user service, OAuth success handler, and JWT filter.
- Create `src/main/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandler.java`
  - Issues `TokenPair` and writes `ApiResponse<TokenResponse>` JSON; local MVP policy is response-body tokens, not URL query tokens.
- Create `src/main/java/com/studywithme/auth/presentation/AuthController.java`
  - Exposes `GET /api/v1/auth/me`.
- Create `src/main/java/com/studywithme/auth/presentation/AuthMeResponse.java`
  - Response DTO for current member.
- Create `src/main/java/com/studywithme/auth/presentation/TokenResponse.java`
  - Response DTO for OAuth success token delivery.
- Create focused tests:
  - `src/test/java/com/studywithme/global/security/JwtAuthenticationFilterTest.java`
  - `src/test/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandlerTest.java`
  - `src/test/java/com/studywithme/auth/presentation/AuthControllerTest.java`

## Token Delivery Decision

For the local MVP, OAuth success returns HTTP `200` JSON:

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "accessTokenExpiresAt": "2026-05-21T00:30:00Z",
    "refreshToken": "...",
    "refreshTokenExpiresAt": "2026-06-04T00:00:00Z",
    "tokenType": "Bearer"
  },
  "message": "요청이 성공했습니다."
}
```

Reason: it avoids leaking tokens in redirect URLs while keeping the browser/manual MVP flow easy to inspect.

Frontend/production decision: before connecting a real browser frontend, refresh token delivery should move to an `HttpOnly; Secure; SameSite` cookie. Use `SameSite=Lax` by default for same-site frontend/API deployment, and use `SameSite=None; Secure` only if cross-site deployment requires credentialed browser requests. After that change, the OAuth success response can expose access-token data, but should not expose the raw refresh token in JSON.

## Tasks

### Task 1: JWT bearer authentication filter

**Files:**
- Create: `src/main/java/com/studywithme/global/security/AuthenticatedMemberPrincipal.java`
- Create: `src/main/java/com/studywithme/global/security/JwtAuthenticationFilter.java`
- Test: `src/test/java/com/studywithme/global/security/JwtAuthenticationFilterTest.java`

- [x] **Step 1: Write the failing tests**

Test behaviors:
- valid bearer token creates authenticated principal with `memberId` and `ROLE_USER`;
- missing bearer token leaves the request anonymous;
- invalid bearer token clears context and returns `AUTH-003`.

- [x] **Step 2: Run tests to verify RED**

```bash
./gradlew test --tests com.studywithme.global.security.JwtAuthenticationFilterTest --no-daemon --console=plain
```

Expected: compile failure because filter/principal classes do not exist yet.

- [x] **Step 3: Implement minimal filter and principal**

Use `OncePerRequestFilter`, `JwtTokenProvider.parse`, and `UsernamePasswordAuthenticationToken`.

- [x] **Step 4: Run tests to verify GREEN**

```bash
./gradlew test --tests com.studywithme.global.security.JwtAuthenticationFilterTest --no-daemon --console=plain
```

Expected: PASS.

### Task 2: OAuth success token handler

**Files:**
- Create: `src/main/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandler.java`
- Create: `src/main/java/com/studywithme/auth/presentation/TokenResponse.java`
- Test: `src/test/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandlerTest.java`

- [x] **Step 1: Write the failing test**

Test behavior: given `StudyWithMeOAuth2User`, handler calls `TokenService.issue` for that member and writes `ApiResponse<TokenResponse>` JSON.

- [x] **Step 2: Run test to verify RED**

```bash
./gradlew test --tests com.studywithme.auth.oauth.OAuth2AuthenticationSuccessHandlerTest --no-daemon --console=plain
```

Expected: compile failure because success handler/response DTO do not exist.

- [x] **Step 3: Implement minimal handler**

Find member by principal member id, issue token pair, set `200 OK`, `application/json`, UTF-8, and write with `ObjectMapper`.

- [x] **Step 4: Run test to verify GREEN**

```bash
./gradlew test --tests com.studywithme.auth.oauth.OAuth2AuthenticationSuccessHandlerTest --no-daemon --console=plain
```

Expected: PASS.

### Task 3: SecurityFilterChain

**Files:**
- Create: `src/main/java/com/studywithme/global/security/SecurityConfig.java`
- Create: `src/main/java/com/studywithme/global/security/JwtAuthenticationEntryPoint.java`
- Test: extend `src/test/java/com/studywithme/auth/presentation/AuthControllerTest.java`

- [x] **Step 1: Write the failing security tests**

Test behaviors:
- unauthenticated `GET /api/v1/auth/me` returns `401` with `AUTH-003`;
- authenticated request reaches controller.

- [x] **Step 2: Run test to verify RED**

```bash
./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain
```

Expected: compile failure because controller/security classes do not exist.

- [x] **Step 3: Implement minimal security config**

Configure:
- CSRF disabled for bearer-token JSON API MVP;
- HTTP session creation allowed only when OAuth provider state needs it;
- `NullSecurityContextRepository` prevents session-stored API authentication;
- `/api/v1/auth/me` authenticated;
- actuator health/info and OAuth endpoints permitted;
- routes not explicitly permitted are denied by default;
- OAuth user service and success handler wired;
- JWT filter inserted before `UsernamePasswordAuthenticationFilter`.

- [x] **Step 4: Run test to verify GREEN**

```bash
./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain
```

Expected: PASS.

### Task 4: `GET /api/v1/auth/me`

**Files:**
- Create: `src/main/java/com/studywithme/auth/presentation/AuthController.java`
- Create: `src/main/java/com/studywithme/auth/presentation/AuthMeResponse.java`
- Test: `src/test/java/com/studywithme/auth/presentation/AuthControllerTest.java`

- [x] **Step 1: Write the failing test**

Test behavior: with a valid JWT for a saved member, `GET /api/v1/auth/me` returns id, email, nickname, profile image URL, status, and roles in `ApiResponse`.

- [x] **Step 2: Run test to verify RED**

```bash
./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain
```

Expected: fail because endpoint does not exist.

- [x] **Step 3: Implement minimal controller**

Use `@AuthenticationPrincipal AuthenticatedMemberPrincipal`, load member by id, and return `ApiResponse.success(AuthMeResponse.from(member))`.

- [x] **Step 4: Run test to verify GREEN**

```bash
./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain
```

Expected: PASS.

### Task 5: Full verification and handoff

**Files:**
- Modify: `docs/handoff.md` if behavior changes need to be preserved.
- Create/update: dated Notion work-log page under `작업일지 > StudyWithMe` if connector access is available.

- [x] **Step 1: Run full tests**

```bash
./gradlew test --no-daemon --console=plain
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 2: Run security review for auth changes**

Use the project-required lightweight `cso` security review because this touches OAuth/JWT/authentication.

- [x] **Step 3: Update handoff/learning notes**

Record the token delivery policy and endpoint behavior so the next session can resume quickly.

## Self-Review

- Spec coverage: SecurityFilterChain, CustomOAuth2UserService wiring, JWT filter, OAuth success handler, token delivery policy, `/api/v1/auth/me`, and focused tests are all mapped to tasks.
- Placeholder scan: no `TBD`, generic "add tests", or unspecified file paths remain.
- Type consistency: principal, response DTOs, filter, handler, config, and controller names are consistent across tasks.
