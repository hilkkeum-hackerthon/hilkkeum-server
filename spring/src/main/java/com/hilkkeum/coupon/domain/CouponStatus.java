package com.hilkkeum.coupon.domain;

public enum CouponStatus {
	AVAILABLE,  // 사용 가능
	USED,       // 사용 완료 (상대 승인 없이 즉시 사용)
	EXPIRED     // 발급 후 1개월 경과
}
