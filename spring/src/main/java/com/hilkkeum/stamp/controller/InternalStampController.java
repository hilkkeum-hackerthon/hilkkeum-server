package com.hilkkeum.stamp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.common.exception.ErrorResponse;
import com.hilkkeum.stamp.dto.AwardStampRequest;
import com.hilkkeum.stamp.dto.AwardStampResponse;
import com.hilkkeum.stamp.service.StampService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 내부 서버 연동 API (Node → Spring). 외부 요청은 nginx에서 /internal/** 로 차단되고,
 * docker 내부망(http://spring:8080)으로만 호출된다.
 * INTERNAL_API_KEY 환경변수를 설정하면 X-Internal-Key 헤더도 검사한다(서버 간 인증은 별도 협의).
 */
@Tag(name = "내부 서버연동")
@RestController
@RequestMapping("/internal/v1/stamps")
public class InternalStampController {

	private final StampService stampService;
	private final String internalApiKey;

	public InternalStampController(StampService stampService,
		@Value("${internal.api-key:}") String internalApiKey) {
		this.stampService = stampService;
		this.internalApiKey = internalApiKey;
	}

	@Operation(summary = "스탬프 적립 연동",
		description = """
			게임 승자가 최종 확정되면 해당 사용자에게 스탬프 1개를 적립한다. (기능명세서 4.7 / 4.8)
			Node가 결산을 확정한 뒤 docker 내부망으로 호출 — **외부(Swagger · 앱)에서는 nginx가 차단(404)**.
			- DB에 확정된 승자(games.winner_player_id)와 요청 정보가 일치해야 함
			- 게임당 1회만 적립 — 중복 요청은 `awarded=false` (재시도해도 안전)
			- 보유량이 3개면 추가 적립하지 않음(`awarded=false`)
			- 처리 기록과 적립은 하나의 트랜잭션, 소원권은 자동 발급하지 않음""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "처리 완료 (awarded=true 적립 / false 이미 처리 · 보유량 3개)"),
		@ApiResponse(responseCode = "400", description = "요청 정보 누락",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "403", description = "서버 간 인증 오류 (INTERNAL_API_KEY 설정 시)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "게임 또는 승자 정보를 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "결산이 확정되지 않았거나(GAME_NOT_SETTLED) 승자 정보가 일치하지 않음(WINNER_MISMATCH)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/award")
	public AwardStampResponse award(
		@Parameter(description = "서버 간 인증 키 (INTERNAL_API_KEY 설정 시에만 필요)")
		@RequestHeader(value = "X-Internal-Key", required = false) String key,
		@Valid @RequestBody AwardStampRequest request) {
		if (StringUtils.hasText(internalApiKey) && !internalApiKey.equals(key)) {
			throw new ApiException(ErrorCode.INTERNAL_FORBIDDEN);
		}
		return stampService.award(request.gameId(), request.winnerMemberId());
	}
}
