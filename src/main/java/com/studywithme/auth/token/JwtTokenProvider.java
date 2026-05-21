package com.studywithme.auth.token;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final TokenProperties tokenProperties;
	private final Clock clock;
	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;

	@Autowired
	public JwtTokenProvider(TokenProperties tokenProperties) {
		this(tokenProperties, Clock.systemUTC());
	}

	JwtTokenProvider(TokenProperties tokenProperties, Clock clock) {
		this.tokenProperties = tokenProperties;
		this.clock = clock;
		SecretKey secretKey = secretKey(tokenProperties.secret());
		this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
		this.jwtDecoder = decoder;
	}

	public AccessToken createAccessToken(Member member) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(tokenProperties.accessTokenTtl());
		List<String> roles = member.getRoles().stream()
			.map(MemberRole::name)
			.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(tokenProperties.issuer())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.subject(member.getId().toString())
			.claim("roles", roles)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(token, expiresAt);
	}

	public AccessTokenClaims parse(String accessToken) {
		try {
			Jwt jwt = jwtDecoder.decode(accessToken);
			if (!tokenProperties.issuer().equals(jwt.getClaimAsString("iss"))) {
				throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
			}
			if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(clock.instant())) {
				throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
			}

			List<String> roles = jwt.getClaimAsStringList("roles");
			if (roles == null || roles.isEmpty()) {
				throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
			}
			return new AccessTokenClaims(
				Long.parseLong(jwt.getSubject()),
				Set.copyOf(roles),
				jwt.getExpiresAt()
			);
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
	}

	private SecretKey secretKey(String secret) {
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 bytes for HS256.");
		}
		return new SecretKeySpec(secretBytes, "HmacSHA256");
	}
}
