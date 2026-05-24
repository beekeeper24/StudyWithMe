package com.studywithme.study.application;

public record StudyUpdateCommand(
	String title,
	String progressMethod,
	String targetAudience,
	String rules,
	Integer capacity,
	String schedule
) {
}
