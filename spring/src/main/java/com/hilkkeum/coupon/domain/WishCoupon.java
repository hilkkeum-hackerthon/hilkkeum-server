package com.hilkkeum.coupon.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소원권. 상태 전환은 동시성 보장을 위해 WishCouponRepository의 조건부 UPDATE로 처리한다.
 */
@Entity
@Table(name = "wish_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishCoupon {

	/** 발급 후 유효기간 */
	public static final int VALID_MONTHS = 1;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "owner_member_id", nullable = false)
	private Long ownerMemberId;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CouponStatus status;

	@Column(name = "wish_content")
	private String wishContent;

	@Column(name = "use_requested_at")
	private LocalDateTime useRequestedAt;

	@Column(name = "responded_at")
	private LocalDateTime respondedAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static WishCoupon issue(Long roomId, Long ownerMemberId, LocalDateTime now) {
		LocalDateTime issuedAt = now.withNano(0);  // DB DATETIME(초 단위)와 응답 값을 일치시킴
		WishCoupon coupon = new WishCoupon();
		coupon.roomId = roomId;
		coupon.ownerMemberId = ownerMemberId;
		coupon.issuedAt = issuedAt;
		coupon.expiresAt = issuedAt.plusMonths(VALID_MONTHS);
		coupon.status = CouponStatus.AVAILABLE;
		return coupon;
	}

	public boolean isOwnedBy(Long memberId) {
		return ownerMemberId.equals(memberId);
	}

	public boolean isExpiredAt(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
