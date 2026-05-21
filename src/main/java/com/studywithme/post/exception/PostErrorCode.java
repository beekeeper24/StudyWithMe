package com.studywithme.post.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PostErrorCode implements ErrorCode {
	POST_NOT_FOUND(
		"POST-001",
		"게시글을 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND
	),
	NOT_POST_AUTHOR(
		"POST-002",
		"게시글 작성자만 수행할 수 있습니다.",
		HttpStatus.FORBIDDEN
	);

	private final String code;
	private final String message;
	private final HttpStatus status;

	PostErrorCode(String code, String message, HttpStatus status) {
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
