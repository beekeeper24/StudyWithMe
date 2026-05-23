package com.studywithme.auth.presentation;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import java.util.List;

public record AuthMeResponse(
	Long id,
	String email,
	String nickname,
	String profileImageUrl,
	boolean nicknameRequired,
	String status,
	List<String> roles
) {

	public static AuthMeResponse from(Member member) {
		return new AuthMeResponse(
			member.getId(),
			member.getEmail(),
			member.getNickname(),
			member.getProfileImageUrl(),
			member.isNicknameRequired(),
			member.getStatus().name(),
			member.getRoles().stream()
				.map(MemberRole::name)
				.toList()
		);
	}
}
