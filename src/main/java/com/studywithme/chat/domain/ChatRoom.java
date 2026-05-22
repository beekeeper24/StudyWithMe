package com.studywithme.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "chat_rooms",
	uniqueConstraints = @UniqueConstraint(name = "uk_chat_rooms_room_key", columnNames = "room_key")
)
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChatRoomType type;

	@Column(name = "room_key", nullable = false, length = 100)
	private String roomKey;

	@Column(name = "study_id")
	private Long studyId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected ChatRoom() {
	}

	private ChatRoom(ChatRoomType type, String roomKey, Long studyId) {
		this.type = type;
		this.roomKey = roomKey;
		this.studyId = studyId;
	}

	public static ChatRoom privateRoom(String roomKey) {
		return new ChatRoom(ChatRoomType.PRIVATE, roomKey, null);
	}

	public static ChatRoom studyRoom(String roomKey, Long studyId) {
		return new ChatRoom(ChatRoomType.STUDY, roomKey, studyId);
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public ChatRoomType getType() {
		return type;
	}

	public String getRoomKey() {
		return roomKey;
	}

	public Long getStudyId() {
		return studyId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
