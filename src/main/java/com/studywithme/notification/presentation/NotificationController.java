package com.studywithme.notification.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.notification.application.NotificationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public ApiResponse<List<NotificationResponse>> findMine(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(notificationService.findMine(authenticatedPrincipal.memberId()).stream()
			.map(NotificationResponse::from)
			.toList());
	}

	@PostMapping("/{notificationId}/read")
	public ApiResponse<NotificationResponse> markRead(
		@PathVariable Long notificationId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(NotificationResponse.from(
			notificationService.markRead(notificationId, authenticatedPrincipal.memberId())
		));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
