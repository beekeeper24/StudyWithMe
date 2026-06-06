package com.studywithme.report.application;

import com.studywithme.member.domain.Member;
import com.studywithme.report.domain.MemberSanction;
import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.domain.MemberSanctionType;
import java.time.LocalDateTime;
import java.util.Map;

public record MemberSanctionResult(
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

	public static MemberSanctionResult from(MemberSanction sanction, Map<Long, Member> members) {
		return new MemberSanctionResult(
			sanction.getId(),
			sanction.getTargetMemberId(),
			nicknameOf(members, sanction.getTargetMemberId()),
			sanction.getAdminMemberId(),
			nicknameOf(members, sanction.getAdminMemberId()),
			sanction.getType(),
			sanction.getReason(),
			sanction.getSourceType(),
			sanction.getSourceId(),
			sanction.getCreatedAt()
		);
	}

	private static String nicknameOf(Map<Long, Member> members, Long memberId) {
		Member member = members.get(memberId);
		return member == null ? null : member.getNickname();
	}
}
