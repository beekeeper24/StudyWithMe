package com.studywithme.auth.application;

import com.studywithme.auth.oauth.OAuth2UserProfile;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginService {

	private final MemberRepository memberRepository;
	private final AdminRoleService adminRoleService;

	@Autowired
	public OAuthLoginService(MemberRepository memberRepository, AdminRoleService adminRoleService) {
		this.memberRepository = memberRepository;
		this.adminRoleService = adminRoleService;
	}

	public OAuthLoginService(MemberRepository memberRepository) {
		this(memberRepository, new AdminRoleService(new AdminRoleProperties()));
	}

	@Transactional
	public Member loginOrSignUp(OAuth2UserProfile profile) {
		return memberRepository.findByOauthProviderAndOauthSubject(
			profile.provider(),
			profile.subject()
		).map(member -> {
			member.updateOAuthProfile(profile.email(), profile.profileImageUrl());
			adminRoleService.grantIfConfiguredAdmin(member);
			return member;
		}).orElseGet(() -> signUp(profile));
	}

	private Member signUp(OAuth2UserProfile profile) {
		Member member = Member.createOAuthMember(
			profile.email(),
			null,
			profile.provider(),
			profile.subject(),
			profile.profileImageUrl()
		);
		adminRoleService.grantIfConfiguredAdmin(member);
		return memberRepository.save(member);
	}
}
