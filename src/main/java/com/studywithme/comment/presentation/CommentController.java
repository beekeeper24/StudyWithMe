package com.studywithme.comment.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentService;
import com.studywithme.comment.application.CommentUpdateCommand;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@PostMapping("/posts/{postId}/comments")
	public ApiResponse<CommentResponse> create(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody CommentRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		CommentCreateCommand command = new CommentCreateCommand(request.content());
		return ApiResponse.success(CommentResponse.from(
			commentService.create(postId, authenticatedPrincipal.memberId(), command)
		));
	}

	@GetMapping("/posts/{postId}/comments")
	public ApiResponse<List<CommentResponse>> findAllByPostId(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		Long requesterMemberId = principal == null ? null : principal.memberId();
		return ApiResponse.success(commentService.findAllByPostId(postId, requesterMemberId).stream()
			.map(CommentResponse::from)
			.toList());
	}

	@PostMapping("/comments/{commentId}/replies")
	public ApiResponse<CommentResponse> reply(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody CommentRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		CommentCreateCommand command = new CommentCreateCommand(request.content());
		return ApiResponse.success(CommentResponse.from(
			commentService.reply(commentId, authenticatedPrincipal.memberId(), command)
		));
	}

	@PutMapping("/comments/{commentId}")
	public ApiResponse<CommentResponse> update(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody CommentRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		CommentUpdateCommand command = new CommentUpdateCommand(request.content());
		return ApiResponse.success(CommentResponse.from(
			commentService.update(commentId, authenticatedPrincipal.memberId(), command)
		));
	}

	@DeleteMapping("/comments/{commentId}")
	public ApiResponse<CommentResponse> delete(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(CommentResponse.from(
			commentService.delete(commentId, authenticatedPrincipal.memberId())
		));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
