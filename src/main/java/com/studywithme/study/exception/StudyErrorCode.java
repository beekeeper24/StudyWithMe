package com.studywithme.study.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StudyErrorCode implements ErrorCode {
	STUDY_NOT_FOUND(
		"STUDY-001",
		"스터디를 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	STUDY_ALREADY_CLOSED(
		"STUDY-002",
		"이미 모집이 종료된 스터디입니다.",
		HttpStatus.CONFLICT
	),
	ALREADY_JOINED(
		"STUDY-003",
		"이미 참여한 스터디입니다.",
		HttpStatus.CONFLICT
	),
	NOT_STUDY_OWNER(
		"STUDY-004",
		"스터디 모집장만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	OWNER_CANNOT_LEAVE(
		"STUDY-005",
		"스터디 모집장은 탈퇴할 수 없습니다.",
		HttpStatus.CONFLICT
	),
	NOT_STUDY_MEMBER(
		"STUDY-006",
		"참여하지 않은 스터디입니다.",
		HttpStatus.CONFLICT
	),
	STUDY_CAPACITY_FULL(
		"STUDY-007",
		"스터디 정원이 마감되었습니다.",
		HttpStatus.CONFLICT
	),
	STUDY_ALREADY_ENDED(
		"STUDY-008",
		"이미 종료된 스터디입니다.",
		HttpStatus.CONFLICT
	),
	ALREADY_REQUESTED(
		"STUDY-009",
		"이미 참여 신청한 스터디입니다.",
		HttpStatus.CONFLICT
	),
	STUDY_HISTORY_NOT_PAST(
		"STUDY-010",
		"지난 스터디 기록만 삭제할 수 있습니다.",
		HttpStatus.CONFLICT
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	StudyErrorCode(String code, String message, HttpStatus status) {
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
