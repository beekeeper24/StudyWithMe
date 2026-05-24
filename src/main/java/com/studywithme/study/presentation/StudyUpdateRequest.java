package com.studywithme.study.presentation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudyUpdateRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank @Size(max = 500) String progressMethod,
	@NotBlank @Size(max = 500) String targetAudience,
	@NotBlank @Size(max = 1000) String rules,
	@NotNull @Min(1) Integer capacity,
	@NotBlank @Size(max = 200) String schedule
) {
}
