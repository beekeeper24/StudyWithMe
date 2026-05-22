package com.studywithme.mention.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MentionExtractorTest {

	private final MentionExtractor mentionExtractor = new MentionExtractor();

	@Test
	@DisplayName("댓글 본문에서 @nickname을 등장 순서대로 중복 없이 추출한다")
	void extractUniqueMentionNicknames() {
		assertThat(mentionExtractor.extract("@alice 확인해주세요 @bob @alice"))
			.containsExactly("alice", "bob");
	}

	@Test
	@DisplayName("@ 뒤에 닉네임 문자가 없으면 멘션으로 보지 않는다")
	void ignoresDanglingAtSign() {
		assertThat(mentionExtractor.extract("@ @alice email@example.com"))
			.containsExactly("alice");
	}
}
