package com.studywithme.chat.repository;

import com.studywithme.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByRoomIdOrderByCreatedAtAscIdAsc(Long roomId);

	Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);

	Optional<ChatMessage> findTopByRoomIdOrderByCreatedAtDescIdDesc(Long roomId);

	long countByRoomIdAndSenderMemberIdNot(Long roomId, Long senderMemberId);

	long countByRoomIdAndSenderMemberIdNotAndIdGreaterThan(Long roomId, Long senderMemberId, Long messageId);
}
