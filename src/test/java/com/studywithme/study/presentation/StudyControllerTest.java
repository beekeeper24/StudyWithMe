package com.studywithme.study.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

		assertThat(studyMemberRepository.existsByStudyIdAndMemberId(study.id(), participant.getId()))
			.isFalse();
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
