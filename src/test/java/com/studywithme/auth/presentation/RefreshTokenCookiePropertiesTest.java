package com.studywithme.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

class RefreshTokenCookiePropertiesTest {

	private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	@Test
	@DisplayName("기본 profile은 로컬 HTTP 개발을 위해 refresh token cookie Secure 기본값을 끈다")
	void localProfileKeepsRefreshCookieSecureDisabledByDefault() throws Exception {
		Object secure = loadProperty("application.yml", "app.auth.refresh-token-cookie.secure");

		assertThat(secure).hasToString("${REFRESH_TOKEN_COOKIE_SECURE:false}");
	}

	@Test
	@DisplayName("prod profile은 HTTPS 운영을 위해 refresh token cookie Secure 기본값을 켠다")
	void prodProfileEnablesRefreshCookieSecureByDefault() throws Exception {
		Object secure = loadProperty("application-prod.yml", "app.auth.refresh-token-cookie.secure");

		assertThat(secure).hasToString("${REFRESH_TOKEN_COOKIE_SECURE:true}");
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
