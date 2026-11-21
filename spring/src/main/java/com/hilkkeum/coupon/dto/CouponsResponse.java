package com.hilkkeum.coupon.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import com.hilkkeum.common.time.KstTime;
import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * GET /api/v1/coupons 응답.
 * 사용 가능한 소원권은 최대 1장. 만료 · 사용 완료된 소원권은 포함하지 않는다.
 */
@Schema(description = "소원권 조회 결과")
public record CouponsResponse(
	@Schema(description = "사용 가능한 소원권 보유 여부", example = "true") boolean hasActiveCoupon,
	@Schema(description = "보유 소원권 (없으면 null)", nullable = true) ActiveCoupon coupon
) {

	@Schema(description = "보유 소원권")
	public record ActiveCoupon(
		@Schema(description = "소원권 ID", example = "1") Long couponId,
		@Schema(description = "상태", example = "AVAILABLE") CouponStatus status,
		@Schema(description = "발급일시", example = "2026-11-21T20:00:00+09:00") OffsetDateTime issuedAt,
		@Schema(description = "만료일시", example = "2026-12-21T20:00:00+09:00") OffsetDateTime expiresAt,
		@Schema(description = "남은 유효기간(일) — D-day 표시용", example = "30") long remainingDays
	) {

		public static ActiveCoupon of(WishCoupon c, LocalDateTime now) {
			long days = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), c.getExpiresAt().toLocalDate()));
			return new ActiveCoupon(c.getId(), c.getStatus(), KstTime.of(c.getIssuedAt()), KstTime.of(c.getExpiresAt()),
				days);
		}
	}
}
