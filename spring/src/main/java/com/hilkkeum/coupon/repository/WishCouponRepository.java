package com.hilkkeum.coupon.repository;

import java.time.LocalDateTime;
import java.util.List;

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

	List<WishCoupon> findByOwnerMemberIdOrderByIssuedAtDesc(Long ownerMemberId);

	/** 상대가 사용 요청해서 내가 승인해야 하는 소원권 */
	@Query("select c from WishCoupon c where c.roomId = :roomId and c.ownerMemberId <> :memberId "
		+ "and c.status = com.hilkkeum.coupon.domain.CouponStatus.USE_REQUESTED order by c.useRequestedAt")
	List<WishCoupon> findPendingApprovals(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

	/** 방의 사용 가능 소원권 중 만료된 것을 EXPIRED로 전환 (조회 시점 만료 처리 — 배치 불필요) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.EXPIRED "
		+ "where c.roomId = :roomId and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE "
		+ "and c.expiresAt <= :now")
	int expireOverdue(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);

	/** AVAILABLE → USE_REQUESTED (본인 소원권, 만료 전일 때만) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.USE_REQUESTED, "
		+ "c.useRequestedAt = :now, c.respondedAt = null "
		+ "where c.id = :id and c.ownerMemberId = :ownerId "
		+ "and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE and c.expiresAt > :now")
	int requestUse(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

	/** USE_REQUESTED → USED (상대 승인) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.USED, "
		+ "c.respondedAt = :now, c.usedAt = :now "
		+ "where c.id = :id and c.status = com.hilkkeum.coupon.domain.CouponStatus.USE_REQUESTED")
	int approve(@Param("id") Long id, @Param("now") LocalDateTime now);

	/** USE_REQUESTED → AVAILABLE (상대 거절, 다시 사용 요청 가능) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE, "
		+ "c.respondedAt = :now, c.useRequestedAt = null "
		+ "where c.id = :id and c.status = com.hilkkeum.coupon.domain.CouponStatus.USE_REQUESTED")
	int reject(@Param("id") Long id, @Param("now") LocalDateTime now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = :status where c.id = :id")
	int updateStatus(@Param("id") Long id, @Param("status") CouponStatus status);
}
