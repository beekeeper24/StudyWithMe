package com.studywithme.report.application;

import com.studywithme.report.domain.MemberSanctionSourceType;

public record MemberSanctionRestoreCommand(
	Long targetMemberId,
	String reason,
	MemberSanctionSourceType sourceType,
	Long sourceId
) {
}
