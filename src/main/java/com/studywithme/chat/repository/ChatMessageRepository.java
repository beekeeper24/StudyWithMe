package com.studywithme.chat.repository;

import com.studywithme.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByRoomIdOrderByCreatedAtAscIdAsc(Long roomId);
}
