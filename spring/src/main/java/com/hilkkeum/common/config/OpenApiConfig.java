package com.hilkkeum.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Swagger(OpenAPI) 설정 — Spring 담당 API(방·사용자 / 스탬프·소원권 / 내부 서버연동).
 * Node(게임) API는 Node 서버의 /swagger 에서 제공.
 */
@Configuration
public class OpenApiConfig {

	public static final String BEARER = "memberToken";

	@Bean
	public OpenAPI hilkkeumOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("힐끔(hilkkeum) API — Spring")
				.version("1.0.0")
				.description("""
					방 · 사용자 / 스탬프 · 소원권 / 내부 서버연동 API.

					- **인증**: 방 생성 · 입장 응답의 `memberToken`을 `Authorization: Bearer {memberToken}` 헤더로 전송 \
					(우측 **Authorize**에 토큰만 입력)
					- **에러 응답**: `{ "code": "ERROR_CODE", "message": "안내 문구" }`
					- **시간**: ISO-8601, 한국 시간(+09:00)
					- **게임(라운드) API**는 Node 서버 문서 `/swagger` 참고
					"""))
			.components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.description("방 생성 · 입장 시 발급받은 memberToken (43자)")))
			.tags(List.of(
				new Tag().name("방·사용자").description("방 생성, 초대 코드 입장, 방 정보 조회"),
				new Tag().name("스탬프·소원권").description("스탬프 조회, 소원권 교환 · 조회 · 사용(즉시)"),
				new Tag().name("내부 서버연동").description(
					"Node → Spring 전용. 외부 요청은 nginx가 차단(404), docker 내부망 `http://spring:8080`으로만 호출")));
	}
}
