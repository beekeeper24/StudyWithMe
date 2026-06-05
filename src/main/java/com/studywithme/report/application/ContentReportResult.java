package com.studywithme.report.application;

import com.studywithme.member.domain.Member;
import com.studywithme.report.domain.ContentReport;
import com.studywithme.report.domain.ContentReportStatus;
import com.studywithme.report.domain.ContentReportTargetType;
import java.time.LocalDateTime;
import java.util.Map;

public record ContentReportResult(
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
	Long assignedAdminMemberId,
	String assignedAdminNickname,
	LocalDateTime assignedAt,
	Long handlerMemberId,
	String handlerNickname,
	String handlingNote,
	LocalDateTime createdAt,
	LocalDateTime handledAt
) {

	public static ContentReportResult from(ContentReport report, Map<Long, Member> members) {
		return new ContentReportResult(
			report.getId(),
			report.getTargetType(),
			report.getTargetId(),
			report.getPostId(),
			report.getTargetTitle(),
			report.getTargetContent(),
			report.getReporterMemberId(),
			nicknameOf(members, report.getReporterMemberId()),
			report.getReportedMemberId(),
			nicknameOf(members, report.getReportedMemberId()),
			report.getReason(),
			report.getStatus(),
			report.getAssignedAdminMemberId(),
			nicknameOf(members, report.getAssignedAdminMemberId()),
			report.getAssignedAt(),
			report.getHandlerMemberId(),
			nicknameOf(members, report.getHandlerMemberId()),
			report.getHandlingNote(),
			report.getCreatedAt(),
			report.getHandledAt()
		);
	}

	private static String nicknameOf(Map<Long, Member> members, Long memberId) {
		if (memberId == null) {
			return null;
		}
		Member member = members.get(memberId);
		return member == null ? null : member.getNickname();
	}
}
