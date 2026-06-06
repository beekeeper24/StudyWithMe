package com.studywithme.auth.token;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final MemberRepository memberRepository;
	private final TokenProperties tokenProperties;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final Clock clock;

	@Autowired
	public TokenService(
		JwtTokenProvider jwtTokenProvider,
		RefreshTokenRepository refreshTokenRepository,
		MemberRepository memberRepository,
		TokenProperties tokenProperties,
		RefreshTokenGenerator refreshTokenGenerator
	) {
		this(
			jwtTokenProvider,
			refreshTokenRepository,
			memberRepository,
			tokenProperties,
			refreshTokenGenerator,
			Clock.systemUTC()
		);
	}

	TokenService(
		JwtTokenProvider jwtTokenProvider,
		RefreshTokenRepository refreshTokenRepository,
		MemberRepository memberRepository,
		TokenProperties tokenProperties,
		Clock clock
	) {
		this(
			jwtTokenProvider,
			refreshTokenRepository,
			memberRepository,
			tokenProperties,
			new RefreshTokenGenerator(),
			clock
		);
	}

	TokenService(
		JwtTokenProvider jwtTokenProvider,
		RefreshTokenRepository refreshTokenRepository,
		MemberRepository memberRepository,
		TokenProperties tokenProperties,
		RefreshTokenGenerator refreshTokenGenerator,
		Clock clock
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenRepository = refreshTokenRepository;
		this.memberRepository = memberRepository;
		this.tokenProperties = tokenProperties;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.clock = clock;
	}

	@Transactional
	public TokenPair issue(Member member) {
		ensureActiveMember(member);
		AccessToken accessToken = jwtTokenProvider.createAccessToken(member);
		String refreshToken = refreshTokenGenerator.generate();
		Instant issuedAt = clock.instant();
		Instant refreshTokenExpiresAt = issuedAt.plus(tokenProperties.refreshTokenTtl());
		refreshTokenRepository.save(RefreshToken.issue(member, refreshToken, issuedAt, refreshTokenExpiresAt));

		return new TokenPair(
			accessToken.token(),
			accessToken.expiresAt(),
			refreshToken,
			refreshTokenExpiresAt
		);
	}

	@Transactional
	public TokenPair refresh(String rawRefreshToken) {
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(
			RefreshTokenHash.sha256(rawRefreshToken)
		).orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		Instant now = clock.instant();

		if (refreshToken.isExpired(now)) {
			throw new BusinessException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
		}
		if (!refreshToken.isUsable(now)) {
			throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		refreshToken.rotate(now);
		Member member = memberRepository.findById(refreshToken.getMember().getId())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw new BusinessException(refreshErrorCodeFor(member.getStatus()));
		}
		return issue(member);
	}

	@Transactional
	public void revoke(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(RefreshTokenHash.sha256(rawRefreshToken))
			.ifPresent(refreshToken -> refreshToken.revoke(clock.instant()));
	}

	@Transactional
	public void revokeAllByMemberId(Long memberId) {
		Instant now = clock.instant();
		refreshTokenRepository.findAllByMemberId(memberId)
			.forEach(refreshToken -> refreshToken.revoke(now));
	}

	private void ensureActiveMember(Member member) {
		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_RESTRICTED);
		}
	}

	private AuthErrorCode refreshErrorCodeFor(MemberStatus status) {
		if (status == MemberStatus.SUSPENDED || status == MemberStatus.BANNED) {
			return AuthErrorCode.ACCOUNT_RESTRICTED;
		}
		return AuthErrorCode.INVALID_REFRESH_TOKEN;
	}
}
