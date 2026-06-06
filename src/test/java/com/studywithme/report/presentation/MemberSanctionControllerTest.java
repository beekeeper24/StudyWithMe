package com.studywithme.report.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.domain.MemberSanctionType;
import com.studywithme.report.repository.MemberSanctionRepository;
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
class MemberSanctionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberSanctionRepository memberSanctionRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		memberSanctionRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("관리자는 회원 제재 이력을 기록할 수 있다")
	void createMemberSanctionByAdmin() throws Exception {
		Member target = saveMember("controller-sanction-target");
		Member admin = saveAdmin("controller-sanction-admin");

		mockMvc.perform(post("/api/v1/admin/member-sanctions")
				.header("Authorization", "Bearer " + accessToken(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new MemberSanctionCreateRequest(
					target.getId(),
					MemberSanctionType.WARNING,
					"신고 처리 후 경고",
					MemberSanctionSourceType.CONTENT_REPORT,
					30L
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.targetMemberId").value(target.getId()))
			.andExpect(jsonPath("$.data.targetNickname").value("controller-sanction-target"))
			.andExpect(jsonPath("$.data.adminMemberId").value(admin.getId()))
			.andExpect(jsonPath("$.data.adminNickname").value("controller-sanction-admin"))
			.andExpect(jsonPath("$.data.type").value("WARNING"))
			.andExpect(jsonPath("$.data.reason").value("신고 처리 후 경고"))
			.andExpect(jsonPath("$.data.sourceType").value("CONTENT_REPORT"))
			.andExpect(jsonPath("$.data.sourceId").value(30L));
	}

	@Test
	@DisplayName("관리자는 특정 회원의 제재 이력을 조회할 수 있다")
	void findMemberSanctionsByAdmin() throws Exception {
		Member target = saveMember("controller-history-target");
		Member admin = saveAdmin("controller-history-admin");
		mockMvc.perform(post("/api/v1/admin/member-sanctions")
				.header("Authorization", "Bearer " + accessToken(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new MemberSanctionCreateRequest(
					target.getId(),
					MemberSanctionType.WARNING,
					"이력 조회용",
					MemberSanctionSourceType.MANUAL,
					null
				))))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/member-sanctions")
				.param("targetMemberId", target.getId().toString())
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].targetMemberId").value(target.getId()))
			.andExpect(jsonPath("$.data[0].type").value("WARNING"));
	}

	@Test
	@DisplayName("일반 회원은 회원 제재 이력을 기록할 수 없다")
	void rejectCreateMemberSanctionByNonAdmin() throws Exception {
		Member target = saveMember("controller-non-admin-target");
		Member requester = saveMember("controller-non-admin-requester");

		mockMvc.perform(post("/api/v1/admin/member-sanctions")
				.header("Authorization", "Bearer " + accessToken(requester))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new MemberSanctionCreateRequest(
					target.getId(),
					MemberSanctionType.WARNING,
					"권한 없음",
					MemberSanctionSourceType.MANUAL,
					null
				))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("SANCTION-002"));
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

	private Member saveAdmin(String name) {
		Member member = Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		);
		member.grantRole(MemberRole.ADMIN);
		return memberRepository.saveAndFlush(member);
	}

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}
