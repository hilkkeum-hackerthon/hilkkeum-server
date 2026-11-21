package com.hilkkeum.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 — 모든 에러를 { "code", "message" } 형식과 알맞은 HTTP 상태로 응답한다.
 * <ul>
 *   <li>400 요청 형식 오류 (검증 실패 · 깨진 JSON · 타입 불일치 · 누락 파라미터)</li>
 *   <li>401/403/404/409 비즈니스 예외 (ApiException → ErrorCode 의 상태)</li>
 *   <li>404 없는 경로 · 405 지원하지 않는 메서드 · 415 Content-Type 오류</li>
 *   <li>409 DB 제약 위반 (동시 요청 등)</li>
 *   <li>500 그 외 예상하지 못한 오류 (로그 기록)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 비즈니스 예외: ErrorCode 에 정의된 상태(401/403/404/409 등) */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
		return respond(e.getErrorCode());
	}

	/** 400: @Valid 검증 실패 — 첫 번째 필드 오류를 안내 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(err -> err.getField() + ": " + err.getDefaultMessage())
			.orElse(ErrorCode.INVALID_REQUEST.getMessage());
		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
	}

	/** 400: 깨진 JSON · 경로/쿼리 타입 불일치 · 필수 파라미터/헤더 누락 */
	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		MissingServletRequestParameterException.class,
		MissingRequestHeaderException.class
	})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
		return respond(ErrorCode.INVALID_REQUEST);
	}

	/** 404: 존재하지 않는 경로 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
		return respond(ErrorCode.NOT_FOUND);
	}

	/** 405: 경로는 있지만 지원하지 않는 HTTP 메서드 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return respond(ErrorCode.METHOD_NOT_ALLOWED);
	}

	/** 415: JSON 이 아닌 Content-Type */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
		return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
	}

	/** 409: DB 유니크 · 외래키 등 제약 위반 (동시 요청 경합 등) */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataConflict(DataIntegrityViolationException e) {
		log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
		return respond(ErrorCode.DATA_CONFLICT);
	}

	/** 500: 예상하지 못한 오류 — 내부 정보는 응답하지 않고 로그로만 남김 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
		log.error("Unhandled exception", e);
		return respond(ErrorCode.INTERNAL_ERROR);
	}

	private ResponseEntity<ErrorResponse> respond(ErrorCode code) {
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code));
	}
}
