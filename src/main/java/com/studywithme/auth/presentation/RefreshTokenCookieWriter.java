package com.studywithme.auth.presentation;

import com.studywithme.auth.token.TokenPair;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieWriter {

	private final RefreshTokenCookieProperties properties;

	public RefreshTokenCookieWriter(RefreshTokenCookieProperties properties) {
		this.properties = properties;
	}

	public void write(HttpServletResponse response, TokenPair tokenPair) {
		long maxAgeSeconds = Math.max(
			0,
			Duration.between(Instant.now(), tokenPair.refreshTokenExpiresAt()).toSeconds()
		);
		ResponseCookie cookie = baseCookie(tokenPair.refreshToken())
			.maxAge(Duration.ofSeconds(maxAgeSeconds))
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public void clear(HttpServletResponse response) {
		ResponseCookie cookie = baseCookie("")
			.maxAge(Duration.ZERO)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public Optional<String> read(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> properties.name().equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> !value.isBlank())
			.findFirst();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(properties.name(), value)
			.httpOnly(true)
			.secure(properties.secure())
			.path(properties.path())
			.sameSite(properties.sameSite());
	}
}
