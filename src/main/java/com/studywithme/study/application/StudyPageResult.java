package com.studywithme.study.application;

import java.util.List;

public record StudyPageResult(
	List<StudyResult> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext,
	boolean hasPrevious
) {
}
