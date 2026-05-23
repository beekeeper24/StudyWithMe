package com.studywithme.study.application;

import java.util.List;

public record MyStudyHistoryResult(
	List<StudyResult> activeStudies,
	List<StudyResult> pastStudies
) {
}
