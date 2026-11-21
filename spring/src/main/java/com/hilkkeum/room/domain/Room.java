package com.hilkkeum.room.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "invite_code", length = 6)
	private String inviteCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RoomStatus status;

	@Column(name = "invite_expires_at")
	private LocalDateTime inviteExpiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 방 생성: 상대 입장 대기(PENDING). 초대 코드는 방이 성립되기 전까지 유효 */
	public static Room create(String inviteCode) {
		Room room = new Room();
		room.inviteCode = inviteCode;
		room.status = RoomStatus.PENDING;
		return room;
	}

	public boolean isPending() {
		return status == RoomStatus.PENDING;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
