package com.studywithme.member.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {
	INVALID_NICKNAME(
		"MEMBER-001",
		"별명은 2~20자의 한글, 영문, 숫자, 밑줄만 사용할 수 있습니다.",
		HttpStatus.BAD_REQUEST
	),
	DUPLICATED_NICKNAME(
		"MEMBER-002",
		"이미 사용 중인 별명입니다.",
		HttpStatus.CONFLICT
	),
	MEMBER_NOT_FOUND(
		"MEMBER-003",
		"회원을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	NICKNAME_REQUIRED(
		"MEMBER-004",
		"별명 설정이 필요합니다.",
		HttpStatus.CONFLICT
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	MemberErrorCode(String code, String message, HttpStatus status) {
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
