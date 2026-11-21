package com.hilkkeum.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 에러 코드. 기능 브랜치에서 필요한 코드를 여기에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

	// 인증 (memberToken)
	MEMBER_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "memberToken 헤더가 필요합니다."),
	MEMBER_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 memberToken입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

	// 방
	INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "잘못된 초대 코드입니다. 다시 입력해 주세요."),
	INVITE_CODE_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 초대 코드입니다."),
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."),
	NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "이 방의 참여자가 아닙니다.");

	private final HttpStatus status;
	private final String message;
}
