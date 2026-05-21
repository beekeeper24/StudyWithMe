# Auth Refresh Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add refresh/reissue and logout HTTP endpoints, and move browser refresh-token delivery from JSON response bodies to an HttpOnly cookie.

**Architecture:** Keep access tokens in JSON responses because frontend JavaScript must use them in `Authorization: Bearer ...`. Keep refresh tokens out of JSON and deliver them through a cookie managed by a small helper so OAuth login, refresh, and logout share one policy. `TokenService` keeps owning refresh-token rotation/revoke rules; `AuthController` only translates HTTP cookies into service calls.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring MVC, Spring Security, JPA, MockMvc, H2/Flyway tests.

---

## File Structure

- Create `src/main/java/com/studywithme/auth/presentation/AccessTokenResponse.java`
  - Response DTO containing access token, access token expiry, and token type only.
- Create `src/main/java/com/studywithme/auth/presentation/RefreshTokenCookieProperties.java`
  - Configuration properties for refresh-token cookie name, path, SameSite, and Secure flag.
- Create `src/main/java/com/studywithme/auth/presentation/RefreshTokenCookieWriter.java`
  - Writes and clears the refresh-token cookie consistently.
- Modify `src/main/java/com/studywithme/auth/presentation/AuthController.java`
  - Add `POST /api/v1/auth/refresh`.
  - Add `POST /api/v1/auth/logout`.
- Modify `src/main/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandler.java`
  - Set refresh-token cookie on OAuth success.
  - Return access-token-only JSON.
- Modify `src/main/java/com/studywithme/auth/token/TokenService.java`
  - Add `revoke(String rawRefreshToken)` for logout.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`
  - Permit `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout`.
- Modify tests:
  - `src/test/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandlerTest.java`
  - `src/test/java/com/studywithme/auth/presentation/AuthControllerTest.java`
  - `src/test/java/com/studywithme/auth/token/RefreshTokenServiceTest.java`

## HTTP Contract

Refresh cookie:

- Name: `refreshToken`
- Path: `/api/v1/auth`
- HttpOnly: always
- SameSite: `Lax` by default
- Secure: configurable with `REFRESH_TOKEN_COOKIE_SECURE`, default `false` for local HTTP development, `true` in production
- Max-Age: seconds until refresh token expiry

OAuth success and refresh response body:

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "accessTokenExpiresAt": "2026-05-21T00:30:00Z",
    "tokenType": "Bearer"
  },
  "message": "요청이 성공했습니다."
}
```

Logout response:

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공했습니다."
}
```

## Tasks

### Task 1: Token service revoke behavior

**Files:**
- Modify: `src/main/java/com/studywithme/auth/token/TokenService.java`
- Test: `src/test/java/com/studywithme/auth/token/RefreshTokenServiceTest.java`

- [x] **Step 1: Write the failing test**

Add this test to `RefreshTokenServiceTest`:

```java
@Test
@DisplayName("logout 시 refresh token을 폐기한다")
void revokeRefreshToken() {
    TokenService tokenService = tokenService();
    Member member = saveMember();
    TokenPair tokenPair = tokenService.issue(member);

    tokenService.revoke(tokenPair.refreshToken());

    RefreshToken savedToken = refreshTokenRepository.findByTokenHash(
        RefreshTokenHash.sha256(tokenPair.refreshToken())
    ).orElseThrow();
    assertThat(savedToken.isRevoked()).isTrue();
    assertThatThrownBy(() -> tokenService.refresh(tokenPair.refreshToken()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
}
```

- [x] **Step 2: Run test to verify RED**

```bash
./gradlew test --tests com.studywithme.auth.token.RefreshTokenServiceTest --no-daemon --console=plain
```

Expected: compile failure because `TokenService.revoke(String)` does not exist.

- [x] **Step 3: Implement minimal revoke**

Add this method to `TokenService`:

```java
@Transactional
public void revoke(String rawRefreshToken) {
    refreshTokenRepository.findByTokenHash(RefreshTokenHash.sha256(rawRefreshToken))
        .ifPresent(refreshToken -> refreshToken.revoke(clock.instant()));
}
```

- [x] **Step 4: Run test to verify GREEN**

```bash
./gradlew test --tests com.studywithme.auth.token.RefreshTokenServiceTest --no-daemon --console=plain
```

Expected: PASS.

### Task 2: Refresh-token cookie writer and access-token response

**Files:**
- Create: `src/main/java/com/studywithme/auth/presentation/AccessTokenResponse.java`
- Create: `src/main/java/com/studywithme/auth/presentation/RefreshTokenCookieProperties.java`
- Create: `src/main/java/com/studywithme/auth/presentation/RefreshTokenCookieWriter.java`
- Test: `src/test/java/com/studywithme/auth/oauth/OAuth2AuthenticationSuccessHandlerTest.java`

- [x] **Step 1: Write the failing OAuth success test**

Update the OAuth success test expectation:

```java
assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=refresh-token");
assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly");
assertThat(response.getHeader("Set-Cookie")).contains("SameSite=Lax");
assertThat(response.getContentAsString()).contains("\"accessToken\":\"access-token\"");
assertThat(response.getContentAsString()).doesNotContain("refreshToken");
```

- [x] **Step 2: Run test to verify RED**

```bash
./gradlew test --tests com.studywithme.auth.oauth.OAuth2AuthenticationSuccessHandlerTest --no-daemon --console=plain
```

Expected: FAIL because OAuth success still returns refresh token in JSON and does not set a cookie.

- [x] **Step 3: Implement DTO and cookie writer**

`AccessTokenResponse`:

```java
public record AccessTokenResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    String tokenType
) {
    private static final String BEARER = "Bearer";

    public static AccessTokenResponse from(TokenPair tokenPair) {
        return new AccessTokenResponse(
            tokenPair.accessToken(),
            tokenPair.accessTokenExpiresAt(),
            BEARER
        );
    }
}
```

`RefreshTokenCookieProperties`:

```java
@ConfigurationProperties(prefix = "app.auth.refresh-token-cookie")
public record RefreshTokenCookieProperties(
    String name,
    String path,
    boolean secure,
    String sameSite
) {
}
```

`RefreshTokenCookieWriter` should use `ResponseCookie` to write `HttpOnly`, configured `Secure`, configured `SameSite`, configured `Path`, and max age based on `TokenPair.refreshTokenExpiresAt()`.

- [x] **Step 4: Wire OAuth success to cookie response**

Inject `RefreshTokenCookieWriter`, write the refresh cookie, and serialize `ApiResponse.success(AccessTokenResponse.from(tokenPair))`.

- [x] **Step 5: Run test to verify GREEN**

```bash
./gradlew test --tests com.studywithme.auth.oauth.OAuth2AuthenticationSuccessHandlerTest --no-daemon --console=plain
```

Expected: PASS.

### Task 3: Refresh and logout HTTP endpoints

**Files:**
- Modify: `src/main/java/com/studywithme/auth/presentation/AuthController.java`
- Modify: `src/main/java/com/studywithme/global/security/SecurityConfig.java`
- Test: `src/test/java/com/studywithme/auth/presentation/AuthControllerTest.java`

- [x] **Step 1: Write failing refresh/logout tests**

Add tests for:

- `POST /api/v1/auth/refresh` with a valid refresh cookie returns access-token-only JSON, rotates the refresh token, and sets a new refresh cookie.
- `POST /api/v1/auth/refresh` without a refresh cookie returns `AUTH-004`.
- `POST /api/v1/auth/logout` with a refresh cookie revokes the token and clears the cookie.

- [x] **Step 2: Run tests to verify RED**

```bash
./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain
```

Expected: FAIL because endpoints are not implemented.

- [x] **Step 3: Implement endpoints**

Controller methods:

```java
@PostMapping("/refresh")
public ApiResponse<AccessTokenResponse> refresh(
    HttpServletRequest request,
    HttpServletResponse response
) {
    String refreshToken = refreshTokenCookieWriter.read(request)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
    TokenPair tokenPair = tokenService.refresh(refreshToken);
    refreshTokenCookieWriter.write(response, tokenPair);
    return ApiResponse.success(AccessTokenResponse.from(tokenPair));
}

@PostMapping("/logout")
public ApiResponse<Void> logout(
    HttpServletRequest request,
    HttpServletResponse response
) {
    refreshTokenCookieWriter.read(request)
        .ifPresent(tokenService::revoke);
    refreshTokenCookieWriter.clear(response);
    return ApiResponse.success(null);
}
```

Permit both endpoints in `SecurityConfig`.

- [x] **Step 4: Run tests to verify GREEN**

```bash
./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain
```

Expected: PASS.

### Task 4: Documentation and full verification

**Files:**
- Modify: `docs/handoff.md`
- Modify/create: `docs/learnings/*` if a reusable rule changes
- Update Notion dated StudyWithMe work log

- [x] **Step 1: Update docs**

Record that refresh token delivery is now cookie-based for OAuth success, refresh, and logout.

- [x] **Step 2: Run full tests**

```bash
./gradlew test --no-daemon --console=plain --rerun-tasks
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run security review**

Run focused `cso` review because this changes refresh-token cookie delivery, logout, and token rotation.

## Self-Review

- Spec coverage: refresh endpoint, logout endpoint, cookie delivery, JSON refresh-token removal, service revoke behavior, security permit-list, docs, and tests are covered.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: `AccessTokenResponse`, `RefreshTokenCookieWriter`, `TokenService.revoke`, and controller method names are consistent across tasks.
