package com.studywithme.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증하지 않고 내 정보를 조회하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedMe() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("Bearer access token으로 인증한 회원의 내 정보를 조회한다")
	void getMeWithBearerToken() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"bee@example.com",
			"beekeeper",
			OAuthProvider.GOOGLE,
			"google-123",
			"https://example.com/profile.png"
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(member.getId()))
			.andExpect(jsonPath("$.data.email").value("bee@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("beekeeper"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.roles[0]").value("USER"));
	}

	@Test
	@DisplayName("명시적으로 허용하지 않은 API 경로는 기본 차단한다")
	void denyUnknownApiByDefault() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"unknown@example.com",
			"unknown-user",
			OAuthProvider.GOOGLE,
			"google-unknown",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(get("/api/v1/not-yet-defined")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("세션 인증 정보만으로는 내 정보를 조회할 수 없다")
	void rejectSessionOnlyAuthentication() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"session@example.com",
			"session-user",
			OAuthProvider.GOOGLE,
			"google-session",
			null
		));
		UsernamePasswordAuthenticationToken sessionAuthentication = new UsernamePasswordAuthenticationToken(
			new AuthenticatedMemberPrincipal(member.getId(), Set.of("USER")),
			null,
			Set.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(
			HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
			new SecurityContextImpl(sessionAuthentication)
		);

		mockMvc.perform(get("/api/v1/auth/me").session(session))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}
}
