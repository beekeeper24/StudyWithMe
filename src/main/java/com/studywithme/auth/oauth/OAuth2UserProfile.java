package com.studywithme.auth.oauth;

import com.studywithme.member.domain.OAuthProvider;

public record OAuth2UserProfile(
	OAuthProvider provider,
	String subject,
	String email,
	String nickname,
	String profileImageUrl
) {
}
