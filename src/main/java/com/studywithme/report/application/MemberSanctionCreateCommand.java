package com.studywithme.report.application;

import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.domain.MemberSanctionType;

public record MemberSanctionCreateCommand(
	Long targetMemberId,
	MemberSanctionType type,
	String reason,
	MemberSanctionSourceType sourceType,
	Long sourceId
) {
}
