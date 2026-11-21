package com.hilkkeum.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hilkkeum.room.domain.RoomMember;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	Optional<RoomMember> findByMemberToken(String memberToken);

	List<RoomMember> findByRoomId(Long roomId);
}
