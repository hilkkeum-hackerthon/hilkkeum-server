package com.hilkkeum.coupon.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;

/**
 * 상태 전환은 모두 "예상 상태" 조건을 건 UPDATE → 동시에 눌러도 한 번만 반영된다(반환값 1).
 */
public interface WishCouponRepository extends JpaRepository<WishCoupon, Long> {

	/** 사용 가능(AVAILABLE) 소원권 — 최대 1장 보유 */
	Optional<WishCoupon> findFirstByOwnerMemberIdAndStatusOrderByIssuedAtDesc(Long ownerMemberId, CouponStatus status);

	boolean existsByOwnerMemberIdAndStatus(Long ownerMemberId, CouponStatus status);

	/** 방의 사용 가능 소원권 중 만료된 것을 EXPIRED로 전환 (조회 시점 만료 처리 — 배치 불필요) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.EXPIRED "
		+ "where c.roomId = :roomId and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE "
		+ "and c.expiresAt <= :now")
	int expireOverdue(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);

	/** AVAILABLE → USED 즉시 사용 (본인 소원권, 만료 전일 때만). 상대 승인 없음 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.USED, c.usedAt = :now "
		+ "where c.id = :id and c.ownerMemberId = :ownerId "
		+ "and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE and c.expiresAt > :now")
	int use(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = :status where c.id = :id")
	int updateStatus(@Param("id") Long id, @Param("status") CouponStatus status);
}
