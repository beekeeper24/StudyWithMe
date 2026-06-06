package com.studywithme.report.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.report.application.MemberSanctionCreateCommand;
import com.studywithme.report.application.MemberSanctionRestoreCommand;
import com.studywithme.report.application.MemberSanctionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberSanctionController {

	private final MemberSanctionService memberSanctionService;

	public MemberSanctionController(MemberSanctionService memberSanctionService) {
		this.memberSanctionService = memberSanctionService;
	}

	@PostMapping("/api/v1/admin/member-sanctions")
	public ApiResponse<MemberSanctionResponse> createSanction(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody MemberSanctionCreateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(MemberSanctionResponse.from(memberSanctionService.createSanction(
			authenticatedPrincipal.memberId(),
			new MemberSanctionCreateCommand(
				request.targetMemberId(),
				request.type(),
				request.reason(),
				request.sourceType(),
				request.sourceId()
			)
		)));
	}

	@PostMapping("/api/v1/admin/members/{targetMemberId}/restore")
	public ApiResponse<MemberSanctionResponse> restoreMember(
		@PathVariable Long targetMemberId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody MemberSanctionRestoreRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(MemberSanctionResponse.from(memberSanctionService.restoreMember(
			authenticatedPrincipal.memberId(),
			new MemberSanctionRestoreCommand(
				targetMemberId,
				request.reason(),
				request.sourceType(),
				request.sourceId()
			)
		)));
	}

	@GetMapping("/api/v1/admin/member-sanctions")
	public ApiResponse<List<MemberSanctionResponse>> findSanctions(
		@RequestParam Long targetMemberId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(memberSanctionService.findSanctions(authenticatedPrincipal.memberId(), targetMemberId)
			.stream()
			.map(MemberSanctionResponse::from)
			.toList());
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
