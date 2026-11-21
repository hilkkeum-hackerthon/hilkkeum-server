package com.hilkkeum.stamp.dto;

import jakarta.validation.constraints.NotNull;

/** Node(게임 결산) → Spring: 이 게임의 승자에게 스탬프 1개 */
public record AwardStampRequest(
	@NotNull(message = "gameId가 필요합니다.") Long gameId,
	@NotNull(message = "winnerMemberId가 필요합니다.") Long winnerMemberId
) {
}
