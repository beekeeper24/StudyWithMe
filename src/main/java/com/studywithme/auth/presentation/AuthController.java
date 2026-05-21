package com.studywithme.auth.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final MemberRepository memberRepository;

	public AuthController(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
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
}
