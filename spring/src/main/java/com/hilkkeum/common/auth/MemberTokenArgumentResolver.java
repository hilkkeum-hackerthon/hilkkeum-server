package com.hilkkeum.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Authorization: Bearer {memberToken} → AuthMember 변환.
 * Spring · Node 공통 인증 방식 (팀 합의: Authorization Bearer로 통일).
 */
@Component
@RequiredArgsConstructor
public class MemberTokenArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String BEARER_PREFIX = "Bearer ";

	private final RoomMemberRepository roomMemberRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentMember.class)
			&& AuthMember.class.equals(parameter.getParameterType());
	}

	@Override
	public AuthMember resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String token = extractBearerToken(webRequest.getHeader(HttpHeaders.AUTHORIZATION));
		if (token == null) {
			throw new ApiException(ErrorCode.MEMBER_TOKEN_MISSING);
		}
		RoomMember member = roomMemberRepository.findByMemberToken(token)
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_TOKEN_INVALID));
		return new AuthMember(member.getId(), member.getRoom().getId(), member.getRole());
	}

	/** "Bearer abc" → "abc" (Bearer 대소문자 무관). 형식이 아니면 null */
	static String extractBearerToken(String authorization) {
		if (!StringUtils.hasText(authorization)) {
			return null;
		}
		String value = authorization.trim();
		if (value.length() <= BEARER_PREFIX.length()
			|| !value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return null;
		}
		String token = value.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}
}
