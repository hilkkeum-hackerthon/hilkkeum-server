package com.hilkkeum.room.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
	@NotBlank(message = "초대 코드를 입력해 주세요.")
	String inviteCode
) {
}
