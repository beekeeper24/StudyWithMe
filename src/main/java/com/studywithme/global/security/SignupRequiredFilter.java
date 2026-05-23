package com.studywithme.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.ErrorCode;
import com.studywithme.global.exception.ErrorResponse;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.exception.MemberErrorCode;
import com.studywithme.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SignupRequiredFilter extends OncePerRequestFilter {

	private static final Set<String> ALLOWED_ONBOARDING_PATHS = Set.of(
		"/api/v1/auth/me",
		"/api/v1/auth/me/signup",
		"/api/v1/auth/me/nickname",
		"/api/v1/auth/me/withdraw",
		"/api/v1/auth/refresh",
		"/api/v1/auth/logout"
	);

	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	public SignupRequiredFilter(MemberRepository memberRepository, ObjectMapper objectMapper) {
		this.memberRepository = memberRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (shouldSkipPath(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
			|| !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof AuthenticatedMemberPrincipal principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		Member member = memberRepository.findById(principal.memberId()).orElse(null);
		if (member == null) {
			writeErrorResponse(request, response, MemberErrorCode.MEMBER_NOT_FOUND);
			return;
		}
		if (member.getStatus() != MemberStatus.ACTIVE) {
			writeErrorResponse(request, response, AuthErrorCode.INVALID_ACCESS_TOKEN);
			return;
		}
		if (isOnboardingPath(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		if (member.isSignupRequired()) {
			writeErrorResponse(request, response, MemberErrorCode.NICKNAME_REQUIRED);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean shouldSkipPath(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return !path.startsWith("/api/v1/");
	}

	private boolean isOnboardingPath(HttpServletRequest request) {
		return ALLOWED_ONBOARDING_PATHS.contains(request.getRequestURI());
	}

	private void writeErrorResponse(
		HttpServletRequest request,
		HttpServletResponse response,
		ErrorCode errorCode
	) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(
			response.getWriter(),
			ErrorResponse.of(errorCode, request.getRequestURI())
		);
	}
}
