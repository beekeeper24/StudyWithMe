package com.studywithme.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.oauth-success")
public record OAuthSuccessRedirectProperties(
	String frontendRedirectUri
) {

	private static final String DEFAULT_FRONTEND_REDIRECT_URI = "http://localhost:5173/auth/callback";

	public OAuthSuccessRedirectProperties {
		if (frontendRedirectUri == null || frontendRedirectUri.isBlank()) {
			frontendRedirectUri = DEFAULT_FRONTEND_REDIRECT_URI;
		}
	}
}
