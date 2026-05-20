package com.studywithme.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.OAuthProvider;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuth2UserProfileFactoryTest {

	@Test
	@DisplayName("Google OAuth2 attributes를 서비스 프로필로 변환한다")
	void fromGoogleAttributes() {
		OAuth2UserProfile profile = OAuth2UserProfileFactory.from(
			"google",
			Map.of(
				"sub", "google-123",
				"email", "bee@example.com",
				"name", "beekeeper",
				"picture", "https://example.com/google.png"
			)
		);

		assertThat(profile.provider()).isEqualTo(OAuthProvider.GOOGLE);
		assertThat(profile.subject()).isEqualTo("google-123");
		assertThat(profile.email()).isEqualTo("bee@example.com");
		assertThat(profile.nickname()).isEqualTo("beekeeper");
		assertThat(profile.profileImageUrl()).isEqualTo("https://example.com/google.png");
	}

	@Test
	@DisplayName("Kakao OAuth2 attributes를 서비스 프로필로 변환한다")
	void fromKakaoAttributes() {
		OAuth2UserProfile profile = OAuth2UserProfileFactory.from(
			"kakao",
			Map.of(
				"id", 12345L,
				"kakao_account", Map.of(
					"email", "bee@kakao.com",
					"profile", Map.of(
						"nickname", "kakaoBee",
						"profile_image_url", "https://example.com/kakao.png"
					)
				)
			)
		);

		assertThat(profile.provider()).isEqualTo(OAuthProvider.KAKAO);
		assertThat(profile.subject()).isEqualTo("12345");
		assertThat(profile.email()).isEqualTo("bee@kakao.com");
		assertThat(profile.nickname()).isEqualTo("kakaoBee");
		assertThat(profile.profileImageUrl()).isEqualTo("https://example.com/kakao.png");
	}

	@Test
	@DisplayName("지원하지 않는 OAuth provider는 예외 처리한다")
	void unsupportedProvider() {
		assertThatThrownBy(() -> OAuth2UserProfileFactory.from("naver", Map.of()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
	}
}
