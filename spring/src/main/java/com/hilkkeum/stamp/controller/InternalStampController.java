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
import com.hilkkeum.stamp.dto.AwardStampRequest;
import com.hilkkeum.stamp.dto.AwardStampResponse;
import com.hilkkeum.stamp.service.StampService;

import jakarta.validation.Valid;

/**
 * 내부 서버 연동 API (Node → Spring). 외부 요청은 nginx에서 /internal/** 로 차단되고,
 * docker 내부망(http://spring:8080)으로만 호출된다.
 * INTERNAL_API_KEY 환경변수를 설정하면 X-Internal-Key 헤더도 검사한다(이중 보호).
 */
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

	@PostMapping("/award")
	public AwardStampResponse award(
		@RequestHeader(value = "X-Internal-Key", required = false) String key,
		@Valid @RequestBody AwardStampRequest request) {
		if (StringUtils.hasText(internalApiKey) && !internalApiKey.equals(key)) {
			throw new ApiException(ErrorCode.INTERNAL_FORBIDDEN);
		}
		return stampService.award(request.gameId(), request.winnerMemberId());
	}
}
