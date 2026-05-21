package com.studywithme.auth.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
	UNSUPPORTED_OAUTH_PROVIDER(
		"AUTH-001",
		"지원하지 않는 OAuth 제공자입니다.",
		HttpStatus.BAD_REQUEST
	),
	INVALID_OAUTH_ATTRIBUTES(
		"AUTH-002",
		"OAuth 사용자 정보가 올바르지 않습니다.",
		HttpStatus.BAD_REQUEST
	),
	INVALID_ACCESS_TOKEN(
		"AUTH-003",
		"Access token이 올바르지 않습니다.",
		HttpStatus.UNAUTHORIZED
	),
	INVALID_REFRESH_TOKEN(
		"AUTH-004",
		"Refresh token이 올바르지 않습니다.",
		HttpStatus.UNAUTHORIZED
	),
	EXPIRED_REFRESH_TOKEN(
		"AUTH-005",
		"Refresh token이 만료되었습니다.",
		HttpStatus.UNAUTHORIZED
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	AuthErrorCode(String code, String message, HttpStatus status) {
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
