package com.hilkkeum.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "초대 코드 입장 요청")
public record JoinRoomRequest(
	@Schema(description = "방장이 공유한 6자리 초대 코드 (소문자 · 앞뒤 공백 허용)", example = "K7M3QX")
	@NotBlank(message = "초대 코드를 입력해 주세요.")
	String inviteCode
) {
}
