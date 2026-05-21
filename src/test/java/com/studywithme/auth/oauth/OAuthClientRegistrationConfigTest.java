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
		assertThat(kakao.getProviderDetails().getAuthorizationUri())
			.isEqualTo("https://kauth.kakao.com/oauth/authorize");
		assertThat(kakao.getProviderDetails().getTokenUri()).isEqualTo("https://kauth.kakao.com/oauth/token");
		assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUri())
			.isEqualTo("https://kapi.kakao.com/v2/user/me");
		assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()).isEqualTo("id");
		assertThat(kakao.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
	}
}
