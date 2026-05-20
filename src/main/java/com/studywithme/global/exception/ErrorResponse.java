package com.studywithme.global.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        boolean success,
        ErrorDetail error,
        LocalDateTime timestamp,
        String path
) {

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode, null, path);
    }

    public static ErrorResponse of(ErrorCode errorCode, Object detail, String path) {
        return new ErrorResponse(
                false,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), detail),
                LocalDateTime.now(),
                path
        );
    }

    public record ErrorDetail(
            String code,
            String message,
            Object detail
    ) {
    }
}
