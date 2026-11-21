package com.hilkkeum.room.dto;

import com.hilkkeum.room.domain.MemberRole;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST /api/v1/rooms/join 응답. 앱은 roomId · memberToken을 저장해 재접속에 사용한다.
 */
@Schema(description = "초대 코드 입장 응답")
public record JoinRoomResponse(
	@Schema(description = "방 ID", example = "1") Long roomId,
	@Schema(description = "참여자 멤버 ID", example = "2") Long memberId,
	@Schema(description = "참여자 개인 토큰 — 이후 Authorization: Bearer 로 전송",
		example = "CwxPmmL6kOAZ-PhQQTxXswtUOCU2wD6sL0qWsW35Du0") String memberToken,
	@Schema(description = "역할", example = "JOINER") MemberRole role
) {
}
