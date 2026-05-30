package com.studywithme.member.repository;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.domain.OAuthProvider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByOauthProviderAndOauthSubject(
		OAuthProvider oauthProvider,
		String oauthSubject
	);

	boolean existsByNickname(String nickname);

	boolean existsByNicknameAndIdNot(String nickname, Long id);

	List<Member> findAllByNicknameInAndStatus(Collection<String> nicknames, MemberStatus status);

	@Query("""
		select m
		from Member m
		where lower(m.email) in :emails
			and m.status = :status
		""")
	List<Member> findAllByNormalizedEmailInAndStatus(
		@Param("emails") Collection<String> emails,
		@Param("status") MemberStatus status
	);
}
