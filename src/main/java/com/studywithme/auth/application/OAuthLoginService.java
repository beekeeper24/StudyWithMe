package com.studywithme.auth.application;

import com.studywithme.auth.oauth.OAuth2UserProfile;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginService {

	private static final int MAX_NICKNAME_LENGTH = 50;
	private static final int SUFFIX_LENGTH = 8;

	private final MemberRepository memberRepository;

	public OAuthLoginService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Transactional
	public Member loginOrSignUp(OAuth2UserProfile profile) {
		return memberRepository.findByOauthProviderAndOauthSubject(
			profile.provider(),
			profile.subject()
		).map(member -> {
			member.updateOAuthProfile(profile.email(), profile.profileImageUrl());
			return member;
		}).orElseGet(() -> signUp(profile));
	}

	private Member signUp(OAuth2UserProfile profile) {
		String nickname = resolveNickname(profile);
		Member member = Member.createOAuthMember(
			profile.email(),
			nickname,
			profile.provider(),
			profile.subject(),
			profile.profileImageUrl()
		);
		return memberRepository.save(member);
	}

	private String resolveNickname(OAuth2UserProfile profile) {
		String baseNickname = normalizeNickname(profile.nickname());
		String candidate = baseNickname;
		int counter = 0;

		while (memberRepository.existsByNickname(candidate)) {
			String suffix = suffix(profile, counter);
			candidate = withSuffix(baseNickname, suffix);
			counter++;
		}

		return candidate;
	}

	private String normalizeNickname(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			return "user";
		}
		return nickname.trim();
	}

	private String suffix(OAuth2UserProfile profile, int counter) {
		int hash = Math.abs(Objects.hash(profile.provider(), profile.subject(), counter));
		return Integer.toHexString(hash).toLowerCase(Locale.ROOT);
	}

	private String withSuffix(String baseNickname, String suffix) {
		String shortSuffix = suffix.length() <= SUFFIX_LENGTH ? suffix : suffix.substring(0, SUFFIX_LENGTH);
		int maxBaseLength = MAX_NICKNAME_LENGTH - shortSuffix.length() - 1;
		String shortenedBase = baseNickname.length() <= maxBaseLength
			? baseNickname
			: baseNickname.substring(0, maxBaseLength);
		return shortenedBase + "-" + shortSuffix;
	}
}
