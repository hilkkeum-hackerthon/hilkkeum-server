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
import com.hilkkeum.common.config.OpenApiConfig;
import com.hilkkeum.common.exception.ErrorResponse;
import com.hilkkeum.room.dto.CreateRoomResponse;
import com.hilkkeum.room.dto.JoinRoomRequest;
import com.hilkkeum.room.dto.JoinRoomResponse;
import com.hilkkeum.room.dto.RoomStatusResponse;
import com.hilkkeum.room.service.RoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "방·사용자")
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;

	@Operation(summary = "방 생성",
		description = """
			새로운 2인용 방을 생성하고 6자리 초대 코드와 방장 식별 토큰을 발급한다. (기능명세서 4.1 / 4.2)
			- 초대 코드는 중복되지 않도록 생성
			- 방 상태 `PENDING`, 생성자는 `CREATOR`
			- 재접속할 수 있도록 멤버 토큰 발급 → 앱에 roomId · memberToken 저장""")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "생성됨"),
		@ApiResponse(responseCode = "500", description = "코드 발급 실패",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateRoomResponse create() {
		return roomService.create();
	}

	@Operation(summary = "초대 코드 입장",
		description = """
			방장이 공유한 6자리 초대 코드로 같은 방에 참여한다. (기능명세서 4.1)
			- 입장 성공 시 `JOINER`, 두 명이 모이면 방 `ACTIVE`
			- 최대 2명, 동시에 입장해도 정원을 초과하지 않음
			- 초대 코드는 방이 성립되기 전까지 유효 (만료 정책은 기능명세서 · ERD 최종 합의 필요)""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "입장 완료"),
		@ApiResponse(responseCode = "400", description = "초대 코드 누락",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 초대 코드",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = "{\"code\":\"INVALID_INVITE_CODE\",\"message\":\"잘못된 초대 코드입니다. 다시 입력해 주세요.\"}"))),
		@ApiResponse(responseCode = "409", description = "이미 2명이 참여한 방 (이미 사용된 코드)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = "{\"code\":\"INVITE_CODE_ALREADY_USED\",\"message\":\"이미 사용된 초대 코드입니다.\"}")))
	})
	@PostMapping("/join")
	public JoinRoomResponse join(@Valid @RequestBody JoinRoomRequest request) {
		return roomService.join(request.inviteCode());
	}

	@Operation(summary = "방 정보 조회",
		description = """
			현재 방의 상태와 상대방 입장 여부를 조회한다. (기능명세서 4.1 / 화면 구성 2, 4)
			- 해당 방의 참가자만 조회 가능
			- 참가자가 1명이면(memberCount < capacity) 상대 입장 대기 표시
			- 상대방 토큰은 응답에 포함하지 않음
			- 안드로이드는 주기적 조회(폴링)로 상대 입장 확인""",
		security = @SecurityRequirement(name = OpenApiConfig.BEARER))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "401", description = "토큰 없음/무효",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "403", description = "해당 방의 참가자가 아님",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "방이 존재하지 않음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/{roomId}")
	public RoomStatusResponse getStatus(@Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
		@Parameter(hidden = true) @CurrentMember AuthMember me) {
		return roomService.getStatus(roomId, me);
	}
}
