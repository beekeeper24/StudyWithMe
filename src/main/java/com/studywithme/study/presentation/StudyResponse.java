package com.studywithme.study.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studywithme.study.application.StudyResult;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudyResponse(
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
	String schedule,
	String status,
	boolean joinedByRequester,
	boolean ownedByRequester,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static StudyResponse from(StudyResult result) {
		return new StudyResponse(
			result.id(),
			result.ownerMemberId(),
			result.ownerNickname(),
			result.ownerProfileImageUrl(),
			result.title(),
			result.description(),
			result.progressMethod(),
			result.targetAudience(),
			result.rules(),
			result.capacity(),
			result.schedule(),
			result.status().name(),
			result.joinedByRequester(),
			result.ownedByRequester(),
			result.createdAt(),
			result.updatedAt()
		);
	}
}
