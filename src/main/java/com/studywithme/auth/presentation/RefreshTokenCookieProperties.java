package com.studywithme.auth.presentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-token-cookie")
public record RefreshTokenCookieProperties(
	String name,
	String path,
	boolean secure,
	String sameSite
) {

	public RefreshTokenCookieProperties {
		name = valueOrDefault(name, "refreshToken");
		path = valueOrDefault(path, "/api/v1/auth");
		sameSite = valueOrDefault(sameSite, "Lax");
	}

	private static String valueOrDefault(String value, String defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value;
	}
}
