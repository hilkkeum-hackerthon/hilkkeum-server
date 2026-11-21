package com.hilkkeum.room.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * 초대 코드 / memberToken 생성기 (SecureRandom 사용 — 추측 불가).
 */
@Component
public class CodeGenerator {

	/** 헷갈리는 글자(0/O, 1/I/L) 제외 → 31자 */
	static final String INVITE_CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
	static final int INVITE_CODE_LENGTH = 6;
	private static final int TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	/** 예: "K7M3QX" */
	public String inviteCode() {
		StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
		for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
			sb.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
		}
		return sb.toString();
	}

	/** 43자 base64url 문자열 */
	public String memberToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
