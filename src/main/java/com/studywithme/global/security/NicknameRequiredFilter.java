package com.studywithme.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.global.exception.ErrorResponse;
import com.studywithme.member.domain.Member;
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

public class NicknameRequiredFilter extends OncePerRequestFilter {

	private static final Set<String> ALLOWED_ONBOARDING_PATHS = Set.of(
		"/api/v1/auth/me",
		"/api/v1/auth/me/nickname",
		"/api/v1/auth/refresh",
		"/api/v1/auth/logout"
	);

	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	public NicknameRequiredFilter(MemberRepository memberRepository, ObjectMapper objectMapper) {
		this.memberRepository = memberRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (shouldSkip(request)) {
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
		if (member.isNicknameRequired()) {
			writeErrorResponse(request, response, MemberErrorCode.NICKNAME_REQUIRED);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean shouldSkip(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return !path.startsWith("/api/v1/")
			|| ALLOWED_ONBOARDING_PATHS.contains(path);
	}

	private void writeErrorResponse(
		HttpServletRequest request,
		HttpServletResponse response,
		MemberErrorCode errorCode
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
