package com.hilkkeum.coupon.dto;

import jakarta.validation.constraints.NotNull;

/** approved=true → 사용 승인(USED), false → 거절(AVAILABLE로 복귀) */
public record RespondCouponRequest(
	@NotNull(message = "approved 값이 필요합니다.") Boolean approved
) {
}
