package com.studywithme.chat.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ChatErrorCode implements ErrorCode {
	CHAT_ROOM_NOT_FOUND(
		"CHAT-001",
		"채팅방을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	NOT_CHAT_ROOM_MEMBER(
		"CHAT-002",
		"채팅방 참여자만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	INVALID_PRIVATE_ROOM_TARGET(
		"CHAT-003",
		"자기 자신과 1:1 채팅방을 만들 수 없습니다.",
		HttpStatus.CONFLICT
	),
	TARGET_MEMBER_NOT_FOUND(
		"CHAT-004",
		"채팅 대상 회원을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	ChatErrorCode(String code, String message, HttpStatus status) {
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
