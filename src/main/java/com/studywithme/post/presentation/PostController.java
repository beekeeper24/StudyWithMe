package com.studywithme.post.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostService;
import com.studywithme.post.application.PostUpdateCommand;
import com.studywithme.post.domain.PostBoardType;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/posts")
public class PostController {

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@PostMapping
	public ApiResponse<PostResponse> create(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody PostCreateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		PostCreateCommand command = new PostCreateCommand(
			request.boardType() == null ? PostBoardType.FREE : request.boardType(),
			request.title(),
			request.content()
		);
		return ApiResponse.success(PostResponse.from(
			postService.create(authenticatedPrincipal.memberId(), command)
		));
	}

	@GetMapping
	public ApiResponse<PostPageResponse> findAll(
		@RequestParam(required = false) PostBoardType boardType,
		@RequestParam(required = false) String keyword,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "50") int size,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		Long requesterMemberId = principal == null ? null : principal.memberId();
		return ApiResponse.success(PostPageResponse.from(
			postService.findPage(boardType, keyword, requesterMemberId, page, size)
		));
	}

	@GetMapping("/{postId}")
	public ApiResponse<PostResponse> findById(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		Long requesterMemberId = principal == null ? null : principal.memberId();
		return ApiResponse.success(PostResponse.from(postService.findById(postId, requesterMemberId)));
	}

	@PutMapping("/{postId}")
	public ApiResponse<PostResponse> update(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody PostUpdateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		PostUpdateCommand command = new PostUpdateCommand(request.title(), request.content());
		return ApiResponse.success(PostResponse.from(
			postService.update(postId, authenticatedPrincipal.memberId(), command)
		));
	}

	@DeleteMapping("/{postId}")
	public ApiResponse<PostResponse> delete(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(PostResponse.from(
			postService.delete(postId, authenticatedPrincipal.memberId())
		));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
