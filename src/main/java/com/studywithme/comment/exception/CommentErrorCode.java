package com.studywithme.comment.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommentErrorCode implements ErrorCode {
	COMMENT_NOT_FOUND(
		"COMMENT-001",
		"댓글을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	NOT_COMMENT_AUTHOR(
		"COMMENT-002",
		"댓글 작성자만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	),
	NESTED_REPLY_NOT_ALLOWED(
		"COMMENT-003",
		"답글에는 다시 답글을 작성할 수 없습니다.",
		HttpStatus.CONFLICT
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	CommentErrorCode(String code, String message, HttpStatus status) {
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
