package com.hilkkeum.game.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * games 테이블은 Node(게임 로직)가 관리한다. Spring은 스탬프 적립에 필요한 컬럼만 읽고,
 * stamp_awarded_at만 GameRepository의 조건부 UPDATE로 기록한다.
 */
@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

	@Id
	private Long id;

	@Column(name = "room_id", nullable = false, insertable = false, updatable = false)
	private Long roomId;

	@Column(name = "status", insertable = false, updatable = false)
	private String status;

	@Column(name = "stamp_awarded_at", insertable = false, updatable = false)
	private LocalDateTime stampAwardedAt;
}
