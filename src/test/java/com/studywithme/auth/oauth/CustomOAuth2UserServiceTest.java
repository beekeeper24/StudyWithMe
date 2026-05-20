package com.studywithme.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.auth.application.OAuthLoginService;
import com.studywithme.member.repository.MemberRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@DataJpaTest
class CustomOAuth2UserServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("OAuth2UserService는 provider profile을 회원으로 저장하고 인증 principal을 반환한다")
	void loadUser() {
		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = request -> new DefaultOAuth2User(
			// External provider authorities are ignored; service roles become app authorities.
			java.util.List.of(new SimpleGrantedAuthority("SCOPE_profile")),
			Map.of(
				"sub", "google-123",
				"email", "bee@example.com",
				"name", "beekeeper",
				"picture", "https://example.com/google.png"
			),
			"sub"
		);
		CustomOAuth2UserService userService = new CustomOAuth2UserService(
			delegate,
			new OAuthLoginService(memberRepository)
		);

		StudyWithMeOAuth2User user = (StudyWithMeOAuth2User) userService.loadUser(
			new OAuth2UserRequest(googleClientRegistration(), accessToken())
		);

		assertThat(user.getName()).isEqualTo("google-123");
		assertThat(user.memberId()).isNotNull();
		assertThat(user.getAttributes()).containsEntry("email", "bee@example.com");
		assertThat(user.getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_USER");
		assertThat(memberRepository.count()).isEqualTo(1);
	}

	private ClientRegistration googleClientRegistration() {
		return ClientRegistration.withRegistrationId("google")
			.clientId("google-client-id")
			.clientSecret("google-client-secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
			.tokenUri("https://oauth2.googleapis.com/token")
			.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
			.userNameAttributeName("sub")
			.clientName("Google")
			.scope("profile", "email")
			.build();
	}

	private OAuth2AccessToken accessToken() {
		return new OAuth2AccessToken(
			OAuth2AccessToken.TokenType.BEARER,
			"token",
			Instant.now(),
			Instant.now().plusSeconds(60)
		);
	}
}
