package com.studywithme.study.presentation;

import com.studywithme.study.application.MyStudyHistoryResult;
import java.util.List;

public record MyStudyHistoryResponse(
	List<StudyResponse> activeStudies,
	List<StudyResponse> pastStudies
) {

	public static MyStudyHistoryResponse from(MyStudyHistoryResult result) {
		return new MyStudyHistoryResponse(
			result.activeStudies().stream().map(StudyResponse::from).toList(),
			result.pastStudies().stream().map(StudyResponse::from).toList()
		);
	}
}
