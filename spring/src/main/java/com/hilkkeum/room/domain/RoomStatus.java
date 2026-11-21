package com.hilkkeum.room.domain;

public enum RoomStatus {
	PENDING,  // 생성자만 있음, 상대 입장 대기
	ACTIVE,   // 두 명 입장 완료 (커플 연결)
	CLOSED
}
