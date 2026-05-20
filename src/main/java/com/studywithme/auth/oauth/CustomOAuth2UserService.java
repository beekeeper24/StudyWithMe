package com.studywithme.auth.oauth;

import com.studywithme.auth.application.OAuthLoginService;
import com.studywithme.member.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
	private final OAuthLoginService oauthLoginService;

	@Autowired
	public CustomOAuth2UserService(OAuthLoginService oauthLoginService) {
		this(new DefaultOAuth2UserService(), oauthLoginService);
	}

	CustomOAuth2UserService(
		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
		OAuthLoginService oauthLoginService
	) {
		this.delegate = delegate;
		this.oauthLoginService = oauthLoginService;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = delegate.loadUser(userRequest);
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuth2UserProfile profile = OAuth2UserProfileFactory.from(registrationId, oauth2User.getAttributes());
		Member member = oauthLoginService.loginOrSignUp(profile);

		return StudyWithMeOAuth2User.from(member, profile, oauth2User.getAttributes());
	}
}
