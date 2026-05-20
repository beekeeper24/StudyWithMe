package com.studywithme.member.repository;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByOauthProviderAndOauthSubject(
		OAuthProvider oauthProvider,
		String oauthSubject
	);

	boolean existsByNickname(String nickname);
}
