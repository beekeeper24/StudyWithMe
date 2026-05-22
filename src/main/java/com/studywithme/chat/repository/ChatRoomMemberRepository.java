package com.studywithme.chat.repository;

import com.studywithme.chat.domain.ChatRoomMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

	List<ChatRoomMember> findAllByRoomId(Long roomId);
}
