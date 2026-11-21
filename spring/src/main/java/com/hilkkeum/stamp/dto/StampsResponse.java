package com.hilkkeum.stamp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스탬프 현황")
public record StampsResponse(
	@Schema(description = "보유 스탬프 수 (최대 3, 3개가 되면 소원권 자동 발급)", example = "3") int stampCount,
	@Schema(description = "최대 보유 스탬프 수", example = "3") int maxStamps,
	@Schema(description = "사용 가능한 소원권 보유 여부 (보유 중 승리하면 스탬프 소멸)", example = "true") boolean hasActiveCoupon
) {
}
