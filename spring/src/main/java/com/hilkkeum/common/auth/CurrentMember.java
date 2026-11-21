package com.hilkkeum.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 Authorization: Bearer {memberToken} 헤더로 인증된 멤버가 주입된다.
 * 헤더가 없거나 토큰이 유효하지 않으면 401.
 *
 * <pre>
 * &#64;GetMapping("/api/v1/coupons")
 * public CouponsResponse getCoupons(&#64;CurrentMember AuthMember me) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
}
