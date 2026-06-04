package com.studywithme.chat.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageReportRequest(
	@NotBlank(message = "신고 사유를 입력해 주세요.")
	@Size(max = 500, message = "신고 사유는 500자 이하여야 합니다.")
	String reason
) {
}
