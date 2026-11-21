package com.hilkkeum.coupon.dto;

import java.time.LocalDateTime;

import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;

public record CouponResponse(
	Long couponId,
	Long ownerMemberId,
	CouponStatus status,
	LocalDateTime issuedAt,
	LocalDateTime expiresAt,
	LocalDateTime useRequestedAt,
	LocalDateTime respondedAt,
	LocalDateTime usedAt
) {

	public static CouponResponse from(WishCoupon c) {
		return new CouponResponse(c.getId(), c.getOwnerMemberId(), c.getStatus(), c.getIssuedAt(),
			c.getExpiresAt(), c.getUseRequestedAt(), c.getRespondedAt(), c.getUsedAt());
	}
}
