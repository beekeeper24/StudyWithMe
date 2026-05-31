package com.studywithme.study.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyMemberRole;
import com.studywithme.study.domain.StudyStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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

	@Test
	@DisplayName("모집 중인 스터디는 제목과 진행 정보로 검색할 수 있다")
	void searchRecruitingStudies() {
		Member owner = memberRepository.saveAndFlush(Member.createOAuthMember(
			"owner@example.com",
			"owner",
			OAuthProvider.GOOGLE,
			"google-owner",
			null
		));
		Study matchedByTitle = studyRepository.save(Study.create(
			"React 집중 스터디",
			"매주 과제를 진행합니다.",
			owner.getId()
		));
		Study matchedByProgress = studyRepository.save(Study.create(
			"프론트엔드 스터디",
			"화면 구현",
			"React 과제를 함께 풉니다.",
			"입문자",
			"인증 필수",
			6,
			"매주 화요일",
			owner.getId()
		));
		Study other = studyRepository.save(Study.create(
			"Java 스터디",
			"백엔드 기초",
			owner.getId()
		));
		studyRepository.flush();

		List<Study> studies = studyRepository.searchAllByStatus(
			StudyStatus.RECRUITING,
			"react",
			PageRequest.of(0, 50)
		);

		assertThat(studies).extracting(Study::getId)
			.containsExactly(matchedByProgress.getId(), matchedByTitle.getId())
			.doesNotContain(other.getId());
	}
}
