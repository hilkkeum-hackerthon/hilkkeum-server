package com.hilkkeum.room.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.auth.CurrentMember;
import com.hilkkeum.room.dto.CreateRoomResponse;
import com.hilkkeum.room.dto.JoinRoomRequest;
import com.hilkkeum.room.dto.JoinRoomResponse;
import com.hilkkeum.room.dto.RoomStatusResponse;
import com.hilkkeum.room.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;

	/** 방 생성 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateRoomResponse create() {
		return roomService.create();
	}

	/** 초대 코드 입장 */
	@PostMapping("/join")
	public JoinRoomResponse join(@Valid @RequestBody JoinRoomRequest request) {
		return roomService.join(request.inviteCode());
	}

	/** 입장 상태 조회 (Header: memberToken) */
	@GetMapping("/{roomId}")
	public RoomStatusResponse getStatus(@PathVariable Long roomId, @CurrentMember AuthMember me) {
		return roomService.getStatus(roomId, me);
	}
}
