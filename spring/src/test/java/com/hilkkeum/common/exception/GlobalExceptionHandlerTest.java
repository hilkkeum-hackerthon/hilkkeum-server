package com.hilkkeum.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 전역 예외 처리 응답 코드 검증: 200 / 201 / 400 / 401 / 404 / 405 / 409 / 415 / 500
 */
class GlobalExceptionHandlerTest {

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void 성공_200() throws Exception {
		mvc.perform(get("/test/ok")).andExpect(status().isOk());
	}

	@Test
	void 생성_201() throws Exception {
		mvc.perform(post("/test/create")).andExpect(status().isCreated());
	}

	@Test
	void 검증_실패_400() throws Exception {
		mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("name: 이름을 입력해 주세요."));
	}

	@Test
	void 깨진_JSON_400() throws Exception {
		mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{bad"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 경로_타입_불일치_400() throws Exception {
		mvc.perform(get("/test/items/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 비즈니스_예외는_ErrorCode_상태_401_404() throws Exception {
		mvc.perform(get("/test/unauthorized"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("MEMBER_TOKEN_MISSING"));
		mvc.perform(get("/test/items/0"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
	}

	@Test
	void 지원하지_않는_메서드_405() throws Exception {
		mvc.perform(delete("/test/ok"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void JSON이_아닌_요청_415() throws Exception {
		mvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("x"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void DB_제약_위반_409() throws Exception {
		mvc.perform(post("/test/conflict"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
	}

	@Test
	void 예상하지_못한_오류_500_내부_메시지_미노출() throws Exception {
		mvc.perform(get("/test/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
	}

	record Body(@NotBlank(message = "이름을 입력해 주세요.") String name) {
	}

	@RestController
	static class TestController {

		@GetMapping("/test/ok")
		String ok() {
			return "ok";
		}

		@PostMapping("/test/create")
		@ResponseStatus(HttpStatus.CREATED)
		String create() {
			return "created";
		}

		@PostMapping("/test/body")
		String body(@Valid @RequestBody Body body) {
			return body.name();
		}

		@GetMapping("/test/items/{id}")
		String item(@PathVariable Long id) {
			throw new ApiException(ErrorCode.ROOM_NOT_FOUND);
		}

		@GetMapping("/test/unauthorized")
		String unauthorized() {
			throw new ApiException(ErrorCode.MEMBER_TOKEN_MISSING);
		}

		@PostMapping("/test/conflict")
		String conflict() {
			throw new DataIntegrityViolationException("Duplicate entry 'K7M3QX' for key 'rooms.uk_rooms_invite_code'");
		}

		@GetMapping("/test/boom")
		String boom() {
			throw new IllegalStateException("db password=secret");
		}
	}
}
