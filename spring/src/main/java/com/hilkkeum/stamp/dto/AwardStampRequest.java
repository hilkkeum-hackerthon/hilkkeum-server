package com.hilkkeum.stamp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Node(게임 결산) → Spring 스탬프 적립 요청.
 * - 승리: winnerMemberId 를 보내면 승자 1명에게 1개
 * - 무승부(맞힌 개수 동일): winnerMemberId 를 생략(null)하면 두 사람 모두 1개 — DB에 무승부로 기록돼 있어야 함
 */
@Schema(description = "스탬프 적립 요청")
public record AwardStampRequest(
	@Schema(description = "게임(라운드) ID", example = "1")
	@NotNull(message = "gameId가 필요합니다.") Long gameId,
	@Schema(description = "확정된 승자의 멤버 ID. 무승부면 생략(null)", example = "2", nullable = true)
	Long winnerMemberId
) {

	public boolean isDraw() {
		return winnerMemberId == null;
	}
}
