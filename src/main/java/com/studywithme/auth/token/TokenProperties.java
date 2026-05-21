package com.studywithme.auth.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.token")
public record TokenProperties(
	String issuer,
	String secret,
	Duration accessTokenTtl,
	Duration refreshTokenTtl
) {
}
