package com.hilkkeum.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 에러 응답 형식: { "code": "MEMBER_TOKEN_INVALID", "message": "유효하지 않은 memberToken입니다." }
 */
@Schema(description = "에러 응답")
public record ErrorResponse(
	@Schema(description = "에러 코드", example = "MEMBER_TOKEN_INVALID") String code,
	@Schema(description = "사용자 안내 문구", example = "유효하지 않은 memberToken입니다.") String message
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}
}
