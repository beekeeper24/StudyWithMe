package com.studywithme.mention.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MentionTargetResolver.class)
class MentionTargetResolverTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MentionTargetResolver mentionTargetResolver;

	@Test
	@DisplayName("추출된 닉네임 중 ACTIVE 회원만 멘션 대상으로 조회한다")
	void resolveActiveMentionTargets() {
		Member alice = saveMember("alice");
		saveMember("bob").withdraw();

		List<Member> targets = mentionTargetResolver.resolve(List.of("missing", "bob", "alice"));

		assertThat(targets).extracting(Member::getId)
			.containsExactly(alice.getId());
	}

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}
}
