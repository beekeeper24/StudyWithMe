package com.studywithme.report.presentation;

import com.studywithme.report.domain.ContentReportModerationAction;
import com.studywithme.report.domain.ContentReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentReportHandleRequest(
	@NotNull(message = "신고 처리 상태를 입력해 주세요.")
	ContentReportStatus status,
	ContentReportModerationAction moderationAction,
	@Size(max = 500, message = "처리 메모는 500자 이하여야 합니다.")
	String handlingNote
) {
	public ContentReportHandleRequest(ContentReportStatus status, String handlingNote) {
		this(status, ContentReportModerationAction.NONE, handlingNote);
	}
}
