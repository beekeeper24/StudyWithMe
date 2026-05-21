# OAuth Client Env Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make real Google/Kakao OAuth browser login configurable through environment variables without committing provider credentials.

**Architecture:** Keep default local/test startup free of real OAuth credentials by placing provider registration in an `oauth` Spring profile. When `oauth` is active, Spring Boot creates Google and Kakao `ClientRegistration` entries from environment variables; without the profile, the app can still run unit/integration tests that do not need provider redirects.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Security OAuth2 Client, JUnit 5, AssertJ.

---

## File Structure

- Create `src/main/resources/application-oauth.yml`
  - Owns real provider registration templates.
  - Reads client id/secret only from environment placeholders.
- Create `src/test/java/com/studywithme/auth/oauth/OAuthClientRegistrationConfigTest.java`
  - Verifies that activating the `oauth` profile with test environment values creates usable Google/Kakao registrations.
- Modify `docs/handoff.md`
  - Records how to run the app for real OAuth browser testing.
- Modify `docs/learnings/0003-oauth-login-baseline.md`
  - Captures the provider configuration rule for future sessions.

## Task 1: OAuth Client Registration Config

**Files:**
- Create: `src/test/java/com/studywithme/auth/oauth/OAuthClientRegistrationConfigTest.java`
- Create: `src/main/resources/application-oauth.yml`

- [x] **Step 1: Write the failing test**

Create `src/test/java/com/studywithme/auth/oauth/OAuthClientRegistrationConfigTest.java`:

```java
package com.studywithme.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("oauth")
@TestPropertySource(properties = {
	"GOOGLE_CLIENT_ID=google-client-id",
	"GOOGLE_CLIENT_SECRET=google-client-secret",
	"KAKAO_CLIENT_ID=kakao-client-id",
	"KAKAO_CLIENT_SECRET=kakao-client-secret"
})
class OAuthClientRegistrationConfigTest {

	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	@Test
	@DisplayName("oauth profile은 Google과 Kakao client registration을 환경변수 값으로 구성한다")
	void configureGoogleAndKakaoOAuthClientsFromEnvironment() {
		ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");
		ClientRegistration kakao = clientRegistrationRepository.findByRegistrationId("kakao");

		assertThat(google.getClientId()).isEqualTo("google-client-id");
		assertThat(google.getClientSecret()).isEqualTo("google-client-secret");
		assertThat(google.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
		assertThat(google.getScopes()).containsExactlyInAnyOrder("profile", "email");

		assertThat(kakao.getClientId()).isEqualTo("kakao-client-id");
		assertThat(kakao.getClientSecret()).isEqualTo("kakao-client-secret");
		assertThat(kakao.getProviderDetails().getAuthorizationUri()).isEqualTo("https://kauth.kakao.com/oauth/authorize");
		assertThat(kakao.getProviderDetails().getTokenUri()).isEqualTo("https://kauth.kakao.com/oauth/token");
		assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUri()).isEqualTo("https://kapi.kakao.com/v2/user/me");
		assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()).isEqualTo("id");
		assertThat(kakao.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
	}
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests com.studywithme.auth.oauth.OAuthClientRegistrationConfigTest --no-daemon --console=plain
```

Expected: FAIL because no OAuth client registration is available for the `oauth` profile yet.

- [x] **Step 3: Add OAuth profile configuration**

Create `src/main/resources/application-oauth.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: oauth
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - profile
              - email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - profile_nickname
              - profile_image
              - account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

- [x] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew test --tests com.studywithme.auth.oauth.OAuthClientRegistrationConfigTest --no-daemon --console=plain
```

Expected: PASS.

## Task 2: Handoff And Learning Notes

**Files:**
- Modify: `docs/handoff.md`
- Modify: `docs/learnings/0003-oauth-login-baseline.md`

- [x] **Step 1: Update handoff with activation command**

Add the OAuth runtime activation rule:

```markdown
OAuth client config:

- Real provider registrations live in the `oauth` Spring profile.
- Start local browser testing with `SPRING_PROFILES_ACTIVE=oauth`.
- Required environment variables:
  - `GOOGLE_CLIENT_ID`
  - `GOOGLE_CLIENT_SECRET`
  - `KAKAO_CLIENT_ID`
  - `KAKAO_CLIENT_SECRET`
```

- [x] **Step 2: Update learning note**

Add a short future-session rule:

```markdown
Provider client registrations should stay in `application-oauth.yml` under the `oauth` profile. Keep real client id/secret values outside git and pass them through environment variables or the deployment secret store.
```

- [x] **Step 3: Run full verification**

Run:

```bash
./gradlew test --no-daemon --console=plain --rerun-tasks
git diff --check
```

Expected: both commands pass.

## Self-Review

- Spec coverage: provider registration templates, env-only secret handling, and browser-test activation notes are covered.
- Placeholder scan: no TBD/TODO markers remain.
- Type consistency: test uses Spring Security's `ClientRegistrationRepository`, matching the runtime auto-configuration target.
