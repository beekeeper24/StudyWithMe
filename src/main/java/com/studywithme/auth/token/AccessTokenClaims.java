package com.studywithme.auth.token;

import java.time.Instant;
import java.util.Set;

public record AccessTokenClaims(
	Long memberId,
	Set<String> roles,
	Instant expiresAt
) {
}
