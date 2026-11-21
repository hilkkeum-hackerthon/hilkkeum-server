package com.hilkkeum.stamp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스탬프 적립 결과")
public record AwardStampResponse(
	@Schema(description = "게임 ID", example = "1") Long gameId,
	@Schema(description = "승자 멤버 ID", example = "2") Long memberId,
	@Schema(description = "이번 요청으로 적립됐는지. 이미 처리된 게임이거나 보유량이 3개면 false (재시도해도 안전)",
		example = "true") boolean awarded,
	@Schema(description = "승자의 현재 스탬프 수 (최대 3)", example = "2") int heartCount
) {
}
