package com.studywithme.study.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyMemberRole;
import com.studywithme.study.domain.StudyMemberStatus;
import com.studywithme.study.domain.StudyStatus;
import com.studywithme.study.exception.StudyErrorCode;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(StudyService.class)
class StudyServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private StudyRepository studyRepository;

	@Autowired
	private StudyMemberRepository studyMemberRepository;

	@Autowired
	private StudyService studyService;

	@Test
	@DisplayName("스터디를 생성하면 모집장 참여 정보가 OWNER 역할로 생성된다")
	void createStudyCreatesOwnerMembership() {
		Member owner = saveMember("owner");

		StudyResult result = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		StudyMember studyMember = studyMemberRepository
			.findByStudyIdAndMemberId(result.id(), owner.getId())
			.orElseThrow();
		assertThat(result.ownerMemberId()).isEqualTo(owner.getId());
		assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.OWNER);
	}

	@Test
	@DisplayName("스터디를 생성하면 구조화된 모집 정보를 결과에 포함한다")
	void createStudyReturnsStructuredRecruitmentFields() {
		Member owner = saveMember("owner");

		StudyResult result = studyService.create(
			owner.getId(),
			new StudyCreateCommand(
				"알고리즘 스터디",
				null,
				"매주 화요일 온라인 풀이",
				"백준 실버 이상",
				"풀이 기록 필수",
				6,
				"화요일 21:00"
			)
		);

		assertThat(result.progressMethod()).isEqualTo("매주 화요일 온라인 풀이");
		assertThat(result.targetAudience()).isEqualTo("백준 실버 이상");
		assertThat(result.rules()).isEqualTo("풀이 기록 필수");
		assertThat(result.capacity()).isEqualTo(6);
		assertThat(result.schedule()).isEqualTo("화요일 21:00");
		assertThat(result.description()).isEqualTo("매주 화요일 온라인 풀이");
	}

	@Test
	@DisplayName("스터디에 참여하면 참여자 정보가 MEMBER 역할로 생성된다")
	void joinStudyCreatesMemberMembership() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		StudyResult result = studyService.join(study.id(), participant.getId());

		StudyMember studyMember = studyMemberRepository
			.findByStudyIdAndMemberId(result.id(), participant.getId())
			.orElseThrow();
		assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.MEMBER);
	}

	@Test
	@DisplayName("이미 참여한 스터디에 다시 참여할 수 없다")
	void joinStudyTwiceThrowsAlreadyJoined() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());

		assertThatThrownBy(() -> studyService.join(study.id(), participant.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(StudyErrorCode.ALREADY_JOINED);
	}

	@Test
	@DisplayName("스터디 모집장은 자신의 스터디에서 나갈 수 없다")
	void ownerCannotLeaveOwnStudy() {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		assertThatThrownBy(() -> studyService.leave(study.id(), owner.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(StudyErrorCode.OWNER_CANNOT_LEAVE);
	}

	@Test
	@DisplayName("참여자는 스터디에서 나가면 참여 이력이 LEFT 상태로 남는다")
	void participantCanLeaveStudy() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());

		studyService.leave(study.id(), participant.getId());

		StudyMember studyMember = studyMemberRepository
			.findByStudyIdAndMemberId(study.id(), participant.getId())
			.orElseThrow();
		assertThat(studyMember.getStatus()).isEqualTo(StudyMemberStatus.LEFT);
		assertThat(studyMember.getLeftAt()).isNotNull();
	}

	@Test
	@DisplayName("나갔던 모집 중 스터디에는 다시 참여할 수 있다")
	void rejoinLeftRecruitingStudy() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());
		studyService.leave(study.id(), participant.getId());

		studyService.join(study.id(), participant.getId());

		StudyMember studyMember = studyMemberRepository
			.findByStudyIdAndMemberId(study.id(), participant.getId())
			.orElseThrow();
		assertThat(studyMember.getStatus()).isEqualTo(StudyMemberStatus.JOINED);
		assertThat(studyMember.getLeftAt()).isNull();
	}

	@Test
	@DisplayName("참여하지 않은 회원은 스터디에서 나갈 수 없다")
	void nonMemberCannotLeaveStudy() {
		Member owner = saveMember("owner");
		Member nonMember = saveMember("non-member");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		assertThatThrownBy(() -> studyService.leave(study.id(), nonMember.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(StudyErrorCode.NOT_STUDY_MEMBER);
	}

	@Test
	@DisplayName("모집장이 아닌 사용자는 스터디를 닫을 수 없다")
	void nonOwnerCannotCloseStudy() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		assertThatThrownBy(() -> studyService.close(study.id(), participant.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(StudyErrorCode.NOT_STUDY_OWNER);
	}

	@Test
	@DisplayName("모집장은 스터디 모집 상태를 종료할 수 있다")
	void ownerCanCloseStudy() {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		studyService.close(study.id(), owner.getId());

		assertThat(studyRepository.findById(study.id()).orElseThrow().getStatus())
			.isEqualTo(StudyStatus.CLOSED);
	}

	@Test
	@DisplayName("모집이 종료된 스터디에는 참여할 수 없다")
	void cannotJoinClosedStudy() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.close(study.id(), owner.getId());

		assertThatThrownBy(() -> studyService.join(study.id(), participant.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(StudyErrorCode.STUDY_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("존재하지 않는 스터디를 조회하면 예외가 발생한다")
	void unknownStudyIdThrowsStudyNotFound() {
		assertThatThrownBy(() -> studyService.findById(999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(StudyErrorCode.STUDY_NOT_FOUND);
	}

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}
}
