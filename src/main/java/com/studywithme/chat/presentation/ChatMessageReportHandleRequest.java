package com.studywithme.chat.presentation;

import com.studywithme.chat.domain.ChatMessageReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageReportHandleRequest(
	@NotNull(message = "처리 상태를 선택해 주세요.")
	ChatMessageReportStatus status,
	@Size(max = 500, message = "처리 메모는 500자 이하여야 합니다.")
	String handlingNote
) {
}
