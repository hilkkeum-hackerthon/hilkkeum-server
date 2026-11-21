package com.hilkkeum.coupon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.auth.CurrentMember;
import com.hilkkeum.common.config.OpenApiConfig;
import com.hilkkeum.common.exception.ErrorResponse;
import com.hilkkeum.coupon.dto.CouponResponse;
import com.hilkkeum.coupon.dto.CouponsResponse;
import com.hilkkeum.coupon.dto.ExchangeCouponResponse;
import com.hilkkeum.coupon.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "스탬프·소원권")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

	private final CouponService couponService;

	@Operation(summary = "소원권 교환",
		description = """
			스탬프 3개를 보유한 사용자가 교환 버튼을 누르면 스탬프를 차감하고 소원권 1장을 발급한다. (기능명세서 4.8 / 화면 15)
			- 스탬프가 3개일 때만 교환 가능, 사용 가능한 소원권은 최대 1장
			- 발급일로부터 1개월 유효
			- 스탬프 차감 · 소원권 발급은 하나의 트랜잭션, 중복 요청에도 한 장만 발급""",
		security = @SecurityRequirement(name = OpenApiConfig.BEARER))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "발급됨"),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "스탬프 부족(NOT_ENOUGH_STAMPS) 또는 소원권 보유 중(COUPON_ALREADY_HELD)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = "{\"code\":\"COUPON_ALREADY_HELD\",\"message\":\"이미 사용 가능한 소원권을 보유하고 있습니다.\"}")))
	})
	@PostMapping("/exchange")
	@ResponseStatus(HttpStatus.CREATED)
	public ExchangeCouponResponse exchange(@Parameter(hidden = true) @CurrentMember AuthMember me) {
		return couponService.exchange(me);
	}

	@Operation(summary = "소원권 조회",
		description = """
			현재 보유한 소원권과 만료일, 남은 유효기간을 조회한다. (기능명세서 4.8 / 화면 15)
			- 본인 소원권만, 사용 가능한 소원권은 최대 1장
			- 만료 · 사용 완료된 소원권은 포함하지 않음 (조회 시점에 만료 처리)
			- `remainingDays`로 D-day 표시, 만료 경고 알림은 보내지 않음""",
		security = @SecurityRequirement(name = OpenApiConfig.BEARER))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK (보유하지 않으면 hasActiveCoupon=false, coupon=null)"),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping
	public CouponsResponse getCoupons(@Parameter(hidden = true) @CurrentMember AuthMember me) {
		return couponService.getCoupons(me);
	}

	@Operation(summary = "소원권 사용",
		description = """
			소원권 사용 버튼 → **상대 승인 없이 즉시 사용** 처리 (AVAILABLE → USED).
			- 본인 소원권만, 만료 전에만 사용 가능
			- 중복 요청에도 한 번만 사용 처리""",
		security = @SecurityRequirement(name = OpenApiConfig.BEARER))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "사용 완료"),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "403", description = "상대의 소원권 (NOT_COUPON_OWNER)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "없거나 다른 방의 소원권 (COUPON_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "이미 사용됨(COUPON_NOT_AVAILABLE) 또는 만료(COUPON_EXPIRED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/{couponId}/use")
	public CouponResponse use(@Parameter(description = "소원권 ID", example = "1") @PathVariable Long couponId,
		@Parameter(hidden = true) @CurrentMember AuthMember me) {
		return couponService.use(couponId, me);
	}
}
