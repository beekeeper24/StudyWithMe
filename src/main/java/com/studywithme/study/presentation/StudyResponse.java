package com.studywithme.study.presentation;

import com.studywithme.study.application.StudyResult;
import java.time.LocalDateTime;

public record StudyResponse(
	Long id,
	Long ownerMemberId,
	String title,
	String description,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static StudyResponse from(StudyResult result) {
		return new StudyResponse(
			result.id(),
			result.ownerMemberId(),
			result.title(),
			result.description(),
			result.status().name(),
			result.createdAt(),
			result.updatedAt()
		);
	}
}
