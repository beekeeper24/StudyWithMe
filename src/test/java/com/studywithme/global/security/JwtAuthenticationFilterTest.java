package com.studywithme.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.token.AccessTokenClaims;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.global.exception.BusinessException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

	private final JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Bearer access token이 유효하면 SecurityContext에 회원 인증 정보를 저장한다")
	void authenticateBearerToken() throws Exception {
		when(jwtTokenProvider.parse("valid-token")).thenReturn(new AccessTokenClaims(
			1L,
			Set.of("USER"),
			Instant.parse("2026-05-21T00:30:00Z")
		));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
		request.addHeader("Authorization", "Bearer valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		assertThat(principal).isInstanceOf(AuthenticatedMemberPrincipal.class);
		AuthenticatedMemberPrincipal memberPrincipal = (AuthenticatedMemberPrincipal) principal;
		assertThat(memberPrincipal.memberId()).isEqualTo(1L);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_USER");
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	@DisplayName("Bearer access token이 없으면 인증을 시도하지 않는다")
	void skipWhenAuthorizationHeaderMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(jwtTokenProvider, never()).parse(Mockito.anyString());
	}

	@Test
	@DisplayName("Bearer access token이 유효하지 않으면 AUTH-003 응답을 반환한다")
	void rejectInvalidBearerToken() throws Exception {
		when(jwtTokenProvider.parse("invalid-token"))
			.thenThrow(new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
		request.addHeader("Authorization", "Bearer invalid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("\"code\":\"AUTH-003\"");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
