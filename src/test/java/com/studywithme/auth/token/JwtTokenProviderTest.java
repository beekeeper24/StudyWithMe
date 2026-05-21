package com.studywithme.auth.token;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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

	@Test
	@DisplayName("roles claim이 없는 access token은 거부한다")
	void rejectAccessTokenWithoutRoles() {
		JwtTokenProvider tokenProvider = new JwtTokenProvider(tokenProperties(), FIXED_CLOCK);
		String token = createTokenWithoutRoles();

		assertThatThrownBy(() -> tokenProvider.parse(token))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
	}

	private String createTokenWithoutRoles() {
		SecretKey secretKey = new SecretKeySpec(
			tokenProperties().secret().getBytes(StandardCharsets.UTF_8),
			"HmacSHA256"
		);
		JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(tokenProperties().issuer())
			.issuedAt(FIXED_CLOCK.instant())
			.expiresAt(FIXED_CLOCK.instant().plus(tokenProperties().accessTokenTtl()))
			.subject("1")
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
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
