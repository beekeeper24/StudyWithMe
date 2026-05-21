package com.studywithme.auth.token;

import java.time.Instant;

public record AccessToken(
	String token,
	Instant expiresAt
) {
}
