package com.studywithme.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.token.AccessTokenClaims;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.exception.ErrorCode;
import com.studywithme.global.exception.ErrorResponse;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, MemberRepository memberRepository) {
		this(jwtTokenProvider, memberRepository, new ObjectMapper().findAndRegisterModules());
	}

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		MemberRepository memberRepository,
		ObjectMapper objectMapper
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.memberRepository = memberRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader(AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			AccessTokenClaims claims = jwtTokenProvider.parse(authorization.substring(BEARER_PREFIX.length()));
			ensureActiveMember(claims.memberId());
			AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(
				claims.memberId(),
				claims.roles()
			);
			Set<SimpleGrantedAuthority> authorities = claims.roles().stream()
				.map(this::toAuthority)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toUnmodifiableSet());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				authorities
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
			writeAuthErrorResponse(request, response, exception.getErrorCode());
		}
	}

	private void ensureActiveMember(Long memberId) {
		MemberStatus status = memberRepository.findById(memberId)
			.map(member -> member.getStatus())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN));
		if (status != MemberStatus.ACTIVE) {
			throw new BusinessException(authErrorCodeFor(status));
		}
	}

	private AuthErrorCode authErrorCodeFor(MemberStatus status) {
		if (status == MemberStatus.SUSPENDED || status == MemberStatus.BANNED) {
			return AuthErrorCode.ACCOUNT_RESTRICTED;
		}
		return AuthErrorCode.INVALID_ACCESS_TOKEN;
	}

	private String toAuthority(String role) {
		if (role.startsWith("ROLE_")) {
			return role;
		}
		return "ROLE_" + role;
	}

	private void writeAuthErrorResponse(
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
