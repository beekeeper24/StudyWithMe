package com.studywithme.auth.oauth;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.presentation.RefreshTokenCookieWriter;
import com.studywithme.auth.token.TokenPair;
import com.studywithme.auth.token.TokenService;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final TokenService tokenService;
	private final MemberRepository memberRepository;
	private final RefreshTokenCookieWriter refreshTokenCookieWriter;
	private final OAuthSuccessRedirectProperties redirectProperties;

	public OAuth2AuthenticationSuccessHandler(
		TokenService tokenService,
		MemberRepository memberRepository,
		RefreshTokenCookieWriter refreshTokenCookieWriter,
		OAuthSuccessRedirectProperties redirectProperties
	) {
		this.tokenService = tokenService;
		this.memberRepository = memberRepository;
		this.refreshTokenCookieWriter = refreshTokenCookieWriter;
		this.redirectProperties = redirectProperties;
	}

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException, ServletException {
		StudyWithMeOAuth2User principal = (StudyWithMeOAuth2User) authentication.getPrincipal();
		Member member = memberRepository.findById(principal.memberId())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN));
		TokenPair tokenPair = tokenService.issue(member);

		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		refreshTokenCookieWriter.write(response, tokenPair);
		response.sendRedirect(frontendRedirectUri(tokenPair));
	}

	private String frontendRedirectUri(TokenPair tokenPair) {
		return UriComponentsBuilder.fromUriString(redirectProperties.frontendRedirectUri())
			.fragment("accessToken={accessToken}&accessTokenExpiresAt={accessTokenExpiresAt}&tokenType=Bearer")
			.buildAndExpand(
				tokenPair.accessToken(),
				tokenPair.accessTokenExpiresAt().toString()
			)
			.toUriString();
	}
}
