package com.studywithme.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record AppCorsProperties(
	List<String> allowedOrigins
) {

	private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
		"http://localhost:5173",
		"http://127.0.0.1:5173",
		"http://localhost:5174",
		"http://127.0.0.1:5174"
	);

	public AppCorsProperties {
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
		} else {
			allowedOrigins = List.copyOf(allowedOrigins);
		}
	}
}
