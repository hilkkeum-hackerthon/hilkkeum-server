package com.hilkkeum.coupon.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.coupon.domain.CouponStatus;
import com.hilkkeum.coupon.domain.WishCoupon;

import jakarta.persistence.LockModeType;

/**
 * 상태 전환은 모두 "예상 상태" 조건을 건 UPDATE → 동시에 눌러도 한 번만 반영된다(반환값 1).
 */
public interface WishCouponRepository extends JpaRepository<WishCoupon, Long> {

	/** 사용 가능(AVAILABLE) 소원권 — 최대 1장 보유 */
	Optional<WishCoupon> findFirstByOwnerMemberIdAndStatusOrderByIssuedAtDesc(Long ownerMemberId, CouponStatus status);

	boolean existsByOwnerMemberIdAndStatus(Long ownerMemberId, CouponStatus status);

	/**
	 * 사용 가능 소원권을 잠금 조회(SELECT ... FOR UPDATE).
	 * 일반 조회는 트랜잭션 시작 시점 스냅샷(REPEATABLE READ)을 읽어 동시 요청이 방금 발급한 소원권을 못 볼 수 있으므로,
	 * 자동 발급 여부 판단에는 항상 최신 데이터를 읽는 이 조회를 사용한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from WishCoupon c where c.ownerMemberId = :ownerId "
		+ "and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE")
	List<WishCoupon> findAvailableForUpdate(@Param("ownerId") Long ownerId);

	/** 방에서 기한이 지난 사용 가능 소원권 */
	@Query("select c from WishCoupon c where c.roomId = :roomId "
		+ "and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE and c.expiresAt <= :now")
	List<WishCoupon> findOverdue(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);

	/** AVAILABLE → EXPIRED (이미 다른 요청이 처리했으면 0) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.EXPIRED "
		+ "where c.id = :id and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE")
	int expire(@Param("id") Long id);

	/** AVAILABLE → USED 즉시 사용 (본인 소원권, 만료 전일 때만). 상대 승인 없음 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update WishCoupon c set c.status = com.hilkkeum.coupon.domain.CouponStatus.USED, c.usedAt = :now "
		+ "where c.id = :id and c.ownerMemberId = :ownerId "
		+ "and c.status = com.hilkkeum.coupon.domain.CouponStatus.AVAILABLE and c.expiresAt > :now")
	int use(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);
}
