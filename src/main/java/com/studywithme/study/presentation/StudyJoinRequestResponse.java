package com.studywithme.study.presentation;

import com.studywithme.study.application.StudyJoinRequestResult;
import java.time.LocalDateTime;

public record StudyJoinRequestResponse(
	Long memberId,
	String nickname,
	String profileImageUrl,
	LocalDateTime requestedAt
) {

	public static StudyJoinRequestResponse from(StudyJoinRequestResult result) {
		return new StudyJoinRequestResponse(
			result.memberId(),
			result.nickname(),
			result.profileImageUrl(),
			result.requestedAt()
		);
	}
}
