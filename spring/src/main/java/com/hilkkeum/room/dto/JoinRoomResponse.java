package com.hilkkeum.room.dto;

import com.hilkkeum.room.domain.MemberRole;
import com.hilkkeum.room.domain.RoomStatus;

/**
 * POST /api/v1/rooms/join 응답. 앱은 roomId·memberToken을 저장해 재접속에 사용한다.
 */
public record JoinRoomResponse(
	Long roomId,
	String memberToken,
	MemberRole role,
	RoomStatus status
) {
}
