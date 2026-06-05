package com.studywithme.report.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.report.application.ContentReportService;
import com.studywithme.report.domain.ContentReportStatus;
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
public class ContentReportController {

	private final ContentReportService contentReportService;

	public ContentReportController(ContentReportService contentReportService) {
		this.contentReportService = contentReportService;
	}

	@PostMapping("/api/v1/posts/{postId}/reports")
	public ApiResponse<ContentReportResponse> reportPost(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody ContentReportRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ContentReportResponse.fromReportCreation(contentReportService.reportPost(
			postId,
			authenticatedPrincipal.memberId(),
			request.reason()
		)));
	}

	@PostMapping("/api/v1/comments/{commentId}/reports")
	public ApiResponse<ContentReportResponse> reportComment(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody ContentReportRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ContentReportResponse.fromReportCreation(contentReportService.reportComment(
			commentId,
			authenticatedPrincipal.memberId(),
			request.reason()
		)));
	}

	@GetMapping("/api/v1/admin/content-reports")
	public ApiResponse<List<ContentReportResponse>> findReports(
		@RequestParam(required = false) ContentReportStatus status,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(contentReportService.findReports(authenticatedPrincipal.memberId(), status).stream()
			.map(ContentReportResponse::from)
			.toList());
	}

	@PostMapping("/api/v1/admin/content-reports/{reportId}/assign")
	public ApiResponse<ContentReportResponse> assignReport(
		@PathVariable Long reportId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ContentReportResponse.from(contentReportService.assignReport(
			reportId,
			authenticatedPrincipal.memberId()
		)));
	}

	@PostMapping("/api/v1/admin/content-reports/{reportId}/handle")
	public ApiResponse<ContentReportResponse> handleReport(
		@PathVariable Long reportId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody ContentReportHandleRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ContentReportResponse.from(contentReportService.handleReport(
			reportId,
			authenticatedPrincipal.memberId(),
			request.status(),
			request.handlingNote()
		)));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
