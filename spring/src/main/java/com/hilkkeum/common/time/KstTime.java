package com.hilkkeum.common.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * DB(DATETIME, 한국 시간)에 저장된 LocalDateTime → 응답용 OffsetDateTime(+09:00).
 * 예: 2026-11-21T20:00:00+09:00
 */
public final class KstTime {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private KstTime() {
	}

	public static OffsetDateTime of(LocalDateTime value) {
		return value == null ? null : value.atZone(ZONE).toOffsetDateTime();
	}
}
