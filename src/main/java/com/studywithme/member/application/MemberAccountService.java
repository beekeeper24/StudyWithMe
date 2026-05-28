package com.studywithme.member.application;

import com.studywithme.auth.token.TokenService;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.exception.MemberErrorCode;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.application.StudyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAccountService {

	private final MemberRepository memberRepository;
	private final TokenService tokenService;
	private final StudyService studyService;

	public MemberAccountService(
		MemberRepository memberRepository,
		TokenService tokenService,
		StudyService studyService
	) {
		this.memberRepository = memberRepository;
		this.tokenService = tokenService;
		this.studyService = studyService;
	}

	@Transactional
	public void withdraw(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		studyService.deleteOwnedActiveStudies(memberId);
		member.withdraw(
			"withdrawn-" + member.getId() + "@studywithme.local",
			"withdrawn:" + member.getId()
		);
		tokenService.revokeAllByMemberId(memberId);
	}
}
