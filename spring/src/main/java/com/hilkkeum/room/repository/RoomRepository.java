package com.hilkkeum.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hilkkeum.room.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
