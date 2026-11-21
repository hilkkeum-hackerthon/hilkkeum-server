package com.hilkkeum.coupon.domain;

public enum CouponStatus {
	AVAILABLE,      // 사용 가능
	USE_REQUESTED,  // 사용 요청 → 상대 승인 대기
	USED,           // 상대 승인 → 사용 완료
	EXPIRED         // 발급 후 1개월 경과
}
