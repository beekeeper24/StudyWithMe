package com.studywithme.notification.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCode {
	NOTIFICATION_NOT_FOUND(
		"NOTIFICATION-001",
		"알림을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	NOT_NOTIFICATION_OWNER(
		"NOTIFICATION-002",
		"자신의 알림만 처리할 수 있습니다.",
		HttpStatus.FORBIDDEN
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	NotificationErrorCode(String code, String message, HttpStatus status) {
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
