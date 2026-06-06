package com.studywithme.report.presentation;

import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.domain.MemberSanctionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberSanctionCreateRequest(
	@NotNull(message = "제재 대상 회원을 선택해 주세요.")
	Long targetMemberId,
	@NotNull(message = "제재 유형을 선택해 주세요.")
	MemberSanctionType type,
	@NotBlank(message = "제재 사유를 입력해 주세요.")
	@Size(max = 500, message = "제재 사유는 500자 이하여야 합니다.")
	String reason,
	MemberSanctionSourceType sourceType,
	Long sourceId
) {
}
