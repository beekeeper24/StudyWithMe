package com.studywithme.post.presentation;

import com.studywithme.post.application.PostPageResult;
import java.util.List;

public record PostPageResponse(
	List<PostResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext,
	boolean hasPrevious
) {

	public static PostPageResponse from(PostPageResult result) {
		return new PostPageResponse(
			result.content().stream()
				.map(PostResponse::from)
				.toList(),
			result.page(),
			result.size(),
			result.totalElements(),
			result.totalPages(),
			result.hasNext(),
			result.hasPrevious()
		);
	}
}
