package com.studywithme.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-05-21T00:00:00Z"),
		ZoneOffset.UTC
	);

	@Test
	@DisplayName("회원 id와 권한을 담은 access token JWT를 발급하고 검증한다")
	void createAndParseAccessToken() {
		JwtTokenProvider tokenProvider = new JwtTokenProvider(tokenProperties(), FIXED_CLOCK);
		Member member = Member.createOAuthMember(
			"bee@example.com",
			"beekeeper",
			OAuthProvider.GOOGLE,
			"google-123",
			null
		);
		ReflectionTestUtils.setField(member, "id", 1L);

		AccessToken accessToken = tokenProvider.createAccessToken(member);
		AccessTokenClaims claims = tokenProvider.parse(accessToken.token());

		assertThat(accessToken.token()).contains(".");
		assertThat(accessToken.expiresAt()).isEqualTo(Instant.parse("2026-05-21T00:30:00Z"));
		assertThat(claims.memberId()).isEqualTo(1L);
		assertThat(claims.roles()).containsExactly(MemberRole.USER.name());
		assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2026-05-21T00:30:00Z"));
	}

	private TokenProperties tokenProperties() {
		return new TokenProperties(
			"studywithme-test",
			"studywithme-test-secret-key-must-be-at-least-32-bytes",
			Duration.ofMinutes(30),
			Duration.ofDays(14)
		);
	}
}
