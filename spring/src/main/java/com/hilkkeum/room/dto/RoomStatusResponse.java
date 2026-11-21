package com.hilkkeum.room.dto;

import java.util.List;

import com.hilkkeum.room.domain.MemberRole;
import com.hilkkeum.room.domain.RoomStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * GET /api/v1/rooms/{roomId} 응답 (대기 화면 폴링 · 재접속 복구).
 * memberCount < capacity 이면 상대 입장 대기 상태. 상대방 토큰은 포함하지 않는다.
 */
@Schema(description = "방 정보")
public record RoomStatusResponse(
	@Schema(description = "방 ID", example = "1") Long roomId,
	@Schema(description = "방 상태 (PENDING: 상대 입장 대기 / ACTIVE: 2명 입장 완료)", example = "ACTIVE") RoomStatus status,
	@Schema(description = "현재 참가자 수", example = "2") int memberCount,
	@Schema(description = "정원", example = "2") int capacity,
	@Schema(description = "참가자 목록 (토큰 미포함)") List<Member> members
) {

	@Schema(description = "참가자")
	public record Member(
		@Schema(description = "멤버 ID", example = "1") Long memberId,
		@Schema(description = "역할 (CREATOR: 방장 / JOINER: 참여자)", example = "CREATOR") MemberRole role
	) {
	}
}
