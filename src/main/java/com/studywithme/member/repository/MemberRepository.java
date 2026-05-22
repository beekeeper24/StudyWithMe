package com.studywithme.member.repository;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.domain.OAuthProvider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByOauthProviderAndOauthSubject(
		OAuthProvider oauthProvider,
		String oauthSubject
	);

	boolean existsByNickname(String nickname);

	List<Member> findAllByNicknameInAndStatus(Collection<String> nicknames, MemberStatus status);
}
