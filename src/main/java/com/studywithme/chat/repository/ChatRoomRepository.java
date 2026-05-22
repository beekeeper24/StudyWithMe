package com.studywithme.chat.repository;

import com.studywithme.chat.domain.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByRoomKey(String roomKey);

	@Query("""
		select room
		from ChatRoom room
		join ChatRoomMember roomMember on roomMember.roomId = room.id
		where roomMember.memberId = :memberId
		order by room.createdAt desc, room.id desc
		""")
	List<ChatRoom> findAllByMemberId(@Param("memberId") Long memberId);
}
