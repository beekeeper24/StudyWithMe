package com.studywithme.auth.oauth;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.OAuthProvider;
import java.util.Locale;
import java.util.Map;

public final class OAuth2UserProfileFactory {

	private OAuth2UserProfileFactory() {
	}

	public static OAuth2UserProfile from(String registrationId, Map<String, Object> attributes) {
		if (registrationId == null || registrationId.isBlank()) {
			throw new BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
		}
		String provider = registrationId.toLowerCase(Locale.ROOT);

		return switch (provider) {
			case "google" -> fromGoogle(attributes);
			case "kakao" -> fromKakao(attributes);
			default -> throw new BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
		};
	}

	private static OAuth2UserProfile fromGoogle(Map<String, Object> attributes) {
		return new OAuth2UserProfile(
			OAuthProvider.GOOGLE,
			requiredString(attributes, "sub"),
			requiredString(attributes, "email"),
			requiredString(attributes, "name"),
			nullableString(attributes, "picture")
		);
	}

	private static OAuth2UserProfile fromKakao(Map<String, Object> attributes) {
		Map<String, Object> kakaoAccount = requiredMap(attributes, "kakao_account");
		Map<String, Object> profile = requiredMap(kakaoAccount, "profile");

		return new OAuth2UserProfile(
			OAuthProvider.KAKAO,
			requiredString(attributes, "id"),
			requiredString(kakaoAccount, "email"),
			requiredString(profile, "nickname"),
			nullableString(profile, "profile_image_url")
		);
	}

	private static String requiredString(Map<String, Object> attributes, String key) {
		Object value = attributes.get(key);
		if (value == null || value.toString().isBlank()) {
			throw new BusinessException(AuthErrorCode.INVALID_OAUTH_ATTRIBUTES);
		}
		return value.toString();
	}

	private static String nullableString(Map<String, Object> attributes, String key) {
		Object value = attributes.get(key);
		if (value == null || value.toString().isBlank()) {
			return null;
		}
		return value.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> requiredMap(Map<String, Object> attributes, String key) {
		Object value = attributes.get(key);
		if (!(value instanceof Map<?, ?> map)) {
			throw new BusinessException(AuthErrorCode.INVALID_OAUTH_ATTRIBUTES);
		}
		return (Map<String, Object>) map;
	}
}
