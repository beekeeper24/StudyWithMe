package com.studywithme.global.exception;

public record FieldErrorDetail(
        String field,
        String message,
        Object rejectedValue
) {
}
