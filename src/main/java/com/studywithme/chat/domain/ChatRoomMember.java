package com.studywithme.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "chat_room_members",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_chat_room_members_room_member",
		columnNames = {"room_id", "member_id"}
	)
)
public class ChatRoomMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	@Column(name = "hidden_at")
	private LocalDateTime hiddenAt;

	protected ChatRoomMember() {
	}

	private ChatRoomMember(Long roomId, Long memberId) {
		this.roomId = roomId;
		this.memberId = memberId;
	}

	public static ChatRoomMember join(Long roomId, Long memberId) {
		return new ChatRoomMember(roomId, memberId);
	}

	public void hide() {
		this.hiddenAt = LocalDateTime.now();
	}

	public void restore() {
		this.hiddenAt = null;
	}

	@PrePersist
	void prePersist() {
		this.joinedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getRoomId() {
		return roomId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public LocalDateTime getHiddenAt() {
		return hiddenAt;
	}
}
