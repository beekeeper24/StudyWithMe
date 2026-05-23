package com.studywithme.auth.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.token.TokenPair;
import com.studywithme.auth.token.TokenService;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.member.application.MemberProfileService;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final MemberRepository memberRepository;
	private final MemberProfileService memberProfileService;
	private final TokenService tokenService;
	private final RefreshTokenCookieWriter refreshTokenCookieWriter;

	public AuthController(
		MemberRepository memberRepository,
		MemberProfileService memberProfileService,
		TokenService tokenService,
		RefreshTokenCookieWriter refreshTokenCookieWriter
	) {
		this.memberRepository = memberRepository;
		this.memberProfileService = memberProfileService;
		this.tokenService = tokenService;
		this.refreshTokenCookieWriter = refreshTokenCookieWriter;
	}

	@GetMapping("/me")
	public ApiResponse<AuthMeResponse> me(@AuthenticationPrincipal AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		Member member = memberRepository.findById(principal.memberId())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN));
		return ApiResponse.success(AuthMeResponse.from(member));
	}

	@PutMapping("/me/nickname")
	public ApiResponse<AuthMeResponse> updateNickname(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@RequestBody NicknameUpdateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		Member member = memberProfileService.updateNickname(authenticatedPrincipal.memberId(), request.nickname());
		return ApiResponse.success(AuthMeResponse.from(member));
	}

	@PostMapping("/refresh")
	public ApiResponse<AccessTokenResponse> refresh(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		String refreshToken = refreshTokenCookieWriter.read(request)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		TokenPair tokenPair = tokenService.refresh(refreshToken);
		refreshTokenCookieWriter.write(response, tokenPair);
		return ApiResponse.success(AccessTokenResponse.from(tokenPair));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		refreshTokenCookieWriter.read(request)
			.ifPresent(tokenService::revoke);
		refreshTokenCookieWriter.clear(response);
		return ApiResponse.success(null);
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
