package com.studywithme.study.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.study.application.StudyCreateCommand;
import com.studywithme.study.application.StudyService;
import com.studywithme.study.application.StudyUpdateCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/studies")
public class StudyController {

	private final StudyService studyService;

	public StudyController(StudyService studyService) {
		this.studyService = studyService;
	}

	@PostMapping
	public ApiResponse<StudyResponse> create(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody StudyCreateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		StudyCreateCommand command = new StudyCreateCommand(
			request.title(),
			request.description(),
			request.progressMethod(),
			request.targetAudience(),
			request.rules(),
			request.capacity(),
			request.schedule()
		);
		return ApiResponse.success(StudyResponse.from(
			studyService.create(authenticatedPrincipal.memberId(), command)
		));
	}

	@PutMapping("/{studyId}")
	public ApiResponse<StudyResponse> update(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody StudyUpdateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		StudyUpdateCommand command = new StudyUpdateCommand(
			request.title(),
			request.progressMethod(),
			request.targetAudience(),
			request.rules(),
			request.capacity(),
			request.schedule()
		);
		return ApiResponse.success(StudyResponse.from(
			studyService.update(studyId, authenticatedPrincipal.memberId(), command)
		));
	}

	@GetMapping
	public ApiResponse<List<StudyResponse>> findAll(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		Long requesterMemberId = principal == null ? null : principal.memberId();
		return ApiResponse.success(studyService.findAll(requesterMemberId).stream()
			.map(StudyResponse::from)
			.toList());
	}

	@GetMapping("/me")
	public ApiResponse<MyStudyHistoryResponse> findMyStudies(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(MyStudyHistoryResponse.from(
			studyService.findMyStudies(authenticatedPrincipal.memberId())
		));
	}

	@GetMapping("/{studyId}")
	public ApiResponse<StudyResponse> findById(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		Long requesterMemberId = principal == null ? null : principal.memberId();
		return ApiResponse.success(StudyResponse.from(studyService.findById(studyId, requesterMemberId)));
	}

	@PostMapping("/{studyId}/join")
	public ApiResponse<StudyResponse> join(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.join(studyId, authenticatedPrincipal.memberId())
		));
	}

	@PostMapping("/{studyId}/leave")
	public ApiResponse<StudyResponse> leave(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.leave(studyId, authenticatedPrincipal.memberId())
		));
	}

	@PostMapping("/{studyId}/close")
	public ApiResponse<StudyResponse> close(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.close(studyId, authenticatedPrincipal.memberId())
		));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
