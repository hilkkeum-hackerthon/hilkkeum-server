package com.hilkkeum.coupon.dto;

import java.time.OffsetDateTime;

import com.hilkkeum.common.time.KstTime;
import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;

import io.swagger.v3.oas.annotations.media.Schema;

/** 소원권 사용 결과 */
@Schema(description = "소원권")
public record CouponResponse(
	@Schema(description = "소원권 ID", example = "1") Long couponId,
	@Schema(description = "소유 멤버 ID", example = "1") Long memberId,
	@Schema(description = "상태 (AVAILABLE / USED / EXPIRED)", example = "USED") CouponStatus status,
	@Schema(description = "발급일시", example = "2026-11-21T20:00:00+09:00") OffsetDateTime issuedAt,
	@Schema(description = "만료일시", example = "2026-12-21T20:00:00+09:00") OffsetDateTime expiresAt,
	@Schema(description = "사용일시", example = "2026-11-22T13:00:00+09:00", nullable = true) OffsetDateTime usedAt
) {

	public static CouponResponse from(WishCoupon c) {
		return new CouponResponse(c.getId(), c.getOwnerMemberId(), c.getStatus(), KstTime.of(c.getIssuedAt()),
			KstTime.of(c.getExpiresAt()), KstTime.of(c.getUsedAt()));
	}
}
