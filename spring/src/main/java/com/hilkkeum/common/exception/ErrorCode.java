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
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type: application/json 으로 요청해 주세요."),
	DATA_CONFLICT(HttpStatus.CONFLICT, "요청이 현재 데이터 상태와 충돌합니다. 다시 시도해 주세요."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

	// 인증 (memberToken)
	MEMBER_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Authorization: Bearer {memberToken} 헤더가 필요합니다."),
	MEMBER_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 memberToken입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

	// 방
	INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "잘못된 초대 코드입니다. 다시 입력해 주세요."),
	INVITE_CODE_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 초대 코드입니다."),
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."),
	NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "이 방의 참여자가 아닙니다."),

	// 스탬프 (내부 연동)
	INTERNAL_FORBIDDEN(HttpStatus.FORBIDDEN, "내부 API 인증에 실패했습니다."),
	GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다."),
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
	GAME_NOT_SETTLED(HttpStatus.CONFLICT, "결산이 확정되지 않은 게임입니다."),
	WINNER_MISMATCH(HttpStatus.CONFLICT, "요청한 승자가 게임에 확정된 승자와 일치하지 않습니다."),

	// 소원권
	NOT_ENOUGH_STAMPS(HttpStatus.CONFLICT, "스탬프가 3개 있어야 소원권으로 교환할 수 있습니다."),
	COUPON_ALREADY_HELD(HttpStatus.CONFLICT, "이미 사용 가능한 소원권을 보유하고 있습니다."),
	COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "소원권을 찾을 수 없습니다."),
	NOT_COUPON_OWNER(HttpStatus.FORBIDDEN, "본인의 소원권만 사용할 수 있습니다."),
	COUPON_NOT_AVAILABLE(HttpStatus.CONFLICT, "사용할 수 없는 상태의 소원권입니다. (이미 사용됨)"),
	COUPON_EXPIRED(HttpStatus.CONFLICT, "만료된 소원권입니다.");

	private final HttpStatus status;
	private final String message;
}
