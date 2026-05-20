package com.studywithme.auth.oauth;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record StudyWithMeOAuth2User(
	Long memberId,
	String name,
	Map<String, Object> attributes,
	Set<GrantedAuthority> authorities
) implements OAuth2User {

	public static StudyWithMeOAuth2User from(Member member, OAuth2UserProfile profile, Map<String, Object> attributes) {
		Set<GrantedAuthority> authorities = member.getRoles().stream()
			.map(MemberRole::name)
			.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
			.collect(Collectors.toUnmodifiableSet());

		return new StudyWithMeOAuth2User(
			member.getId(),
			profile.subject(),
			Map.copyOf(attributes),
			authorities
		);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
}
