package com.hilkkeum.game.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hilkkeum.game.domain.Game;

public interface GameRepository extends JpaRepository<Game, Long> {

	/** 게임당 스탬프 처리 1회만: 아직 처리 안 된 게임일 때만 기록(반환값 1), 이미 처리됐으면 0 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Game g set g.stampAwardedAt = :now where g.id = :gameId and g.stampAwardedAt is null")
	int markStampAwarded(@Param("gameId") Long gameId, @Param("now") LocalDateTime now);

	/** games.winner_player_id(game_players.id) → 승자의 room_members.id */
	@Query(value = "select gp.member_id from game_players gp where gp.id = :playerId and gp.game_id = :gameId",
		nativeQuery = true)
	Optional<Long> findMemberIdOfPlayer(@Param("gameId") Long gameId, @Param("playerId") Long playerId);
}
