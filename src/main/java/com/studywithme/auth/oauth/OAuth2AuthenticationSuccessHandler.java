package com.studywithme.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.presentation.AccessTokenResponse;
import com.studywithme.auth.presentation.RefreshTokenCookieWriter;
import com.studywithme.auth.token.TokenPair;
import com.studywithme.auth.token.TokenService;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final TokenService tokenService;
	private final MemberRepository memberRepository;
	private final RefreshTokenCookieWriter refreshTokenCookieWriter;
	private final ObjectMapper objectMapper;

	public OAuth2AuthenticationSuccessHandler(
		TokenService tokenService,
		MemberRepository memberRepository,
		RefreshTokenCookieWriter refreshTokenCookieWriter,
		ObjectMapper objectMapper
	) {
		this.tokenService = tokenService;
		this.memberRepository = memberRepository;
		this.refreshTokenCookieWriter = refreshTokenCookieWriter;
		this.objectMapper = objectMapper;
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

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		refreshTokenCookieWriter.write(response, tokenPair);
		objectMapper.writeValue(response.getWriter(), ApiResponse.success(AccessTokenResponse.from(tokenPair)));
	}
}
