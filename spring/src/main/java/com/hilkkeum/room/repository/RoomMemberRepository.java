package com.hilkkeum.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.room.domain.RoomMember;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	Optional<RoomMember> findByMemberToken(String memberToken);

	List<RoomMember> findByRoomIdOrderByIdAsc(Long roomId);

	/** 스탬프 +1 — 보유량이 max 미만일 때만 증가(반환값 1). max에 도달했으면 적립하지 않음(0) */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RoomMember m set m.stampCount = m.stampCount + 1 where m.id = :memberId and m.stampCount < :max")
	int addStampUnderCap(@Param("memberId") Long memberId, @Param("max") int max);

	/** 스탬프 차감 — amount 이상일 때만 성공(반환값 1). 동시에 교환해도 중복 차감되지 않음 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RoomMember m set m.stampCount = m.stampCount - :amount "
		+ "where m.id = :memberId and m.stampCount >= :amount")
	int useStamps(@Param("memberId") Long memberId, @Param("amount") int amount);
}
