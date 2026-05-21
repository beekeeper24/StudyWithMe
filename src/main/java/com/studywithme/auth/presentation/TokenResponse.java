package com.studywithme.auth.presentation;

import com.studywithme.auth.token.TokenPair;
import java.time.Instant;

public record TokenResponse(
	String accessToken,
	Instant accessTokenExpiresAt,
	String refreshToken,
	Instant refreshTokenExpiresAt,
	String tokenType
) {

	private static final String BEARER = "Bearer";

	public static TokenResponse from(TokenPair tokenPair) {
		return new TokenResponse(
			tokenPair.accessToken(),
			tokenPair.accessTokenExpiresAt(),
			tokenPair.refreshToken(),
			tokenPair.refreshTokenExpiresAt(),
			BEARER
		);
	}
}
