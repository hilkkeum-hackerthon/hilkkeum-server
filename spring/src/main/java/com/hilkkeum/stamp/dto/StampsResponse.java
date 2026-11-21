package com.hilkkeum.stamp.dto;

/**
 * GET /api/v1/stamps 응답.
 *
 * @param stampCount      보유 스탬프 수
 * @param stampsPerCoupon 소원권 1장에 필요한 스탬프 수 (3)
 * @param exchangeable    지금 교환 가능한지 (stampCount >= stampsPerCoupon)
 */
public record StampsResponse(int stampCount, int stampsPerCoupon, boolean exchangeable) {
}
