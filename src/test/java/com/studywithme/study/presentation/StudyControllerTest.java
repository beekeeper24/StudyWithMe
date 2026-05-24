package com.studywithme.study.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.application.StudyCreateCommand;
import com.studywithme.study.application.StudyResult;
import com.studywithme.study.application.StudyService;
import com.studywithme.study.domain.StudyMemberStatus;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StudyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private StudyRepository studyRepository;

	@Autowired
	private StudyMemberRepository studyMemberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private StudyService studyService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		studyMemberRepository.deleteAll();
		studyRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증하지 않고 스터디를 생성하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedCreateStudy() throws Exception {
		mockMvc.perform(post("/api/v1/studies")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "알고리즘 스터디",
						"description": "매주 알고리즘 문제를 풉니다."
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 스터디에 참여하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedJoinStudy() throws Exception {
		mockMvc.perform(post("/api/v1/studies/{studyId}/join", 1L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 스터디에서 나가면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedLeaveStudy() throws Exception {
		mockMvc.perform(post("/api/v1/studies/{studyId}/leave", 1L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 스터디 모집을 종료하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedCloseStudy() throws Exception {
		mockMvc.perform(post("/api/v1/studies/{studyId}/close", 1L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증한 회원은 스터디를 생성할 수 있다")
	void createStudyWithBearerToken() throws Exception {
		Member owner = saveMember("owner");

		mockMvc.perform(post("/api/v1/studies")
				.header("Authorization", "Bearer " + accessToken(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "알고리즘 스터디",
						"description": "매주 알고리즘 문제를 풉니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("알고리즘 스터디"))
			.andExpect(jsonPath("$.data.status").value("RECRUITING"))
			.andExpect(jsonPath("$.data.ownerMemberId").value(owner.getId()));
	}

	@Test
	@DisplayName("인증한 회원은 구조화된 모집 정보로 스터디를 생성할 수 있다")
	void createStudyWithStructuredRecruitmentFields() throws Exception {
		Member owner = saveMember("owner");

		mockMvc.perform(post("/api/v1/studies")
				.header("Authorization", "Bearer " + accessToken(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "알고리즘 스터디",
						"progressMethod": "매주 화요일 온라인으로 문제 풀이를 진행합니다.",
						"targetAudience": "백준 실버 이상, 꾸준히 참여 가능한 사람",
						"rules": "불참 시 전날 공유하고, 풀이 기록을 남깁니다.",
						"capacity": 6,
						"schedule": "매주 화요일 21:00"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("알고리즘 스터디"))
			.andExpect(jsonPath("$.data.progressMethod").value("매주 화요일 온라인으로 문제 풀이를 진행합니다."))
			.andExpect(jsonPath("$.data.targetAudience").value("백준 실버 이상, 꾸준히 참여 가능한 사람"))
			.andExpect(jsonPath("$.data.rules").value("불참 시 전날 공유하고, 풀이 기록을 남깁니다."))
			.andExpect(jsonPath("$.data.capacity").value(6))
			.andExpect(jsonPath("$.data.schedule").value("매주 화요일 21:00"));
	}

	@Test
	@DisplayName("스터디 정원은 1명 이상이어야 한다")
	void rejectCreateStudyWithZeroCapacity() throws Exception {
		Member owner = saveMember("owner");

		mockMvc.perform(post("/api/v1/studies")
				.header("Authorization", "Bearer " + accessToken(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "알고리즘 스터디",
						"description": "기존 소개",
						"progressMethod": "매주 화요일 온라인으로 문제 풀이를 진행합니다.",
						"targetAudience": "백준 실버 이상",
						"rules": "풀이 기록 필수",
						"capacity": 0,
						"schedule": "매주 화요일 21:00"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("GLOBAL-400"));
	}

	@Test
	@DisplayName("스터디 모집장은 스터디 모집 정보를 수정할 수 있다")
	void updateStudyByOwner() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand(
				"알고리즘 스터디",
				null,
				"매주 온라인 풀이",
				"백준 실버 이상",
				"풀이 인증 필수",
				6,
				"화요일 21:00"
			)
		);

		mockMvc.perform(put("/api/v1/studies/{studyId}", study.id())
				.header("Authorization", "Bearer " + accessToken(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "면접 대비 스터디",
						"progressMethod": "매주 토요일 모의 면접을 진행합니다.",
						"targetAudience": "백엔드 취업 준비생",
						"rules": "질문지를 미리 작성하고 피드백을 남깁니다.",
						"capacity": 4,
						"schedule": "매주 토요일 10:00"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(study.id()))
			.andExpect(jsonPath("$.data.title").value("면접 대비 스터디"))
			.andExpect(jsonPath("$.data.progressMethod").value("매주 토요일 모의 면접을 진행합니다."))
			.andExpect(jsonPath("$.data.targetAudience").value("백엔드 취업 준비생"))
			.andExpect(jsonPath("$.data.rules").value("질문지를 미리 작성하고 피드백을 남깁니다."))
			.andExpect(jsonPath("$.data.capacity").value(4))
			.andExpect(jsonPath("$.data.schedule").value("매주 토요일 10:00"))
			.andExpect(jsonPath("$.data.ownedByRequester").value(true));
	}

	@Test
	@DisplayName("스터디 모집장이 아닌 회원은 스터디 모집 정보를 수정할 수 없다")
	void rejectUpdateStudyByNonOwner() throws Exception {
		Member owner = saveMember("owner");
		Member nonOwner = saveMember("non-owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(put("/api/v1/studies/{studyId}", study.id())
				.header("Authorization", "Bearer " + accessToken(nonOwner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "면접 대비 스터디",
						"progressMethod": "매주 토요일 모의 면접을 진행합니다.",
						"targetAudience": "백엔드 취업 준비생",
						"rules": "질문지를 미리 작성하고 피드백을 남깁니다.",
						"capacity": 4,
						"schedule": "매주 토요일 10:00"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("STUDY-004"));
	}

	@Test
	@DisplayName("스터디 수정 시 정원은 1명 이상이어야 한다")
	void rejectUpdateStudyWithZeroCapacity() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(put("/api/v1/studies/{studyId}", study.id())
				.header("Authorization", "Bearer " + accessToken(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "면접 대비 스터디",
						"progressMethod": "매주 토요일 모의 면접을 진행합니다.",
						"targetAudience": "백엔드 취업 준비생",
						"rules": "질문지를 미리 작성하고 피드백을 남깁니다.",
						"capacity": 0,
						"schedule": "매주 토요일 10:00"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("GLOBAL-400"));
	}

	@Test
	@DisplayName("스터디 목록은 공개 조회할 수 있다")
	void listStudiesPublicly() throws Exception {
		Member owner = saveMember("owner");
		studyService.create(owner.getId(), new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다."));

		mockMvc.perform(get("/api/v1/studies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].ownerNickname").value("owner"))
			.andExpect(jsonPath("$.data[0].ownerProfileImageUrl").doesNotExist())
			.andExpect(jsonPath("$.data[0].joinedByRequester").value(false))
			.andExpect(jsonPath("$.data[0].ownedByRequester").value(false));
	}

	@Test
	@DisplayName("공개 스터디 목록은 모집 중인 스터디만 조회한다")
	void listStudiesPubliclyOnlyRecruiting() throws Exception {
		Member owner = saveMember("owner");
		StudyResult recruiting = studyService.create(
			owner.getId(),
			new StudyCreateCommand("모집 중인 스터디", "진행 중")
		);
		StudyResult closed = studyService.create(
			owner.getId(),
			new StudyCreateCommand("마감된 스터디", "종료")
		);
		studyService.close(closed.id(), owner.getId());

		mockMvc.perform(get("/api/v1/studies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].id").value(recruiting.id()));
	}

	@Test
	@DisplayName("인증한 회원은 마이페이지용 현재/지난 스터디 이력을 조회할 수 있다")
	void findMyStudies() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult active = studyService.create(
			owner.getId(),
			new StudyCreateCommand("참여 중인 스터디", "진행 중")
		);
		studyService.join(active.id(), participant.getId());
		StudyResult closed = studyService.create(
			owner.getId(),
			new StudyCreateCommand("마감된 스터디", "종료")
		);
		studyService.join(closed.id(), participant.getId());
		studyService.close(closed.id(), owner.getId());
		StudyResult left = studyService.create(
			owner.getId(),
			new StudyCreateCommand("나간 스터디", "이탈")
		);
		studyService.join(left.id(), participant.getId());
		studyService.leave(left.id(), participant.getId());

		mockMvc.perform(get("/api/v1/studies/me")
				.header("Authorization", "Bearer " + accessToken(participant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.activeStudies.length()").value(1))
			.andExpect(jsonPath("$.data.activeStudies[0].id").value(active.id()))
			.andExpect(jsonPath("$.data.pastStudies.length()").value(2));
	}

	@Test
	@DisplayName("인증하지 않고 내 스터디 이력을 조회하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedFindMyStudies() throws Exception {
		mockMvc.perform(get("/api/v1/studies/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증한 회원이 스터디 목록을 조회하면 자신의 가입 여부를 함께 반환한다")
	void listStudiesWithRequesterMembership() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult joinedStudy = studyService.create(
			owner.getId(),
			new StudyCreateCommand("참여한 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(joinedStudy.id(), participant.getId());

		mockMvc.perform(get("/api/v1/studies")
				.header("Authorization", "Bearer " + accessToken(participant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].joinedByRequester").value(true))
			.andExpect(jsonPath("$.data[0].ownedByRequester").value(false));
	}

	@Test
	@DisplayName("스터디 상세는 공개 조회할 수 있다")
	void getStudyPublicly() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(get("/api/v1/studies/{studyId}", study.id()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(study.id()))
			.andExpect(jsonPath("$.data.ownerNickname").value("owner"))
			.andExpect(jsonPath("$.data.joinedByRequester").value(false))
			.andExpect(jsonPath("$.data.ownedByRequester").value(false));
	}

	@Test
	@DisplayName("인증한 모집장이 스터디 상세를 조회하면 모집장 여부를 함께 반환한다")
	void getStudyWithRequesterOwnership() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(get("/api/v1/studies/{studyId}", study.id())
				.header("Authorization", "Bearer " + accessToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.joinedByRequester").value(true))
			.andExpect(jsonPath("$.data.ownedByRequester").value(true));
	}

	@Test
	@DisplayName("인증한 회원은 스터디에 참여할 수 있다")
	void joinStudyWithBearerToken() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/join", study.id())
				.header("Authorization", "Bearer " + accessToken(participant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(study.id()));
	}

	@Test
	@DisplayName("참여로 정원이 가득 차면 스터디 모집이 자동 마감되고 공개 목록에서 숨겨진다")
	void joinStudyClosesRecruitmentWhenCapacityBecomesFull() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand(
				"알고리즘 스터디",
				null,
				"온라인 풀이",
				"백준 실버 이상",
				"풀이 인증 필수",
				2,
				"화요일 21:00"
			)
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/join", study.id())
				.header("Authorization", "Bearer " + accessToken(participant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("CLOSED"));

		mockMvc.perform(get("/api/v1/studies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("정원이 가득 찬 스터디에는 참여할 수 없다")
	void rejectJoinStudyWhenCapacityIsFull() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		Member lateParticipant = saveMember("late-participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand(
				"알고리즘 스터디",
				null,
				"온라인 풀이",
				"백준 실버 이상",
				"풀이 인증 필수",
				2,
				"화요일 21:00"
			)
		);
		studyService.join(study.id(), participant.getId());

		mockMvc.perform(post("/api/v1/studies/{studyId}/join", study.id())
				.header("Authorization", "Bearer " + accessToken(lateParticipant)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("STUDY-007"));
	}

	@Test
	@DisplayName("정원 마감 후 참여자가 탈퇴해도 스터디는 공개 목록에 다시 나타나지 않는다")
	void leaveAfterCapacityClosedStudyKeepsStudyHiddenFromPublicList() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand(
				"알고리즘 스터디",
				null,
				"온라인 풀이",
				"백준 실버 이상",
				"풀이 인증 필수",
				2,
				"화요일 21:00"
			)
		);
		studyService.join(study.id(), participant.getId());

		mockMvc.perform(post("/api/v1/studies/{studyId}/leave", study.id())
				.header("Authorization", "Bearer " + accessToken(participant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("CLOSED"));

		mockMvc.perform(get("/api/v1/studies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("인증한 참여자는 스터디에서 나갈 수 있다")
	void leaveStudyByParticipantWithBearerToken() throws Exception {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());

		mockMvc.perform(post("/api/v1/studies/{studyId}/leave", study.id())
				.header("Authorization", "Bearer " + accessToken(participant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		assertThat(studyMemberRepository.findByStudyIdAndMemberId(study.id(), participant.getId()))
			.get()
			.extracting("status")
			.isEqualTo(StudyMemberStatus.LEFT);
	}

	@Test
	@DisplayName("스터디 모집장은 자신의 스터디에서 나갈 수 없다")
	void rejectLeaveStudyByOwner() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/leave", study.id())
				.header("Authorization", "Bearer " + accessToken(owner)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("STUDY-005"));
	}

	@Test
	@DisplayName("참여하지 않은 회원은 스터디에서 나갈 수 없다")
	void rejectLeaveStudyByNonMember() throws Exception {
		Member owner = saveMember("owner");
		Member nonMember = saveMember("non-member");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/leave", study.id())
				.header("Authorization", "Bearer " + accessToken(nonMember)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("STUDY-006"));
	}

	@Test
	@DisplayName("스터디 모집장은 스터디 모집을 종료할 수 있다")
	void closeStudyByOwnerWithBearerToken() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/close", study.id())
				.header("Authorization", "Bearer " + accessToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("CLOSED"));
	}

	@Test
	@DisplayName("스터디 모집장이 아닌 회원은 스터디 모집을 종료할 수 없다")
	void rejectCloseStudyByNonOwner() throws Exception {
		Member owner = saveMember("owner");
		Member nonOwner = saveMember("non-owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/close", study.id())
				.header("Authorization", "Bearer " + accessToken(nonOwner)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("STUDY-004"));
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

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}
