package com.hilkkeum.room.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.room.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByInviteCode(String inviteCode);

	boolean existsByInviteCode(String inviteCode);

	/**
	 * PENDING → ACTIVE 조건부 전환. 동시에 두 명이 같은 코드를 입력해도 1명만 성공(반환값 1).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Room r set r.status = com.hilkkeum.room.domain.RoomStatus.ACTIVE "
		+ "where r.id = :roomId and r.status = com.hilkkeum.room.domain.RoomStatus.PENDING")
	int activateIfPending(@Param("roomId") Long roomId);
}
