package com.studywithme.auth.presentation;

import com.studywithme.auth.token.TokenPair;
import java.time.Instant;

public record AccessTokenResponse(
	String accessToken,
	Instant accessTokenExpiresAt,
	String tokenType
) {

	private static final String BEARER = "Bearer";

	public static AccessTokenResponse from(TokenPair tokenPair) {
		return new AccessTokenResponse(
			tokenPair.accessToken(),
			tokenPair.accessTokenExpiresAt(),
			BEARER
		);
	}
}
