package com.hilkkeum.coupon.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;
import com.hilkkeum.coupon.dto.CouponResponse;
import com.hilkkeum.coupon.dto.CouponsResponse;
import com.hilkkeum.coupon.dto.ExchangeCouponResponse;
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

	/**
	 * 스탬프 3개 차감 후 소원권 1장 발급 (발급일로부터 1개월 유효).
	 * - 사용 가능한 소원권을 이미 보유하면 409 (COUPON_ALREADY_HELD)
	 * - 스탬프가 3개가 아니면 409 (NOT_ENOUGH_STAMPS)
	 * - 차감은 조건부 UPDATE라 동시에 여러 번 눌러도 한 장만 발급, 차감·발급은 한 트랜잭션
	 */
	@Transactional
	public ExchangeCouponResponse exchange(AuthMember me) {
		LocalDateTime now = LocalDateTime.now();
		couponRepository.expireOverdue(me.roomId(), now);
		if (couponRepository.existsByOwnerMemberIdAndStatus(me.memberId(), CouponStatus.AVAILABLE)) {
			throw new ApiException(ErrorCode.COUPON_ALREADY_HELD);
		}
		if (roomMemberRepository.useStamps(me.memberId(), STAMPS_PER_COUPON) != 1) {
			throw new ApiException(ErrorCode.NOT_ENOUGH_STAMPS);
		}
		WishCoupon coupon = couponRepository.save(WishCoupon.issue(me.roomId(), me.memberId(), now));
		return ExchangeCouponResponse.of(coupon, currentStamps(me.memberId()));
	}

	/** 보유 소원권(최대 1장, 만료 · 사용 완료 제외) + 남은 일수. 조회 시점에 만료 처리 */
	@Transactional
	public CouponsResponse getCoupons(AuthMember me) {
		LocalDateTime now = LocalDateTime.now();
		couponRepository.expireOverdue(me.roomId(), now);

		CouponsResponse.ActiveCoupon active = couponRepository
			.findFirstByOwnerMemberIdAndStatusOrderByIssuedAtDesc(me.memberId(), CouponStatus.AVAILABLE)
			.map(c -> CouponsResponse.ActiveCoupon.of(c, now))
			.orElse(null);
		return new CouponsResponse(active != null, active);
	}

	/** 사용 가능한 소원권 보유 여부 (스탬프 조회의 교환 가능 여부에 사용) */
	@Transactional
	public boolean hasActiveCoupon(Long memberId, Long roomId) {
		couponRepository.expireOverdue(roomId, LocalDateTime.now());
		return couponRepository.existsByOwnerMemberIdAndStatus(memberId, CouponStatus.AVAILABLE);
	}

	/**
	 * 소원권 사용 — 상대 승인 없이 즉시 사용 처리 (AVAILABLE → USED).
	 * 본인 소원권 · 만료 전일 때만. 동시에 여러 번 눌러도 한 번만 사용된다.
	 */
	@Transactional
	public CouponResponse use(Long couponId, AuthMember me) {
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
		if (couponRepository.use(couponId, me.memberId(), now) != 1) {
			throw new ApiException(ErrorCode.COUPON_NOT_AVAILABLE);
		}
		return CouponResponse.from(reload(couponId));
	}

	private int currentStamps(Long memberId) {
		return roomMemberRepository.findById(memberId).map(RoomMember::getStampCount).orElse(0);
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
