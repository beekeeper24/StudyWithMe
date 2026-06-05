package com.studywithme.report.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ContentReportErrorCode implements ErrorCode {
	CONTENT_REPORT_NOT_FOUND(
		"REPORT-001",
		"신고를 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	CANNOT_REPORT_OWN_CONTENT(
		"REPORT-002",
		"자신이 작성한 콘텐츠는 신고할 수 없습니다.",
		HttpStatus.CONFLICT
	),
	CONTENT_REPORT_DUPLICATED(
		"REPORT-003",
		"이미 신고한 콘텐츠입니다.",
		HttpStatus.CONFLICT
	),
	CONTENT_REPORT_ADMIN_REQUIRED(
		"REPORT-004",
		"콘텐츠 신고 관리는 관리자만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	INVALID_CONTENT_REPORT_STATUS(
		"REPORT-005",
		"신고 처리 상태가 올바르지 않습니다.",
		HttpStatus.BAD_REQUEST
	),
	CONTENT_REPORT_ALREADY_HANDLED(
		"REPORT-006",
		"이미 다른 관리자가 처리한 신고입니다.",
		HttpStatus.CONFLICT
	),
	CONTENT_REPORT_ALREADY_ASSIGNED(
		"REPORT-007",
		"이미 다른 관리자가 담당 중인 신고입니다.",
		HttpStatus.CONFLICT
	),
	CONTENT_REPORT_ASSIGNEE_REQUIRED(
		"REPORT-008",
		"담당 관리자만 신고를 처리할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	INVALID_CONTENT_REPORT_MODERATION_ACTION(
		"REPORT-009",
		"신고 처리 액션이 올바르지 않습니다.",
		HttpStatus.BAD_REQUEST
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	ContentReportErrorCode(String code, String message, HttpStatus status) {
		this.code = code;
		this.message = message;
		this.status = status;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}
}
