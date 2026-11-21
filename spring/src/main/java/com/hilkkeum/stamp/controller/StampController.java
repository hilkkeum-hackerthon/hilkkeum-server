package com.hilkkeum.stamp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.auth.CurrentMember;
import com.hilkkeum.common.config.OpenApiConfig;
import com.hilkkeum.common.exception.ErrorResponse;
import com.hilkkeum.stamp.dto.StampsResponse;
import com.hilkkeum.stamp.service.StampService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "스탬프·소원권")
@RestController
@RequestMapping("/api/v1/stamps")
@RequiredArgsConstructor
public class StampController {

	private final StampService stampService;

	@Operation(summary = "스탬프 조회", description = "보유 스탬프(최대 3)와 소원권 보유 여부를 조회한다. 스탬프 3개가 되면 소원권이 자동 발급되며, 소원권을 사용 · 만료하면 스탬프가 0이 된다.",
		security = @SecurityRequirement(name = OpenApiConfig.BEARER))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "401", description = "토큰 없음/무효",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping
	public StampsResponse getStamps(@Parameter(hidden = true) @CurrentMember AuthMember me) {
		return stampService.getStamps(me.memberId(), me.roomId());
	}
}
