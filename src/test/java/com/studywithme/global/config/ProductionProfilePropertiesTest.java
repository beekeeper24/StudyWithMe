package com.studywithme.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

class ProductionProfilePropertiesTest {

	private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	@Test
	@DisplayName("prod profile은 CORS 허용 origin을 운영 환경변수로 받아 로컬 기본값 누수를 막는다")
	void prodProfileRequiresFrontendCorsOrigins() throws Exception {
		Object allowedOrigins = loadProperty("application-prod.yml", "app.cors.allowed-origins");

		assertThat(allowedOrigins).hasToString("${APP_CORS_ALLOWED_ORIGINS}");
	}

	@Test
	@DisplayName("prod profile은 OAuth 성공 redirect URI를 운영 환경변수로 받아 로컬 callback 누수를 막는다")
	void prodProfileRequiresOAuthSuccessRedirectUri() throws Exception {
		Object redirectUri = loadProperty(
			"application-prod.yml",
			"app.auth.oauth-success.frontend-redirect-uri"
		);

		assertThat(redirectUri).hasToString("${OAUTH_SUCCESS_FRONTEND_REDIRECT_URI}");
	}

	private Object loadProperty(String resourceName, String propertyName) throws IOException {
		return loader.load(resourceName, new FileSystemResource("src/main/resources/" + resourceName))
			.stream()
			.map(source -> source.getProperty(propertyName))
			.filter(value -> value != null)
			.findFirst()
			.orElseThrow();
	}
}
