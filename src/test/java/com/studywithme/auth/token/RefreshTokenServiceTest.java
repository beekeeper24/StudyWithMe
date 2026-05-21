package com.studywithme.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class RefreshTokenServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-05-21T00:00:00Z"),
		ZoneOffset.UTC
	);

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Test
	@DisplayName("토큰 발급 시 access token은 JWT로 반환하고 refresh token은 해시만 DB에 저장한다")
	void issueTokenPair() {
		TokenService tokenService = tokenService();
		Member member = saveMember();

		TokenPair tokenPair = tokenService.issue(member);

		RefreshToken savedToken = refreshTokenRepository.findAll().getFirst();
		assertThat(tokenPair.accessToken()).contains(".");
		assertThat(tokenPair.refreshToken()).isNotBlank();
		assertThat(savedToken.getTokenHash()).isNotEqualTo(tokenPair.refreshToken());
		assertThat(savedToken.matches(tokenPair.refreshToken())).isTrue();
		assertThat(savedToken.getMember().getId()).isEqualTo(member.getId());
		assertThat(savedToken.getExpiresAt()).isEqualTo(Instant.parse("2026-06-04T00:00:00Z"));
	}

	@Test
	@DisplayName("refresh token으로 재발급하면 기존 refresh token은 회전 처리하고 새 refresh token을 저장한다")
	void rotateRefreshToken() {
		TokenService tokenService = tokenService();
		Member member = saveMember();
		TokenPair firstPair = tokenService.issue(member);

		TokenPair secondPair = tokenService.refresh(firstPair.refreshToken());

		RefreshToken oldToken = refreshTokenRepository.findByTokenHash(
			RefreshTokenHash.sha256(firstPair.refreshToken())
		).orElseThrow();
		RefreshToken newToken = refreshTokenRepository.findByTokenHash(
			RefreshTokenHash.sha256(secondPair.refreshToken())
		).orElseThrow();
		assertThat(oldToken.isRotated()).isTrue();
		assertThat(newToken.isUsable(FIXED_CLOCK.instant())).isTrue();
		assertThat(secondPair.accessToken()).contains(".");
		assertThat(secondPair.refreshToken()).isNotEqualTo(firstPair.refreshToken());
		assertThat(refreshTokenRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("이미 회전 처리된 refresh token은 다시 사용할 수 없다")
	void rejectRotatedRefreshToken() {
		TokenService tokenService = tokenService();
		Member member = saveMember();
		TokenPair firstPair = tokenService.issue(member);
		tokenService.refresh(firstPair.refreshToken());

		assertThatThrownBy(() -> tokenService.refresh(firstPair.refreshToken()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
	}

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

	private TokenService tokenService() {
		TokenProperties tokenProperties = new TokenProperties(
			"studywithme-test",
			"studywithme-test-secret-key-must-be-at-least-32-bytes",
			Duration.ofMinutes(30),
			Duration.ofDays(14)
		);
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(tokenProperties, FIXED_CLOCK);
		return new TokenService(
			jwtTokenProvider,
			refreshTokenRepository,
			memberRepository,
			tokenProperties,
			FIXED_CLOCK
		);
	}

	private Member saveMember() {
		Member member = Member.createOAuthMember(
			"bee@example.com",
			"beekeeper",
			OAuthProvider.GOOGLE,
			"google-123",
			null
		);
		return memberRepository.saveAndFlush(member);
	}
}
