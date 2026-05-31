package com.studywithme.study.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.exception.GlobalErrorCode;

public enum StudyHistoryScope {
	ACTIVE,
	PAST;

	public static StudyHistoryScope from(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return StudyHistoryScope.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE);
		}
	}
}
