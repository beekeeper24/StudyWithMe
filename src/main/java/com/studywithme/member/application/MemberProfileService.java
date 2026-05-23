package com.studywithme.member.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.exception.MemberErrorCode;
import com.studywithme.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberProfileService {

	private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9_]{2,20}$");
	public static final String CURRENT_TERMS_VERSION = "2026-05-24";
	public static final String CURRENT_PRIVACY_POLICY_VERSION = "2026-05-24";

	private final MemberRepository memberRepository;

	public MemberProfileService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Transactional
	public Member updateNickname(Long memberId, String nickname) {
		String normalizedNickname = normalize(nickname);
		validateNickname(normalizedNickname);
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		if (memberRepository.existsByNicknameAndIdNot(normalizedNickname, member.getId())) {
			throw new BusinessException(MemberErrorCode.DUPLICATED_NICKNAME);
		}

		member.updateNickname(normalizedNickname);
		return member;
	}

	@Transactional
	public Member completeSignup(
		Long memberId,
		String nickname,
		boolean termsAgreed,
		boolean privacyPolicyAgreed
	) {
		if (!termsAgreed || !privacyPolicyAgreed) {
			throw new BusinessException(MemberErrorCode.TERMS_AGREEMENT_REQUIRED);
		}
		String normalizedNickname = normalize(nickname);
		validateNickname(normalizedNickname);
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		if (memberRepository.existsByNicknameAndIdNot(normalizedNickname, member.getId())) {
			throw new BusinessException(MemberErrorCode.DUPLICATED_NICKNAME);
		}

		member.completeSignup(
			normalizedNickname,
			CURRENT_TERMS_VERSION,
			CURRENT_PRIVACY_POLICY_VERSION,
			LocalDateTime.now()
		);
		return member;
	}

	private String normalize(String nickname) {
		return nickname == null ? "" : nickname.trim();
	}

	private void validateNickname(String nickname) {
		if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
			throw new BusinessException(MemberErrorCode.INVALID_NICKNAME);
		}
	}
}
