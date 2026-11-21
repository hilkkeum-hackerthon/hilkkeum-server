package com.hilkkeum.game.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.game.domain.Game;

public interface GameRepository extends JpaRepository<Game, Long> {

	/** 게임당 스탬프 1회만: 아직 적립 안 된 게임일 때만 기록(반환값 1), 이미 적립됐으면 0 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Game g set g.stampAwardedAt = :now where g.id = :gameId and g.stampAwardedAt is null")
	int markStampAwarded(@Param("gameId") Long gameId, @Param("now") LocalDateTime now);
}
