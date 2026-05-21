package com.studywithme.study.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyMemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class StudyRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private StudyRepository studyRepository;

	@Autowired
	private StudyMemberRepository studyMemberRepository;

	@Test
	@DisplayName("스터디와 모집장 참여 정보를 저장한다")
	void saveStudyAndOwnerMembership() {
		Member owner = memberRepository.saveAndFlush(Member.createOAuthMember(
			"owner@example.com",
			"owner",
			OAuthProvider.GOOGLE,
			"google-owner",
			null
		));
		Study study = studyRepository.saveAndFlush(Study.create(
			"알고리즘 스터디",
			"매주 알고리즘 문제를 풀고 리뷰합니다.",
			owner.getId()
		));

		StudyMember studyMember = studyMemberRepository.saveAndFlush(
			StudyMember.owner(study.getId(), owner.getId())
		);

		assertThat(study.getId()).isNotNull();
		assertThat(study.getOwnerMemberId()).isEqualTo(owner.getId());
		assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.OWNER);
	}
}
