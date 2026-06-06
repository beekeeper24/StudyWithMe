package com.studywithme.report.presentation;

import com.studywithme.report.domain.MemberSanctionSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberSanctionRestoreRequest(
	@NotBlank(message = "복구 사유를 입력해 주세요.")
	@Size(max = 500, message = "복구 사유는 500자 이하여야 합니다.")
	String reason,
	MemberSanctionSourceType sourceType,
	Long sourceId
) {
}
