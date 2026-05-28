package com.studywithme.study.application;

import java.time.LocalDateTime;

public record StudyJoinRequestResult(
	Long memberId,
	String nickname,
	String profileImageUrl,
	LocalDateTime requestedAt
) {
}
