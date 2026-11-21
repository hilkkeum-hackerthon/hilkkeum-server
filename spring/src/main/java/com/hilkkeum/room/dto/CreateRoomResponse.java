package com.hilkkeum.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST /api/v1/rooms 응답. 앱은 roomId · memberToken을 저장해 재접속에 사용한다.
 */
@Schema(description = "방 생성 응답")
public record CreateRoomResponse(
	@Schema(description = "방 ID", example = "1") Long roomId,
	@Schema(description = "6자리 초대 코드 (0/O · 1/I/L 제외)", example = "K7M3QX") String inviteCode,
	@Schema(description = "방장 멤버 ID", example = "1") Long memberId,
	@Schema(description = "방장 개인 토큰 — 이후 Authorization: Bearer 로 전송",
		example = "TtG75lJAy453UP3LPYOLj7CP0LxPzlOBRzYHOmnoW8Y") String memberToken
) {
}
