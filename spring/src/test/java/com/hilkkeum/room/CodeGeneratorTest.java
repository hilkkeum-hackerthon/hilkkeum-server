package com.hilkkeum.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.hilkkeum.room.service.CodeGenerator;

class CodeGeneratorTest {

	private final CodeGenerator generator = new CodeGenerator();

	@Test
	void 초대_코드는_헷갈리는_글자_없이_6자리() {
		for (int i = 0; i < 1000; i++) {
			assertThat(generator.inviteCode()).matches("^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}$");
		}
	}

	@Test
	void memberToken은_43자_base64url이고_매번_다르다() {
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			String token = generator.memberToken();
			assertThat(token).matches("^[A-Za-z0-9_-]{43}$");
			tokens.add(token);
		}
		assertThat(tokens).hasSize(1000);
	}
}
