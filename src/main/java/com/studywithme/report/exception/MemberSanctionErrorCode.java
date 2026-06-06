package com.studywithme.report.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberSanctionErrorCode implements ErrorCode {
	MEMBER_SANCTION_TARGET_NOT_FOUND(
		"SANCTION-001",
		"제재 대상 회원을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	MEMBER_SANCTION_ADMIN_REQUIRED(
		"SANCTION-002",
		"회원 제재 관리는 관리자만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	MemberSanctionErrorCode(String code, String message, HttpStatus status) {
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
