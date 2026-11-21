package com.hilkkeum.stamp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Node(게임 결산) → Spring: 확정된 승자에게 스탬프 1개 */
@Schema(description = "스탬프 적립 요청")
public record AwardStampRequest(
	@Schema(description = "게임(라운드) ID", example = "1")
	@NotNull(message = "gameId가 필요합니다.") Long gameId,
	@Schema(description = "확정된 승자의 멤버 ID", example = "2")
	@NotNull(message = "winnerMemberId가 필요합니다.") Long winnerMemberId
) {
}
