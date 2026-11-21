package com.hilkkeum.coupon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.auth.CurrentMember;
import com.hilkkeum.coupon.dto.CouponResponse;
import com.hilkkeum.coupon.dto.CouponsResponse;
import com.hilkkeum.coupon.dto.RespondCouponRequest;
import com.hilkkeum.coupon.service.CouponService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 모든 API: Header Authorization: Bearer {memberToken} */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

	private final CouponService couponService;

	/** 소원권 교환 (스탬프 3개 차감) */
	@PostMapping("/exchange")
	@ResponseStatus(HttpStatus.CREATED)
	public CouponResponse exchange(@CurrentMember AuthMember me) {
		return couponService.exchange(me);
	}

	/** 소원권 조회 (상태 · 만료일 + 내가 승인해야 할 요청) */
	@GetMapping
	public CouponsResponse getCoupons(@CurrentMember AuthMember me) {
		return couponService.getCoupons(me);
	}

	/** 소원권 사용 요청 → 상대에게 사용 확인 요청 */
	@PostMapping("/{couponId}/use-request")
	public CouponResponse requestUse(@PathVariable Long couponId, @CurrentMember AuthMember me) {
		return couponService.requestUse(couponId, me);
	}

	/** 소원권 사용 응답 (상대가 승인/거절) */
	@PostMapping("/{couponId}/respond")
	public CouponResponse respond(@PathVariable Long couponId, @Valid @RequestBody RespondCouponRequest request,
		@CurrentMember AuthMember me) {
		return couponService.respond(couponId, request.approved(), me);
	}
}
