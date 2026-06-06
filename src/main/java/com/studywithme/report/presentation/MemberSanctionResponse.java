package com.studywithme.report.presentation;

import com.studywithme.report.application.MemberSanctionResult;
import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.domain.MemberSanctionType;
import java.time.LocalDateTime;

public record MemberSanctionResponse(
	Long id,
	Long targetMemberId,
	String targetNickname,
	Long adminMemberId,
	String adminNickname,
	MemberSanctionType type,
	String reason,
	MemberSanctionSourceType sourceType,
	Long sourceId,
	LocalDateTime createdAt
) {

	public static MemberSanctionResponse from(MemberSanctionResult result) {
		return new MemberSanctionResponse(
			result.id(),
			result.targetMemberId(),
			result.targetNickname(),
			result.adminMemberId(),
			result.adminNickname(),
			result.type(),
			result.reason(),
			result.sourceType(),
			result.sourceId(),
			result.createdAt()
		);
	}
}
