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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public ApiResponse<StudyPageResponse> findAll(
		@RequestParam(required = false) String keyword,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "50") int size,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		Long requesterMemberId = principal == null ? null : principal.memberId();
		return ApiResponse.success(StudyPageResponse.from(
			studyService.findPage(keyword, requesterMemberId, page, size)
		));
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

	@DeleteMapping("/me/history/{studyId}")
	public ApiResponse<Void> hideMyStudyHistory(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		studyService.hideMyStudyHistory(studyId, authenticatedPrincipal.memberId());
		return ApiResponse.success(null);
	}

	@DeleteMapping("/me/history")
	public ApiResponse<Void> hideAllMyPastStudyHistory(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		studyService.hideAllMyPastStudyHistory(authenticatedPrincipal.memberId());
		return ApiResponse.success(null);
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
			studyService.requestJoin(studyId, authenticatedPrincipal.memberId())
		));
	}

	@GetMapping("/{studyId}/join-requests")
	public ApiResponse<List<StudyJoinRequestResponse>> findJoinRequests(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(studyService.findJoinRequests(studyId, authenticatedPrincipal.memberId())
			.stream()
			.map(StudyJoinRequestResponse::from)
			.toList());
	}

	@PostMapping("/{studyId}/join-requests/{memberId}/approve")
	public ApiResponse<StudyResponse> approveJoinRequest(
		@PathVariable Long studyId,
		@PathVariable Long memberId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.approveJoinRequest(studyId, authenticatedPrincipal.memberId(), memberId)
		));
	}

	@PostMapping("/{studyId}/join-requests/{memberId}/reject")
	public ApiResponse<StudyResponse> rejectJoinRequest(
		@PathVariable Long studyId,
		@PathVariable Long memberId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.rejectJoinRequest(studyId, authenticatedPrincipal.memberId(), memberId)
		));
	}

	@PostMapping("/{studyId}/join-requests/cancel")
	public ApiResponse<StudyResponse> cancelJoinRequest(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.cancelJoinRequest(studyId, authenticatedPrincipal.memberId())
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

	@PostMapping("/{studyId}/end")
	public ApiResponse<StudyResponse> end(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.end(studyId, authenticatedPrincipal.memberId())
		));
	}

	@DeleteMapping("/{studyId}")
	public ApiResponse<StudyResponse> delete(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(StudyResponse.from(
			studyService.delete(studyId, authenticatedPrincipal.memberId())
		));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
