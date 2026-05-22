package com.studywithme.mention.application;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MentionTargetResolver {

	private final MemberRepository memberRepository;

	public MentionTargetResolver(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public List<Member> resolve(List<String> nicknames) {
		if (nicknames == null || nicknames.isEmpty()) {
			return List.of();
		}
		Map<String, Member> membersByNickname = memberRepository.findAllByNicknameInAndStatus(nicknames, MemberStatus.ACTIVE)
			.stream()
			.collect(LinkedHashMap::new, (map, member) -> map.put(member.getNickname(), member), Map::putAll);
		return nicknames.stream()
			.map(membersByNickname::get)
			.filter(member -> member != null)
			.toList();
	}
}
