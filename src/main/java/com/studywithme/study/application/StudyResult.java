package com.studywithme.study.application;

import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyStatus;
import java.time.LocalDateTime;

public record StudyResult(
	Long id,
	Long ownerMemberId,
	String title,
	String description,
	StudyStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static StudyResult from(Study study) {
		return new StudyResult(
			study.getId(),
			study.getOwnerMemberId(),
			study.getTitle(),
			study.getDescription(),
			study.getStatus(),
			study.getCreatedAt(),
			study.getUpdatedAt()
		);
	}
}
