package com.hilkkeum.room.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.room.domain.RoomMember;

import jakarta.persistence.LockModeType;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	Optional<RoomMember> findByMemberToken(String memberToken);

	List<RoomMember> findByRoomIdOrderByIdAsc(Long roomId);

	/**
	 * 스탬프 · 소원권 상태를 바꾸기 전에 멤버 행을 잠금(SELECT ... FOR UPDATE).
	 * 같은 사람에 대한 적립 · 자동 발급 · 사용이 동시에 들어와도 순서대로 처리되어 소원권이 중복 발급되지 않는다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from RoomMember m where m.id = :memberId")
	Optional<RoomMember> findByIdForUpdate(@Param("memberId") Long memberId);

	/** 만료된 소원권 주인들의 스탬프 초기화 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RoomMember m set m.stampCount = 0 where m.id in :memberIds")
	int resetStamps(@Param("memberIds") Collection<Long> memberIds);
}
