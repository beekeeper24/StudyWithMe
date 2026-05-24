package com.studywithme.study.application;

public record StudyCreateCommand(
	String title,
	String description,
	String progressMethod,
	String targetAudience,
	String rules,
	Integer capacity,
	String schedule
) {

	public StudyCreateCommand(String title, String description) {
		this(title, description, null, null, null, null, null);
	}
}
