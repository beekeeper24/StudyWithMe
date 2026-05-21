package com.studywithme.study.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank @Size(max = 2000) String description
) {
}
