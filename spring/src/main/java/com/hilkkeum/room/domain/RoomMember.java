package com.hilkkeum.room.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MemberRole role;

	@Column(name = "member_token", nullable = false, unique = true, length = 64)
	private String memberToken;

	@Column(name = "stamp_count", nullable = false)
	private int stampCount;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	public static RoomMember create(Room room, MemberRole role, String memberToken) {
		RoomMember member = new RoomMember();
		member.room = room;
		member.role = role;
		member.memberToken = memberToken;
		member.stampCount = 0;
		return member;
	}

	/** 스탬프 +1 — 최대치(max)에 도달했으면 적립하지 않고 false */
	public boolean addStamp(int max) {
		if (stampCount >= max) {
			return false;
		}
		stampCount++;
		return true;
	}

	public boolean hasFullStamps(int max) {
		return stampCount >= max;
	}

	/** 소원권 사용 · 만료 시 스탬프 초기화 */
	public void resetStamps() {
		stampCount = 0;
	}

	@PrePersist
	void onCreate() {
		if (joinedAt == null) {
			joinedAt = LocalDateTime.now();
		}
	}
}
