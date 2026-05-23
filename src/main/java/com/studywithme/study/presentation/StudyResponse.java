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
			result.status().name(),
			result.joinedByRequester(),
			result.ownedByRequester(),
			result.createdAt(),
			result.updatedAt()
		);
	}
}
