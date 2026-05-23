package com.studywithme.chat.repository;

import com.studywithme.chat.domain.ChatRoomMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

	Optional<ChatRoomMember> findByRoomIdAndMemberId(Long roomId, Long memberId);

	List<ChatRoomMember> findAllByRoomId(Long roomId);
}
