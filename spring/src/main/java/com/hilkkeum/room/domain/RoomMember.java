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

	@PrePersist
	void onCreate() {
		if (joinedAt == null) {
			joinedAt = LocalDateTime.now();
		}
	}
}
