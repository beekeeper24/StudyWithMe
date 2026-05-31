package com.studywithme.study.presentation;

import com.studywithme.study.application.StudyPageResult;
import java.util.List;

public record StudyPageResponse(
	List<StudyResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext,
	boolean hasPrevious
) {

	public static StudyPageResponse from(StudyPageResult result) {
		return new StudyPageResponse(
			result.content().stream()
				.map(StudyResponse::from)
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
