package com.studywithme.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MemberRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("OAuth 제공자와 subject로 회원을 조회한다")
	void findByOauthProviderAndOauthSubject() {
		Member member = Member.createOAuthMember(
			"bee@example.com",
			"beekeeper",
			OAuthProvider.GOOGLE,
			"google-123",
			"https://example.com/profile.png"
		);

		memberRepository.saveAndFlush(member);

		Member found = memberRepository.findByOauthProviderAndOauthSubject(
			OAuthProvider.GOOGLE,
			"google-123"
		).orElseThrow();

		assertThat(found.getEmail()).isEqualTo("bee@example.com");
		assertThat(found.getNickname()).isEqualTo("beekeeper");
		assertThat(found.getRoles()).containsExactly(MemberRole.USER);
	}

	@Test
	@DisplayName("닉네임 중복 여부를 확인한다")
	void existsByNickname() {
		Member member = Member.createOAuthMember(
			"bee@example.com",
			"beekeeper",
			OAuthProvider.KAKAO,
			"kakao-123",
			null
		);
		memberRepository.saveAndFlush(member);

		assertThat(memberRepository.existsByNickname("beekeeper")).isTrue();
		assertThat(memberRepository.existsByNickname("another")).isFalse();
	}
}
