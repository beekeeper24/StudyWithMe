package com.studywithme.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.auth.token.RefreshTokenHash;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.auth.application.OAuthLoginService;
import com.studywithme.auth.oauth.OAuth2UserProfile;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.TokenPair;
import com.studywithme.auth.token.TokenService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private OAuthLoginService oAuthLoginService;

	@AfterEach
	void tearDown() {
		refreshTokenRepository.deleteAll();
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
			.andExpect(jsonPath("$.data.nicknameRequired").value(false))
			.andExpect(jsonPath("$.data.termsAgreementRequired").value(false))
			.andExpect(jsonPath("$.data.signupRequired").value(false))
			.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.roles[0]").value("USER"));
	}

	@Test
	@DisplayName("별명이 없는 OAuth 신규 회원은 내 정보에서 별명 설정 필요 상태로 조회된다")
	void getMeWithNicknameRequired() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.GOOGLE,
			"google-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.nickname").isEmpty())
			.andExpect(jsonPath("$.data.nicknameRequired").value(true))
			.andExpect(jsonPath("$.data.termsAgreementRequired").value(true))
			.andExpect(jsonPath("$.data.signupRequired").value(true));
	}

	@Test
	@DisplayName("회원가입을 완료하지 않은 회원은 온보딩 허용 API 외 인증 API를 사용할 수 없다")
	void rejectAuthenticatedApiBeforeSignupCompletion() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.GOOGLE,
			"google-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(post("/api/v1/studies")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"알고리즘 스터디","description":"매주 문제를 풉니다."}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MEMBER-004"));
	}

	@Test
	@DisplayName("별명만 설정하고 약관에 동의하지 않은 회원도 앱 API를 사용할 수 없다")
	void rejectAuthenticatedApiBeforeTermsAgreement() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.GOOGLE,
			"google-newbie",
			null
		));
		member.updateNickname("스터디왕");
		memberRepository.flush();
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(post("/api/v1/studies")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"알고리즘 스터디","description":"매주 문제를 풉니다."}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MEMBER-004"));
	}

	@Test
	@DisplayName("신규 OAuth 회원은 별명과 필수 약관 동의를 함께 저장해 회원가입을 완료한다")
	void completeSignup() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.GOOGLE,
			"google-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(put("/api/v1/auth/me/signup")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nickname":"스터디왕","termsAgreed":true,"privacyPolicyAgreed":true}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.nickname").value("스터디왕"))
			.andExpect(jsonPath("$.data.nicknameRequired").value(false))
			.andExpect(jsonPath("$.data.termsAgreementRequired").value(false))
			.andExpect(jsonPath("$.data.signupRequired").value(false))
			.andExpect(jsonPath("$.data.termsVersion").value("2026-05-24"))
			.andExpect(jsonPath("$.data.privacyPolicyVersion").value("2026-05-24"));
	}

	@Test
	@DisplayName("필수 약관에 동의하지 않으면 회원가입을 완료할 수 없다")
	void rejectSignupWithoutTermsAgreement() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.GOOGLE,
			"google-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(put("/api/v1/auth/me/signup")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nickname":"스터디왕","termsAgreed":true,"privacyPolicyAgreed":false}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MEMBER-005"));
	}

	@Test
	@DisplayName("인증한 회원은 별명을 설정할 수 있다")
	void updateNickname() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.GOOGLE,
			"google-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(put("/api/v1/auth/me/nickname")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nickname":"스터디왕"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.nickname").value("스터디왕"))
			.andExpect(jsonPath("$.data.nicknameRequired").value(false));
	}

	@Test
	@DisplayName("이미 사용 중인 별명으로 설정할 수 없다")
	void rejectDuplicatedNickname() throws Exception {
		memberRepository.saveAndFlush(Member.createOAuthMember(
			"owner@example.com",
			"스터디왕",
			OAuthProvider.GOOGLE,
			"google-owner",
			null
		));
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.KAKAO,
			"kakao-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(put("/api/v1/auth/me/nickname")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nickname":"스터디왕"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MEMBER-002"));
	}

	@Test
	@DisplayName("별명은 공백 없이 한글, 영문, 숫자, 밑줄만 사용할 수 있다")
	void rejectInvalidNicknamePattern() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"newbie@example.com",
			null,
			OAuthProvider.KAKAO,
			"kakao-newbie",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(put("/api/v1/auth/me/nickname")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nickname":"스터디 왕"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MEMBER-001"));
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

	@Test
	@DisplayName("refresh token cookie로 access token을 재발급하고 refresh token을 회전한다")
	void refreshWithCookie() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"refresh@example.com",
			"refresh-user",
			OAuthProvider.GOOGLE,
			"google-refresh",
			null
		));
		TokenPair tokenPair = tokenService.issue(member);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new MockCookie("refreshToken", tokenPair.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		String setCookie = result.getResponse().getHeader("Set-Cookie");
		assertThat(setCookie).contains("refreshToken=");
		assertThat(setCookie).contains("HttpOnly");
		assertThat(setCookie).contains("SameSite=Lax");
		assertThat(responseBody).doesNotContain("refreshToken");
		assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenHash.sha256(tokenPair.refreshToken()))
			.orElseThrow()
			.isRotated()).isTrue();
	}

	@Test
	@DisplayName("refresh token cookie가 없으면 AUTH-004 응답을 반환한다")
	void rejectRefreshWithoutCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-004"));
	}

	@Test
	@DisplayName("logout 시 refresh token을 폐기하고 cookie를 삭제한다")
	void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"logout@example.com",
			"logout-user",
			OAuthProvider.GOOGLE,
			"google-logout",
			null
		));
		TokenPair tokenPair = tokenService.issue(member);

		String setCookie = mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(new MockCookie("refreshToken", tokenPair.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andReturn()
			.getResponse()
			.getHeader("Set-Cookie");

		assertThat(setCookie).contains("refreshToken=");
		assertThat(setCookie).contains("Max-Age=0");
		assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenHash.sha256(tokenPair.refreshToken()))
			.orElseThrow()
			.isRevoked()).isTrue();
	}

	@Test
	@DisplayName("회원탈퇴 시 계정을 익명화하고 refresh token을 폐기해 같은 OAuth 계정 재가입을 허용한다")
	void withdrawMemberAndAllowOAuthSignupAgain() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"withdraw@example.com",
			"withdraw-user",
			OAuthProvider.GOOGLE,
			"google-withdraw",
			null
		));
		TokenPair tokenPair = tokenService.issue(member);
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		String setCookie = mockMvc.perform(delete("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken)
				.cookie(new MockCookie("refreshToken", tokenPair.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andReturn()
			.getResponse()
			.getHeader("Set-Cookie");

		Member withdrawnMember = memberRepository.findById(member.getId()).orElseThrow();
		assertThat(setCookie).contains("refreshToken=");
		assertThat(setCookie).contains("Max-Age=0");
		assertThat(withdrawnMember.getStatus().name()).isEqualTo("WITHDRAWN");
		assertThat(withdrawnMember.getNickname()).isNull();
		assertThat(withdrawnMember.getEmail()).startsWith("withdrawn-");
		assertThat(withdrawnMember.getOauthSubject()).startsWith("withdrawn:");
		assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenHash.sha256(tokenPair.refreshToken()))
			.orElseThrow()
			.isRevoked()).isTrue();

		Member signedUpAgain = oAuthLoginService.loginOrSignUp(new OAuth2UserProfile(
			OAuthProvider.GOOGLE,
			"google-withdraw",
			"withdraw@example.com",
			"provider-name",
			null
		));

		assertThat(signedUpAgain.getId()).isNotEqualTo(member.getId());
		assertThat(signedUpAgain.isSignupRequired()).isTrue();
		assertThat(memberRepository.findByOauthProviderAndOauthSubject(
			OAuthProvider.GOOGLE,
			"google-withdraw"
		).orElseThrow().getId()).isEqualTo(signedUpAgain.getId());
	}

	@Test
	@DisplayName("탈퇴한 회원의 기존 access token은 사용할 수 없다")
	void rejectWithdrawnMemberAccessToken() throws Exception {
		Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
			"withdrawn-access@example.com",
			"withdrawn-access-user",
			OAuthProvider.GOOGLE,
			"google-withdrawn-access",
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(member).token();

		mockMvc.perform(delete("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}
}
