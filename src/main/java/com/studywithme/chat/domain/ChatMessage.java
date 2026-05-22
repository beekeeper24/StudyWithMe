package com.studywithme.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "sender_member_id", nullable = false)
	private Long senderMemberId;

	@Column(nullable = false, length = 1000)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected ChatMessage() {
	}

	private ChatMessage(Long roomId, Long senderMemberId, String content) {
		this.roomId = roomId;
		this.senderMemberId = senderMemberId;
		this.content = content;
	}

	public static ChatMessage create(Long roomId, Long senderMemberId, String content) {
		return new ChatMessage(roomId, senderMemberId, content);
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getRoomId() {
		return roomId;
	}

	public Long getSenderMemberId() {
		return senderMemberId;
	}

	public String getContent() {
		return content;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
