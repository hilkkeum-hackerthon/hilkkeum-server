package com.hilkkeum.common.exception;

/**
 * 에러 응답 형식: { "code": "MEMBER_TOKEN_INVALID", "message": "유효하지 않은 memberToken입니다." }
 */
public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}
}
