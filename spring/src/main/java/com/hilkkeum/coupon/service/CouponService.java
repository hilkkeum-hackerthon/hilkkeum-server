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

/**
 * 소원권 규칙
 * - 스탬프 3개가 되면 소원권 자동 발급 (스탬프는 3개로 유지) — StampService에서 호출
 * - 최대 1장 보유, 발급일부터 1개월 유효
 * - 보유자 본인이 '사용'을 누르면 소진 → 스탬프 0으로 초기화
 * - 만료되면 알림 없이 사라짐 → 스탬프 0으로 초기화
 */
@Service
@RequiredArgsConstructor
public class CouponService {

	private final WishCouponRepository couponRepository;
	private final RoomMemberRepository roomMemberRepository;

	/** 보유 소원권(최대 1장) + 남은 일수. 조회 시점에 만료 처리 */
	@Transactional
	public CouponsResponse getCoupons(AuthMember me) {
		LocalDateTime now = LocalDateTime.now();
		expireOverdue(me.roomId(), now);

		CouponsResponse.ActiveCoupon active = couponRepository
			.findFirstByOwnerMemberIdAndStatusOrderByIssuedAtDesc(me.memberId(), CouponStatus.AVAILABLE)
			.map(c -> CouponsResponse.ActiveCoupon.of(c, now))
			.orElse(null);
		return new CouponsResponse(active != null, active);
	}

	/** 사용 가능한 소원권 보유 여부 — 화면 표시용 (호출 전에 expireOverdue로 만료 처리할 것) */
	@Transactional(readOnly = true)
	public boolean hasActiveCoupon(Long memberId) {
		return couponRepository.existsByOwnerMemberIdAndStatus(memberId, CouponStatus.AVAILABLE);
	}

	/** 사용 가능한 소원권 보유 여부 — 자동 발급 판단용 (잠금 조회로 최신 데이터 기준) */
	@Transactional
	public boolean hasActiveCouponForUpdate(Long memberId) {
		return !couponRepository.findAvailableForUpdate(memberId).isEmpty();
	}

	/** 소원권 자동 발급 — 스탬프가 가득 찼고 소원권이 없을 때 StampService가 호출 (멤버 행 잠금 상태) */
	@Transactional
	public WishCoupon issue(RoomMember owner, LocalDateTime now) {
		return couponRepository.save(WishCoupon.issue(owner.getRoom().getId(), owner.getId(), now));
	}

	/**
	 * 소원권 사용 — 보유자 본인이 누르면 즉시 소진(AVAILABLE → USED)하고 스탬프를 0으로 초기화.
	 * 만료 전에만 가능, 중복 요청에도 한 번만 처리된다.
	 */
	@Transactional
	public CouponResponse use(Long couponId, AuthMember me) {
		LocalDateTime now = LocalDateTime.now();
		WishCoupon coupon = findInMyRoom(couponId, me);
		if (!coupon.isOwnedBy(me.memberId())) {
			throw new ApiException(ErrorCode.NOT_COUPON_OWNER);
		}
		if (coupon.getStatus() == CouponStatus.EXPIRED
			|| (coupon.getStatus() == CouponStatus.AVAILABLE && coupon.isExpiredAt(now))) {
			expireOverdue(me.roomId(), now);
			throw new ApiException(ErrorCode.COUPON_EXPIRED);
		}
		if (couponRepository.use(couponId, me.memberId(), now) != 1) {
			throw new ApiException(ErrorCode.COUPON_NOT_AVAILABLE);
		}
		roomMemberRepository.findByIdForUpdate(me.memberId()).ifPresent(RoomMember::resetStamps);
		return CouponResponse.from(reload(couponId));
	}

	/**
	 * 방의 만료된 소원권을 EXPIRED로 전환하고(조회 목록에서 사라짐), 주인의 스탬프를 0으로 초기화.
	 * 별도 배치 없이 조회 · 적립 · 사용 시점에 처리한다.
	 */
	@Transactional
	public void expireOverdue(Long roomId, LocalDateTime now) {
		List<Long> owners = couponRepository.findOverdue(roomId, now).stream()
			.filter(c -> couponRepository.expire(c.getId()) == 1)
			.map(WishCoupon::getOwnerMemberId)
			.distinct()
			.toList();
		if (!owners.isEmpty()) {
			roomMemberRepository.resetStamps(owners);
		}
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
