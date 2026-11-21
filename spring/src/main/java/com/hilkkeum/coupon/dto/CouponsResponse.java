package com.hilkkeum.coupon.dto;

import java.util.List;

/**
 * GET /api/v1/coupons 응답.
 *
 * @param stampCount       내 스탬프 수 (3개 이상이면 교환 가능)
 * @param myCoupons        내 소원권 목록 (상태 · 만료일 포함)
 * @param pendingApprovals 상대가 사용 요청해서 내가 승인해야 하는 소원권 → 앱은 폴링하다가 있으면 승인 요청 알림 표시
 */
public record CouponsResponse(int stampCount, List<CouponResponse> myCoupons, List<CouponResponse> pendingApprovals) {
}
