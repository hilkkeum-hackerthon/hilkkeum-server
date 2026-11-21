package com.hilkkeum.stamp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스탬프 현황")
public record StampsResponse(
	@Schema(description = "보유 스탬프 수 (최대 3)", example = "3") int stampCount,
	@Schema(description = "소원권 1장에 필요한 스탬프 수", example = "3") int stampsPerCoupon,
	@Schema(description = "지금 교환 가능한지 (스탬프 3개 + 사용 가능한 소원권 미보유)", example = "true") boolean exchangeable
) {
}
