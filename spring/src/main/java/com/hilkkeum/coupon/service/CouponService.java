package com.hilkkeum.coupon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;
import com.hilkkeum.coupon.dto.CouponResponse;
import com.hilkkeum.coupon.dto.CouponsResponse;
import com.hilkkeum.coupon.repository.WishCouponRepository;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

	/** 소원권 1장 = 스탬프 3개 */
	public static final int STAMPS_PER_COUPON = 3;

	private final WishCouponRepository couponRepository;
	private final RoomMemberRepository roomMemberRepository;

	/** 스탬프 3개 차감 후 소원권 발급 (발급일로부터 1개월 유효) */
	@Transactional
	public CouponResponse exchange(AuthMember me) {
		if (roomMemberRepository.useStamps(me.memberId(), STAMPS_PER_COUPON) != 1) {
			throw new ApiException(ErrorCode.NOT_ENOUGH_STAMPS);
		}
		WishCoupon coupon = couponRepository.save(WishCoupon.issue(me.roomId(), me.memberId(), LocalDateTime.now()));
		return CouponResponse.from(coupon);
	}

	/** 내 스탬프 · 소원권 + 내가 승인해야 할 상대의 사용 요청 (조회 시점에 만료 처리) */
	@Transactional
	public CouponsResponse getCoupons(AuthMember me) {
		couponRepository.expireOverdue(me.roomId(), LocalDateTime.now());

		int stampCount = roomMemberRepository.findById(me.memberId())
			.map(RoomMember::getStampCount)
			.orElse(0);
		List<CouponResponse> mine = couponRepository.findByOwnerMemberIdOrderByIssuedAtDesc(me.memberId())
			.stream().map(CouponResponse::from).toList();
		List<CouponResponse> pending = couponRepository.findPendingApprovals(me.roomId(), me.memberId())
			.stream().map(CouponResponse::from).toList();
		return new CouponsResponse(stampCount, mine, pending);
	}

	/** 소원권 사용 버튼 → 상대에게 사용 확인 요청 (본인 소원권 · AVAILABLE · 만료 전) */
	@Transactional
	public CouponResponse requestUse(Long couponId, AuthMember me) {
		LocalDateTime now = LocalDateTime.now();
		WishCoupon coupon = findInMyRoom(couponId, me);
		if (!coupon.isOwnedBy(me.memberId())) {
			throw new ApiException(ErrorCode.NOT_COUPON_OWNER);
		}
		if (coupon.getStatus() == CouponStatus.EXPIRED) {
			throw new ApiException(ErrorCode.COUPON_EXPIRED);
		}
		if (coupon.getStatus() == CouponStatus.AVAILABLE && coupon.isExpiredAt(now)) {
			couponRepository.updateStatus(couponId, CouponStatus.EXPIRED);
			throw new ApiException(ErrorCode.COUPON_EXPIRED);
		}
		if (couponRepository.requestUse(couponId, me.memberId(), now) != 1) {
			throw new ApiException(ErrorCode.COUPON_NOT_AVAILABLE);
		}
		return CouponResponse.from(reload(couponId));
	}

	/** 상대의 사용 요청에 응답 — 상대만 가능, 승인하면 USED / 거절하면 AVAILABLE */
	@Transactional
	public CouponResponse respond(Long couponId, boolean approved, AuthMember me) {
		WishCoupon coupon = findInMyRoom(couponId, me);
		if (coupon.isOwnedBy(me.memberId())) {
			throw new ApiException(ErrorCode.CANNOT_RESPOND_OWN_COUPON);
		}
		LocalDateTime now = LocalDateTime.now();
		int updated = approved ? couponRepository.approve(couponId, now) : couponRepository.reject(couponId, now);
		if (updated != 1) {
			throw new ApiException(ErrorCode.COUPON_NOT_REQUESTED);
		}
		return CouponResponse.from(reload(couponId));
	}

	/** 다른 방의 소원권은 존재 여부도 노출하지 않음(404) */
	private WishCoupon findInMyRoom(Long couponId, AuthMember me) {
		return couponRepository.findById(couponId)
			.filter(c -> c.getRoomId().equals(me.roomId()))
			.orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND));
	}

	private WishCoupon reload(Long couponId) {
		return couponRepository.findById(couponId)
			.orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND));
	}
}
