package com.studywithme.study.application;

import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyStatus;
import java.time.LocalDateTime;

public record StudyResult(
	Long id,
	Long ownerMemberId,
	String ownerNickname,
	String ownerProfileImageUrl,
	String title,
	String description,
	String progressMethod,
	String targetAudience,
	String rules,
	Integer capacity,
	long joinedMemberCount,
	String schedule,
	StudyStatus status,
	boolean joinedByRequester,
	boolean joinRequestedByRequester,
	boolean ownedByRequester,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static StudyResult from(Study study) {
		return from(study, null, null, 0, false, false, false);
	}

	public static StudyResult from(
		Study study,
		String ownerNickname,
		String ownerProfileImageUrl,
		long joinedMemberCount,
		boolean joinedByRequester,
		boolean joinRequestedByRequester,
		boolean ownedByRequester
	) {
		return new StudyResult(
			study.getId(),
			study.getOwnerMemberId(),
			ownerNickname,
			ownerProfileImageUrl,
			study.getTitle(),
			study.getDescription(),
			study.getProgressMethod(),
			study.getTargetAudience(),
			study.getRules(),
			study.getCapacity(),
			joinedMemberCount,
			study.getSchedule(),
			study.getStatus(),
			joinedByRequester,
			joinRequestedByRequester,
			ownedByRequester,
			study.getCreatedAt(),
			study.getUpdatedAt()
		);
	}
}
