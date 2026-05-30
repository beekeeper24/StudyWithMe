package com.studywithme.auth.application;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminRoleBootstrapper implements ApplicationRunner {

	private final MemberRepository memberRepository;
	private final AdminRoleService adminRoleService;

	public AdminRoleBootstrapper(MemberRepository memberRepository, AdminRoleService adminRoleService) {
		this.memberRepository = memberRepository;
		this.adminRoleService = adminRoleService;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Set<String> adminEmails = adminRoleService.getAdminEmails();
		if (adminEmails.isEmpty()) {
			return;
		}

		for (Member member : memberRepository.findAllByNormalizedEmailInAndStatus(adminEmails, MemberStatus.ACTIVE)) {
			adminRoleService.grantIfConfiguredAdmin(member);
		}
	}
}
