package com.studywithme.study.presentation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@Size(max = 2000) String description,
	@Size(max = 500) String progressMethod,
	@Size(max = 500) String targetAudience,
	@Size(max = 1000) String rules,
	@Min(1) Integer capacity,
	@Size(max = 200) String schedule
) {

	@AssertTrue
	public boolean hasRecruitmentInfo() {
		return hasText(description) || (
			hasText(progressMethod)
				&& hasText(targetAudience)
				&& hasText(rules)
				&& capacity != null
				&& hasText(schedule)
		);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
