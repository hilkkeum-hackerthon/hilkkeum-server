package com.hilkkeum.common.auth;

import org.springframework.core.MethodParameter;
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
 * memberToken 헤더 → AuthMember 변환. (API 명세서: Header memberToken)
 */
@Component
@RequiredArgsConstructor
public class MemberTokenArgumentResolver implements HandlerMethodArgumentResolver {

	public static final String HEADER_NAME = "memberToken";

	private final RoomMemberRepository roomMemberRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentMember.class)
			&& AuthMember.class.equals(parameter.getParameterType());
	}

	@Override
	public AuthMember resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String token = webRequest.getHeader(HEADER_NAME);
		if (!StringUtils.hasText(token)) {
			throw new ApiException(ErrorCode.MEMBER_TOKEN_MISSING);
		}
		RoomMember member = roomMemberRepository.findByMemberToken(token.trim())
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_TOKEN_INVALID));
		return new AuthMember(member.getId(), member.getRoom().getId(), member.getRole());
	}
}
