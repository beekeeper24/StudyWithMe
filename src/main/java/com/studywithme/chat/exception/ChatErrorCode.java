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
	),
	STUDY_CHAT_ROOM_CLOSED(
		"CHAT-005",
		"종료된 스터디 채팅방에는 메시지를 보낼 수 없습니다.",
		HttpStatus.CONFLICT
	),
	CHAT_MESSAGE_NOT_FOUND(
		"CHAT-006",
		"채팅 메시지를 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	NOT_CHAT_MESSAGE_SENDER(
		"CHAT-007",
		"메시지 작성자만 삭제할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	CANNOT_REPORT_OWN_MESSAGE(
		"CHAT-008",
		"자신이 보낸 메시지는 신고할 수 없습니다.",
		HttpStatus.CONFLICT
	),
	CHAT_MESSAGE_REPORT_DUPLICATED(
		"CHAT-009",
		"이미 신고한 메시지입니다.",
		HttpStatus.CONFLICT
	),
	CHAT_REPORT_ADMIN_REQUIRED(
		"CHAT-010",
		"채팅 신고 관리는 관리자만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	DELETED_CHAT_MESSAGE_REPORT_NOT_ALLOWED(
		"CHAT-011",
		"삭제된 메시지는 신고할 수 없습니다.",
		HttpStatus.CONFLICT
	),
	INVALID_CHAT_REPORT_STATUS(
		"CHAT-012",
		"신고 처리 상태가 올바르지 않습니다.",
		HttpStatus.BAD_REQUEST
	),
	CHAT_REPORT_ALREADY_HANDLED(
		"CHAT-013",
		"이미 다른 관리자가 처리한 신고입니다.",
		HttpStatus.CONFLICT
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
