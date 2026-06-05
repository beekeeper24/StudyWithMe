package com.studywithme.report.presentation;

import com.studywithme.report.application.ContentReportResult;
import com.studywithme.report.domain.ContentReportModerationAction;
import com.studywithme.report.domain.ContentReportStatus;
import com.studywithme.report.domain.ContentReportTargetType;
import java.time.LocalDateTime;

public record ContentReportResponse(
	Long id,
	ContentReportTargetType targetType,
	Long targetId,
	Long postId,
	String targetTitle,
	String targetContent,
	Long reporterMemberId,
	String reporterNickname,
	Long reportedMemberId,
	String reportedNickname,
	String reason,
	ContentReportStatus status,
	ContentReportModerationAction moderationAction,
	Long assignedAdminMemberId,
	String assignedAdminNickname,
	LocalDateTime assignedAt,
	Long handlerMemberId,
	String handlerNickname,
	String handlingNote,
	LocalDateTime createdAt,
	LocalDateTime handledAt
) {

	public static ContentReportResponse fromReportCreation(ContentReportResult result) {
		return from(result, false);
	}

	public static ContentReportResponse from(ContentReportResult result) {
		return from(result, true);
	}

	private static ContentReportResponse from(ContentReportResult result, boolean includeMemberContext) {
		return new ContentReportResponse(
			result.id(),
			result.targetType(),
			result.targetId(),
			result.postId(),
			result.targetTitle(),
			result.targetContent(),
			result.reporterMemberId(),
			includeMemberContext ? result.reporterNickname() : null,
			result.reportedMemberId(),
			includeMemberContext ? result.reportedNickname() : null,
			result.reason(),
			result.status(),
			result.moderationAction(),
			result.assignedAdminMemberId(),
			includeMemberContext ? result.assignedAdminNickname() : null,
			result.assignedAt(),
			result.handlerMemberId(),
			includeMemberContext ? result.handlerNickname() : null,
			result.handlingNote(),
			result.createdAt(),
			result.handledAt()
		);
	}
}
