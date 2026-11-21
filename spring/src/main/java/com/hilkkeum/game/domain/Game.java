package com.hilkkeum.game.domain;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * games 테이블은 Node(게임 로직)가 관리한다. Spring은 스탬프 적립 검증에 필요한 컬럼만 읽고,
 * stamp_awarded_at만 GameRepository의 조건부 UPDATE로 기록한다.
 */
@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

	/** 결산(승자 확정) 이후 상태 */
	private static final Set<String> SETTLED_STATUSES = Set.of("SETTLEMENT", "FINISHED");

	@Id
	private Long id;

	@Column(name = "room_id", nullable = false, insertable = false, updatable = false)
	private Long roomId;

	@Column(name = "status", insertable = false, updatable = false)
	private String status;

	@Column(name = "winner_player_id", insertable = false, updatable = false)
	private Long winnerPlayerId;

	@Column(name = "decided_by", insertable = false, updatable = false)
	private String decidedBy;

	@Column(name = "stamp_awarded_at", insertable = false, updatable = false)
	private LocalDateTime stampAwardedAt;

	/** 무승부 기록값 (맞힌 개수 동일 → 벌칙 없이 두 사람 모두 스탬프) */
	public static final String DRAW = "DRAW";

	public boolean isSettled() {
		return SETTLED_STATUSES.contains(status);
	}

	/** 결산 단계 이후이고 승자가 확정됐는지 */
	public boolean isWinnerConfirmed() {
		return isSettled() && winnerPlayerId != null;
	}

	/** 결산 단계 이후이고 무승부로 확정됐는지 (승자 없음 + decided_by = DRAW) */
	public boolean isDrawConfirmed() {
		return isSettled() && winnerPlayerId == null && DRAW.equals(decidedBy);
	}
}
