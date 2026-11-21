package com.hilkkeum.room.dto;

import com.hilkkeum.room.domain.MemberRole;
import com.hilkkeum.room.domain.RoomStatus;

/**
 * GET /api/v1/rooms/{roomId} 응답 (대기 화면 폴링 · 재접속 복구용).
 *
 * @param inviteCode   방이 성립되기 전(PENDING)에만 내려준다 → 대기 화면에서 코드 다시 보기/복사
 * @param partnerJoined 두 번째 참가자 입장 여부. true가 되면 설정 화면으로 이동
 */
public record RoomStatusResponse(
	Long roomId,
	RoomStatus status,
	String inviteCode,
	MemberRole myRole,
	int memberCount,
	boolean partnerJoined
) {
}
