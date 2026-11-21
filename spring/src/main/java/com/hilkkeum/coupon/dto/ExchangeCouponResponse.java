package com.hilkkeum.coupon.dto;

import java.time.OffsetDateTime;

import com.hilkkeum.common.time.KstTime;
import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;

import io.swagger.v3.oas.annotations.media.Schema;

/** POST /api/v1/coupons/exchange 응답 */
@Schema(description = "소원권 교환 결과")
public record ExchangeCouponResponse(
	@Schema(description = "발급된 소원권 ID", example = "1") Long couponId,
	@Schema(description = "멤버 ID", example = "1") Long memberId,
	@Schema(description = "교환 후 남은 스탬프 수", example = "0") int heartCount,
	@Schema(description = "발급된 소원권") Coupon coupon
) {

	@Schema(description = "소원권 정보")
	public record Coupon(
		@Schema(description = "상태", example = "AVAILABLE") CouponStatus status,
		@Schema(description = "발급일시", example = "2026-11-21T20:00:00+09:00") OffsetDateTime issuedAt,
		@Schema(description = "만료일시 (발급 + 1개월)", example = "2026-12-21T20:00:00+09:00") OffsetDateTime expiresAt
	) {
	}

	public static ExchangeCouponResponse of(WishCoupon c, int heartCount) {
		return new ExchangeCouponResponse(c.getId(), c.getOwnerMemberId(), heartCount,
			new Coupon(c.getStatus(), KstTime.of(c.getIssuedAt()), KstTime.of(c.getExpiresAt())));
	}
}
