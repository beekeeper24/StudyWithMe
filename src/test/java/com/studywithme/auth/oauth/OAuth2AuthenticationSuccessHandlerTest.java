package com.studywithme.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.presentation.RefreshTokenCookieProperties;
import com.studywithme.auth.presentation.RefreshTokenCookieWriter;
import com.studywithme.auth.token.TokenPair;
import com.studywithme.auth.token.TokenService;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2AuthenticationSuccessHandlerTest {

	private final TokenService tokenService = Mockito.mock(TokenService.class);
	private final MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
	private final RefreshTokenCookieWriter refreshTokenCookieWriter = new RefreshTokenCookieWriter(
		new RefreshTokenCookieProperties("refreshToken", "/api/v1/auth", false, "Lax")
	);
	private final OAuth2AuthenticationSuccessHandler successHandler = new OAuth2AuthenticationSuccessHandler(
		tokenService,
		memberRepository,
		refreshTokenCookieWriter,
		new ObjectMapper().findAndRegisterModules()
	);

	@Test
	@DisplayName("OAuth 인증 성공 시 access token은 JSON으로 반환하고 refresh token은 cookie로 설정한다")
	void writeTokenPairResponse() throws Exception {
		Member member = Member.createOAuthMember(
			"bee@example.com",
			"beekeeper",
			OAuthProvider.GOOGLE,
			"google-123",
			null
		);
		ReflectionTestUtils.setField(member, "id", 1L);
		OAuth2UserProfile profile = new OAuth2UserProfile(
			OAuthProvider.GOOGLE,
			"google-123",
			"bee@example.com",
			"beekeeper",
			null
		);
		StudyWithMeOAuth2User principal = StudyWithMeOAuth2User.from(member, profile, Map.of("sub", "google-123"));
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(tokenService.issue(member)).thenReturn(new TokenPair(
			"access-token",
			Instant.parse("2026-05-21T00:30:00Z"),
			"refresh-token",
			Instant.parse("2026-06-04T00:00:00Z")
		));
		MockHttpServletResponse response = new MockHttpServletResponse();

		successHandler.onAuthenticationSuccess(
			new MockHttpServletRequest(),
			response,
			new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
		);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
		assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=refresh-token");
		assertThat(response.getHeader("Set-Cookie")).contains("Path=/api/v1/auth");
		assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly");
		assertThat(response.getHeader("Set-Cookie")).contains("SameSite=Lax");
		assertThat(response.getContentAsString()).contains("\"success\":true");
		assertThat(response.getContentAsString()).contains("\"accessToken\":\"access-token\"");
		assertThat(response.getContentAsString()).doesNotContain("refreshToken");
		assertThat(response.getContentAsString()).contains("\"tokenType\":\"Bearer\"");
	}
}
